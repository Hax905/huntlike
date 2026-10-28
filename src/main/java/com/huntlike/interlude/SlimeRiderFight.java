package com.huntlike.interlude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.huntlike.entity.BossEgg;
import com.huntlike.entity.ClusterBall;
import com.huntlike.hunt.HuntData;
import com.huntlike.hunt.HuntDefs;
import com.huntlike.hunt.HuntDefs.Drop;
import com.huntlike.hunt.Messages;
import com.huntlike.hunt.QuestManager;
import com.huntlike.interlude.InterludeDefs.Stage;
import com.huntlike.mixin.MobAccessor;

/**
 * The Slime Rider fight. Phase 1: Bartholomew rides Pom, they share one health bar, and both Hunts' weapons work. When
 * it runs out, a 50/50 roll decides who stays with half that health; the other leaves. Phase 2 has that one's own
 * abilities, and only its Hunt's weapons work. 5 minutes, no Hunt XP, paid with Interlude Tokens.
 */
public final class SlimeRiderFight {
	private SlimeRiderFight() {
	}

	public static final String TAG = "huntlike_interlude";
	public static final int TIME_LIMIT = 20 * 60 * 5;
	public static final int SUMMON_TICKS = 40;
	public static final double FLEE_DISTANCE = 64.0;

	public enum Phase { SUMMON, PAIR, POM, BARTHOLOMEW }

	private static final class Fight {
		final UUID owner;
		final int stage;
		Phase phase = Phase.SUMMON;
		final Vec3 spot;
		int summonTicks = SUMMON_TICKS;
		UUID pom;
		UUID bartholomew;
		final List<UUID> splits = new ArrayList<>();
		boolean split;
		long start;
		ServerBossEvent bar;
		int meleeCooldown;
		/** Ticks until the next charge or lunge; then the wind-up counts down, then the dash. */
		int abilityTimer;
		int windup;
		int dash;
		boolean dashHit;
		int turretTimer = 30;
		boolean turretEgg;
		int kickTimer = 100;
		int kickWindup;
		int ewwTimer;
		int ewwWarning;
		int hopTimer;

		Fight(UUID owner, int stage, Vec3 spot, RandomSource random) {
			this.owner = owner;
			this.stage = stage;
			this.spot = spot;
			this.abilityTimer = 100 + random.nextInt(101);
			this.ewwTimer = 240 + random.nextInt(81);
		}
	}

	private static final Map<UUID, Fight> FIGHTS = new HashMap<>();
	/** Bodies that Animal weapons work on, and bodies that Cluster weapons work on (both in Phase 1). */
	private static final Set<UUID> ANIMAL_BODIES = new HashSet<>();
	private static final Set<UUID> CLUSTER_BODIES = new HashSet<>();
	/** Set while a hit on Bartholomew is passed on to Pom (so weapon bonuses aren't counted twice). */
	private static boolean forwarding;

	// ---------------------------------------------------------------- queries

	public static boolean active(ServerPlayer player) {
		return FIGHTS.containsKey(player.getUUID());
	}

	/** The running stage (0 = none), for the menu. */
	public static int runningStage(ServerPlayer player) {
		Fight fight = FIGHTS.get(player.getUUID());
		return fight == null ? 0 : fight.stage;
	}

	public static int ticksLeft(ServerPlayer player) {
		Fight fight = FIGHTS.get(player.getUUID());
		if (fight == null || fight.phase == Phase.SUMMON) {
			return TIME_LIMIT;
		}
		return (int) Math.max(0, TIME_LIMIT - (player.level().getGameTime() - fight.start));
	}

	/** Entity ids of the living bodies (the owner sees them glow). */
	public static int[] bodyIds(ServerPlayer player) {
		Fight fight = FIGHTS.get(player.getUUID());
		if (fight == null) {
			return new int[0];
		}
		List<Integer> ids = new ArrayList<>();
		for (LivingEntity body : bodies(player.level(), fight)) {
			ids.add(body.getId());
		}
		return ids.stream().mapToInt(Integer::intValue).toArray();
	}

	public static boolean isInterlude(Entity entity) {
		return entity.entityTags().contains(TAG);
	}

	public static boolean countsAsAnimal(Entity entity) {
		return ANIMAL_BODIES.contains(entity.getUUID());
	}

	public static boolean countsAsCluster(Entity entity) {
		return CLUSTER_BODIES.contains(entity.getUUID());
	}

	/** A hit passed on from Bartholomew to Pom keeps the bonuses it already got. */
	public static boolean skipAdjust(LivingEntity target) {
		return forwarding && target instanceof Pom;
	}

	// ---------------------------------------------------------------- tokens and starting

	/** 1 gold ingot = 16 Interlude Tokens (bulk: 10 ingots = 160). */
	public static void buyTokens(ServerPlayer player, HuntData data, boolean bulk) {
		int gold = bulk ? InterludeDefs.BULK_GOLD : 1;
		Inventory inventory = player.getInventory();
		int have = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).is(Items.GOLD_INGOT)) {
				have += inventory.getItem(i).getCount();
			}
		}
		if (have < gold) {
			player.sendSystemMessage(Messages.error("You need " + gold + " Gold Ingot" + (gold == 1 ? "." : "s.")));
			return;
		}
		int left = gold;
		for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(Items.GOLD_INGOT)) {
				int take = Math.min(left, stack.getCount());
				stack.shrink(take);
				left -= take;
			}
		}
		inventory.setChanged();
		data.interludeTokens += gold * InterludeDefs.TOKENS_PER_GOLD;
		player.setAttached(QuestManager.DATA, data);
		QuestManager.playSound(player, SoundEvents.CHAIN_PLACE, 1.2F);
	}

	private static Component line(String event, int color, int stage) {
		Stage s = InterludeDefs.stage(stage);
		return Messages.c("Interlude ", HuntDefs.PREFIX).append(Messages.c(event, color)).append(Messages.c(" - ", HuntDefs.PREFIX))
				.append(Messages.c(InterludeDefs.NAME, InterludeDefs.COLOR)).append(Component.literal(" "))
				.append(Messages.c("[Stage " + s.roman() + "]", s.color()));
	}

	public static void start(ServerPlayer player, HuntData data, int stageNumber) {
		if (stageNumber < 1 || stageNumber > InterludeDefs.STAGES.size()) {
			return;
		}
		Stage stage = InterludeDefs.stage(stageNumber);
		if (active(player)) {
			player.sendSystemMessage(Messages.error("The Interlude is already running."));
			return;
		}
		if (data.quest != null || com.huntlike.towernaar.TowernaarFight.phase(player) != 0) {
			player.sendSystemMessage(Messages.error("You already have an active quest."));
			return;
		}
		if (QuestManager.isPeaceful(player)) {
			player.sendSystemMessage(Messages.peacefulStart());
			return;
		}
		if (!InterludeDefs.ranksMet(data, stage)) {
			player.sendSystemMessage(Messages.error("This stage unlocks at " + InterludeDefs.rankText(stage) + "."));
			return;
		}
		if (!InterludeDefs.stageOpen(data, stage)) {
			player.sendSystemMessage(Messages.error("Win Stage " + HuntDefs.ROMAN[stageNumber - 1] + " first."));
			return;
		}
		if (data.interludeTokens < stage.tokenCost()) {
			player.sendSystemMessage(Messages.error("Not enough Interlude Tokens."));
			return;
		}
		ServerLevel level = player.level();
		Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
		BlockPos ahead = BlockPos.containing(player.getX() + look.x * 4, player.getY(), player.getZ() + look.z * 4);
		BlockPos spot = QuestManager.findGround(level, ahead);
		if (spot == null) {
			spot = QuestManager.findGround(level, player.blockPosition());
		}
		if (spot == null) {
			player.sendSystemMessage(Messages.badTerrain());
			return;
		}
		data.interludeTokens -= stage.tokenCost();
		player.setAttached(QuestManager.DATA, data);
		FIGHTS.put(player.getUUID(), new Fight(player.getUUID(), stageNumber, Vec3.atBottomCenterOf(spot), player.getRandom()));
		player.sendSystemMessage(line("Started", HuntDefs.PREFIX, stageNumber));
		QuestManager.playSound(player, SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F);
		QuestManager.playSound(player, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.0F);
		QuestManager.sync(player);
	}

	public static void cancel(ServerPlayer player) {
		Fight fight = FIGHTS.get(player.getUUID());
		if (fight != null) {
			fail(player, fight);
		}
	}

	// ---------------------------------------------------------------- the fight

	private static Mob find(ServerLevel level, UUID id) {
		if (id == null) {
			return null;
		}
		Entity entity = level.getEntity(id);
		return entity instanceof Mob mob && mob.isAlive() ? mob : null;
	}

	private static List<LivingEntity> bodies(ServerLevel level, Fight fight) {
		List<LivingEntity> list = new ArrayList<>();
		switch (fight.phase) {
			case PAIR -> {
				addIfAlive(list, find(level, fight.pom));
				addIfAlive(list, find(level, fight.bartholomew));
			}
			case POM -> addIfAlive(list, find(level, fight.pom));
			case BARTHOLOMEW -> {
				addIfAlive(list, find(level, fight.bartholomew));
				for (UUID id : fight.splits) {
					addIfAlive(list, find(level, id));
				}
			}
			default -> {
			}
		}
		return list;
	}

	private static void addIfAlive(List<LivingEntity> list, Mob mob) {
		if (mob != null) {
			list.add(mob);
		}
	}

	private static void prepare(Mob mob, ServerPlayer owner, double health) {
		mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
		mob.setHealth((float) health);
		mob.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(64.0);
		MobAccessor accessor = (MobAccessor) mob;
		accessor.huntlike$getGoalSelector().removeAllGoals(goal -> true);
		accessor.huntlike$getTargetSelector().removeAllGoals(goal -> true);
		mob.setPersistenceRequired();
		mob.addTag(TAG);
		mob.addTag(com.huntlike.hunt.BossController.OWNER_PREFIX + owner.getUUID());
	}

	private static void spawn(ServerPlayer player, Fight fight) {
		ServerLevel level = player.level();
		Stage stage = InterludeDefs.stage(fight.stage);
		Pom pom = ModInterlude.POM.create(level, EntitySpawnReason.MOB_SUMMONED);
		Bartholomew bartholomew = ModInterlude.BARTHOLOMEW.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (pom == null || bartholomew == null) {
			fail(player, fight);
			return;
		}
		pom.snapTo(fight.spot.x, fight.spot.y, fight.spot.z, player.getYRot() + 180.0F, 0.0F);
		prepare(pom, player, stage.health());
		pom.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25);
		pom.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(1.0);
		bartholomew.setSize(2, true);
		bartholomew.snapTo(fight.spot.x, fight.spot.y + 1.5, fight.spot.z, player.getYRot() + 180.0F, 0.0F);
		prepare(bartholomew, player, stage.health());
		level.addFreshEntity(pom);
		level.addFreshEntity(bartholomew);
		bartholomew.startRiding(pom, true, true);
		fight.pom = pom.getUUID();
		fight.bartholomew = bartholomew.getUUID();
		fight.phase = Phase.PAIR;
		fight.start = level.getGameTime();
		ANIMAL_BODIES.add(pom.getUUID());
		CLUSTER_BODIES.add(pom.getUUID());
		ANIMAL_BODIES.add(bartholomew.getUUID());
		CLUSTER_BODIES.add(bartholomew.getUUID());
		fight.bar = new ServerBossEvent(pom.getUUID(), Messages.c(InterludeDefs.NAME, stage.color()), BossEvent.BossBarColor.GREEN,
				BossEvent.BossBarOverlay.NOTCHED_10);
		fight.bar.addPlayer(player);
		level.sendParticles(ParticleTypes.EXPLOSION, pom.getX(), pom.getY() + 0.5, pom.getZ(), 1, 0, 0, 0, 0);
		player.sendSystemMessage(line("Boss Spawned", HuntDefs.AQUA, fight.stage));
		QuestManager.playSound(player, SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F);
		QuestManager.playSound(player, SoundEvents.ZOMBIE_HORSE_AMBIENT, 0.8F);
		QuestManager.sync(player);
	}

	public static void tick(MinecraftServer server) {
		for (Map.Entry<UUID, Fight> entry : new ArrayList<>(FIGHTS.entrySet())) {
			Fight fight = entry.getValue();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				// Logging out ends the Interlude (no refund).
				end(server, fight);
				continue;
			}
			ServerLevel level = player.level();
			if (fight.phase == Phase.SUMMON) {
				tickSummon(player, fight);
				continue;
			}
			List<LivingEntity> bodies = bodies(level, fight);
			long elapsed = level.getGameTime() - fight.start;
			if (!player.isAlive() || bodies.isEmpty() || elapsed > TIME_LIMIT || QuestManager.isPeaceful(player)
					|| bodies.get(0).level() != level || bodies.get(0).distanceTo(player) > FLEE_DISTANCE) {
				fail(player, fight);
				continue;
			}
			updateBar(player, fight, bodies, elapsed);
			if (fight.meleeCooldown > 0) {
				fight.meleeCooldown--;
			}
			switch (fight.phase) {
				case PAIR -> tickPair(player, fight, (Mob) bodies.get(0));
				case POM -> tickPom(player, fight, (Mob) bodies.get(0));
				case BARTHOLOMEW -> tickBartholomew(player, fight, bodies);
				default -> {
				}
			}
			if (elapsed % 20 == 0) {
				QuestManager.sync(player);
			}
		}
	}

	private static void tickSummon(ServerPlayer player, Fight fight) {
		ServerLevel level = player.level();
		double progress = 1.0 - fight.summonTicks / (double) SUMMON_TICKS;
		double radius = 1.8 * (1.0 - progress) + 0.2;
		double angle = fight.summonTicks * 0.45;
		for (int arm = 0; arm < 4; arm++) {
			double a = angle + arm * Math.PI / 2.0;
			double y = fight.spot.y + 0.1 + progress * 1.8 + arm * 0.1;
			level.sendParticles(new DustParticleOptions(InterludeDefs.COLOR, 1.0F), fight.spot.x + Math.cos(a) * radius, y,
					fight.spot.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
			level.sendParticles(ParticleTypes.ITEM_SLIME, fight.spot.x + Math.cos(a + 0.3) * radius, y,
					fight.spot.z + Math.sin(a + 0.3) * radius, 1, 0, 0, 0, 0);
		}
		if (--fight.summonTicks <= 0) {
			spawn(player, fight);
		}
	}

	private static void updateBar(ServerPlayer player, Fight fight, List<LivingEntity> bodies, long elapsed) {
		if (elapsed % 5 != 0) {
			return;
		}
		Stage stage = InterludeDefs.stage(fight.stage);
		float health = 0;
		float max = 0;
		for (LivingEntity body : bodies) {
			if (fight.phase == Phase.PAIR && body instanceof Bartholomew) {
				continue;
			}
			health += body.getHealth();
			max += body.getMaxHealth();
		}
		String name = switch (fight.phase) {
			case POM -> "Pom";
			case BARTHOLOMEW -> "Bartholomew";
			default -> InterludeDefs.NAME;
		};
		int seconds = (int) Math.max(0, (TIME_LIMIT - elapsed) / 20);
		fight.bar.setName(Messages.c(name, stage.color())
				.append(Messages.c("  ❤ " + (int) Math.ceil(health) + "/" + (int) max, HuntDefs.RED))
				.append(Messages.c(String.format("  %d:%02d", seconds / 60, seconds % 60), HuntDefs.GRAY)));
		fight.bar.setProgress(max <= 0 ? 0 : Math.max(0, Math.min(1, health / max)));
		if (!fight.bar.getPlayers().contains(player)) {
			fight.bar.addPlayer(player);
		}
	}

	private static boolean targetable(ServerPlayer player) {
		return player.isAlive() && !player.isSpectator() && !player.isCreative();
	}

	private static void melee(ServerPlayer player, Fight fight, Mob attacker, float damage) {
		if (fight.meleeCooldown > 0 || !targetable(player)) {
			return;
		}
		double reach = attacker.getBbWidth() / 2.0 + 1.4;
		if (attacker.distanceTo(player) <= reach) {
			player.hurtServer(player.level(), player.level().damageSources().mobAttack(attacker), damage);
			fight.meleeCooldown = 20;
		}
	}

	private static Vec3 flatDirection(Entity from, Entity to) {
		Vec3 d = new Vec3(to.getX() - from.getX(), 0, to.getZ() - from.getZ());
		return d.lengthSqr() < 1.0E-4 ? Vec3.ZERO : d.normalize();
	}

	/** Pom runs at the player (or stops while an ability winds up). */
	private static void chase(Mob mob, ServerPlayer player, long time) {
		mob.getLookControl().setLookAt(player);
		if (time % 10 == 0) {
			mob.getNavigation().moveTo(player, 1.2);
		}
	}

	/** Hops toward the player, like a slime. */
	private static void hop(Mob slime, ServerPlayer player, Fight fight, double strength) {
		slime.getLookControl().setLookAt(player);
		slime.setYRot((float) (Math.atan2(player.getZ() - slime.getZ(), player.getX() - slime.getX()) * 180.0 / Math.PI) - 90.0F);
		if (--fight.hopTimer <= 0 && slime.onGround()) {
			Vec3 dir = flatDirection(slime, player);
			slime.setDeltaMovement(dir.x * strength, 0.42, dir.z * strength);
			slime.level().playSound(null, slime.blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.HOSTILE, 0.8F, 1.0F);
			fight.hopTimer = 12;
		}
	}

	/**
	 * A charge: the wind-up (standing still, with particles), then a dash at the player that hits once on contact. Used
	 * by Twin Tackle, Front Charge and Lunge!. Returns true while it's running.
	 */
	private static boolean tickCharge(ServerPlayer player, Fight fight, Mob mob, int windupTicks, int damage, boolean leap) {
		ServerLevel level = player.level();
		if (fight.windup > 0) {
			mob.getNavigation().stop();
			mob.getLookControl().setLookAt(player);
			if (fight.windup % 4 == 0) {
				level.sendParticles(leap ? ParticleTypes.ITEM_SLIME : ParticleTypes.CRIT, mob.getX(), mob.getY() + mob.getBbHeight() / 2,
						mob.getZ(), 6, mob.getBbWidth() / 2, mob.getBbHeight() / 3, mob.getBbWidth() / 2, 0.05);
			}
			if (--fight.windup == 0) {
				Vec3 dir = flatDirection(mob, player);
				mob.setDeltaMovement(dir.x * (leap ? 0.9 : 1.0), leap ? 0.55 : 0.1, dir.z * (leap ? 0.9 : 1.0));
				fight.dash = leap ? 15 : 10;
				fight.dashHit = false;
				level.playSound(null, mob.blockPosition(), leap ? SoundEvents.SLIME_JUMP : SoundEvents.ZOMBIE_HORSE_ANGRY,
						SoundSource.HOSTILE, 1.0F, 0.8F);
			}
			return true;
		}
		if (fight.dash > 0) {
			fight.dash--;
			if (!leap && fight.dash > 2) {
				Vec3 dir = flatDirection(mob, player);
				mob.setDeltaMovement(dir.x * 1.0, mob.getDeltaMovement().y, dir.z * 1.0);
			}
			if (!fight.dashHit && targetable(player) && mob.distanceTo(player) <= mob.getBbWidth() / 2.0 + 1.2) {
				player.hurtServer(level, level.damageSources().mobAttack(mob), damage);
				Vec3 dir = flatDirection(mob, player);
				player.push(dir.x * 0.8, 0.35, dir.z * 0.8);
				player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
				fight.dashHit = true;
			}
			if (fight.dash == 0) {
				fight.abilityTimer = 100 + mob.getRandom().nextInt(101);
			}
			return true;
		}
		if (--fight.abilityTimer <= 0) {
			fight.windup = windupTicks;
			mob.getNavigation().stop();
			if (mob instanceof Pom pom) {
				pom.standIfPossible();
			}
			level.playSound(null, mob.blockPosition(), leap ? SoundEvents.SLIME_SQUISH : SoundEvents.ZOMBIE_HORSE_ANGRY,
					SoundSource.HOSTILE, 1.0F, 0.6F);
			return true;
		}
		return false;
	}

	/** Phase 1: Twin Tackle (every 5-10 s, Stage x 3), and from Stage II the Supporting Turrets. */
	private static void tickPair(ServerPlayer player, Fight fight, Mob pom) {
		ServerLevel level = player.level();
		long time = level.getGameTime();
		Mob bartholomew = find(level, fight.bartholomew);
		if (bartholomew != null && bartholomew.getVehicle() != pom) {
			bartholomew.startRiding(pom, true, true);
		}
		boolean busy = tickCharge(player, fight, pom, 12, fight.stage * 3, false);
		if (!busy) {
			chase(pom, player, time);
			melee(player, fight, pom, InterludeDefs.stage(fight.stage).melee());
		}
		if (fight.stage >= 2 && --fight.turretTimer <= 0 && pom.hasLineOfSight(player)) {
			fight.turretTimer = 30;
			fight.turretEgg = !fight.turretEgg;
			if (fight.turretEgg) {
				BossEgg egg = new BossEgg(level, pom, 1.0F);
				shoot(egg, player);
				level.addFreshEntity(egg);
				level.playSound(null, pom.blockPosition(), SoundEvents.EGG_THROW, SoundSource.HOSTILE, 1.0F, 1.2F);
			} else if (bartholomew != null) {
				ClusterBall ball = new ClusterBall(level, bartholomew, Items.SLIME_BALL, 1.0F);
				shoot(ball, player);
				level.addFreshEntity(ball);
				level.playSound(null, bartholomew.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.HOSTILE, 1.0F, 0.6F);
			}
		}
	}

	private static void shoot(net.minecraft.world.entity.projectile.Projectile projectile, ServerPlayer player) {
		double dx = player.getX() - projectile.getX();
		double dy = player.getEyeY() - 0.4 - projectile.getY();
		double dz = player.getZ() - projectile.getZ();
		projectile.shoot(dx, dy, dz, 1.6F, 1.0F);
	}

	/** Phase 2 with Pom: Front Charge, Painful Kick (II+), and Ultimate Technique (III) at 10% health. */
	private static void tickPom(ServerPlayer player, Fight fight, Mob pom) {
		ServerLevel level = player.level();
		long time = level.getGameTime();
		if (fight.stage >= 3 && pom.getHealth() <= pom.getMaxHealth() * 0.1F) {
			// Ultimate Technique: no more attacks, it just runs (slower than a sprinting player).
			if (time % 20 == 0) {
				Vec3 away = flatDirection(player, pom).scale(12);
				pom.getNavigation().moveTo(pom.getX() + away.x, pom.getY(), pom.getZ() + away.z, 1.0);
			}
			if (time % 10 == 0) {
				level.sendParticles(ParticleTypes.CLOUD, pom.getX(), pom.getY() + 0.3, pom.getZ(), 2, 0.3, 0.1, 0.3, 0.01);
			}
			return;
		}
		if (fight.kickWindup > 0) {
			pom.getNavigation().stop();
			pom.getLookControl().setLookAt(player);
			if (--fight.kickWindup == 0 && targetable(player) && pom.distanceTo(player) <= pom.getBbWidth() / 2.0 + 2.2) {
				player.hurtServer(level, level.damageSources().mobAttack(pom), fight.stage * 3);
				player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 2), pom);
				Vec3 dir = flatDirection(pom, player);
				player.push(dir.x * 1.1, 0.4, dir.z * 1.1);
				player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
				level.playSound(null, pom.blockPosition(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 0.8F, 1.3F);
			}
			return;
		}
		boolean busy = tickCharge(player, fight, pom, 20, fight.stage * 2, false);
		if (busy) {
			return;
		}
		if (fight.stage >= 2 && --fight.kickTimer <= 0 && pom.distanceTo(player) <= 3.0) {
			fight.kickTimer = 160;
			fight.kickWindup = 15;
			if (pom instanceof Pom horse) {
				horse.standIfPossible();
			}
			level.playSound(null, pom.blockPosition(), SoundEvents.ZOMBIE_HORSE_ANGRY, SoundSource.HOSTILE, 1.0F, 1.2F);
			return;
		}
		chase(pom, player, time);
		melee(player, fight, pom, InterludeDefs.stage(fight.stage).melee());
	}

	/** Phase 2 with Bartholomew: Lunge!, Eww... (II+); his split halves (III) just hop and bite. */
	private static void tickBartholomew(ServerPlayer player, Fight fight, List<LivingEntity> bodies) {
		ServerLevel level = player.level();
		Mob bartholomew = find(level, fight.bartholomew);
		int melee = InterludeDefs.stage(fight.stage).melee();
		for (LivingEntity body : bodies) {
			if (body != bartholomew && body instanceof Mob split) {
				hopSplit(split, player, fight);
				melee(player, fight, split, Math.max(1, melee / 2));
			}
		}
		if (bartholomew == null) {
			return;
		}
		if (fight.stage >= 2) {
			tickEww(player, fight, bartholomew);
		}
		boolean busy = tickCharge(player, fight, bartholomew, 20, fight.stage * 3, true);
		if (!busy) {
			hop(bartholomew, player, fight, 0.35);
			melee(player, fight, bartholomew, melee);
		}
	}

	private static final Map<UUID, Integer> SPLIT_HOPS = new HashMap<>();

	private static void hopSplit(Mob slime, ServerPlayer player, Fight fight) {
		int timer = SPLIT_HOPS.getOrDefault(slime.getUUID(), 0) - 1;
		if (timer <= 0 && slime.onGround()) {
			Vec3 dir = flatDirection(slime, player);
			slime.setDeltaMovement(dir.x * 0.4, 0.42, dir.z * 0.4);
			timer = 10;
		}
		SPLIT_HOPS.put(slime.getUUID(), timer);
	}

	/** "Look Away!", then (4 - Stage) seconds later: still looking at him means Blindness and Weakness I for 2 seconds. */
	private static void tickEww(ServerPlayer player, Fight fight, Mob bartholomew) {
		if (fight.ewwWarning > 0) {
			if (--fight.ewwWarning == 0) {
				Vec3 toSlime = bartholomew.getEyePosition().subtract(player.getEyePosition()).normalize();
				boolean looking = player.getViewVector(1.0F).dot(toSlime) > 0.6 && player.hasLineOfSight(bartholomew);
				if (looking && targetable(player)) {
					player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), bartholomew);
					player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0), bartholomew);
					player.level().playSound(null, player.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.0F, 0.5F);
				}
			}
			return;
		}
		if (--fight.ewwTimer <= 0) {
			fight.ewwTimer = 240 + bartholomew.getRandom().nextInt(81);
			fight.ewwWarning = Math.max(1, (4 - fight.stage) * 20);
			player.connection.send(new ClientboundSetTitlesAnimationPacket(2, Math.max(10, fight.ewwWarning), 6));
			player.connection.send(new ClientboundSetSubtitleTextPacket(Messages.c("Eww...", InterludeDefs.COLOR)));
			player.connection.send(new ClientboundSetTitleTextPacket(Messages.c("Look Away!", HuntDefs.RED)));
			player.level().playSound(null, bartholomew.blockPosition(), SoundEvents.SLIME_ATTACK, SoundSource.HOSTILE, 1.0F, 0.5F);
		}
	}

	// ---------------------------------------------------------------- phase 2, splitting, winning and failing

	private static Fight fightOf(Entity body) {
		UUID owner = com.huntlike.hunt.BossController.ownerOf(body);
		return owner == null ? null : FIGHTS.get(owner);
	}

	/** Pom's health ran out. In Phase 1 the 50/50 roll happens and Pom stays alive (true); otherwise it dies. */
	static boolean onPomDefeated(Pom pom) {
		Fight fight = fightOf(pom);
		if (fight == null || fight.phase != Phase.PAIR || !(pom.level() instanceof ServerLevel level)) {
			return false;
		}
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(fight.owner);
		Stage stage = InterludeDefs.stage(fight.stage);
		Mob bartholomew = find(level, fight.bartholomew);
		double half = stage.health() / 2.0;
		fight.windup = 0;
		fight.dash = 0;
		fight.abilityTimer = 60 + pom.getRandom().nextInt(61);
		ANIMAL_BODIES.remove(fight.bartholomew);
		CLUSTER_BODIES.remove(fight.pom);
		if (pom.getRandom().nextBoolean() || bartholomew == null) {
			// Pom stays.
			fight.phase = Phase.POM;
			CLUSTER_BODIES.remove(fight.bartholomew);
			if (bartholomew != null) {
				bartholomew.stopRiding();
				poof(level, bartholomew);
				bartholomew.discard();
			}
			pom.getAttribute(Attributes.MAX_HEALTH).setBaseValue(half);
			pom.setHealth((float) half);
			if (player != null) {
				player.sendSystemMessage(Messages.c("Bartholomew the cluster ran away!", InterludeDefs.COLOR));
			}
			return true;
		}
		// Bartholomew stays: Pom leaves for good.
		fight.phase = Phase.BARTHOLOMEW;
		ANIMAL_BODIES.remove(fight.pom);
		bartholomew.stopRiding();
		bartholomew.getAttribute(Attributes.MAX_HEALTH).setBaseValue(half);
		bartholomew.setHealth((float) half);
		poof(level, pom);
		pom.allowDeath = true;
		pom.discard();
		if (player != null) {
			player.sendSystemMessage(Messages.c("The Equus got distracted by a butterfly and left without anyone noticing!",
					InterludeDefs.COLOR));
		}
		return false;
	}

	private static void poof(ServerLevel level, Entity entity) {
		level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 20, 0.4, 0.4, 0.4,
				0.05);
		level.playSound(null, entity.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.HOSTILE, 1.0F, 0.6F);
	}

	/** Damage rules: Phase 1 hits on Bartholomew go to Pom's (shared) health; no fall, wall or drowning damage. */
	public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (!isInterlude(entity)) {
			return true;
		}
		if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypes.IN_WALL)) {
			return false;
		}
		Fight fight = fightOf(entity);
		if (fight != null && fight.phase == Phase.PAIR && entity instanceof Bartholomew && entity.level() instanceof ServerLevel level) {
			Mob pom = find(level, fight.pom);
			if (pom != null && !forwarding) {
				forwarding = true;
				try {
					pom.hurtServer(level, source, amount);
				} finally {
					forwarding = false;
				}
				entity.hurtTime = 10;
			}
			return false;
		}
		return true;
	}

	private static void onDeath(LivingEntity entity, DamageSource source) {
		if (!isInterlude(entity) || !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		ANIMAL_BODIES.remove(entity.getUUID());
		CLUSTER_BODIES.remove(entity.getUUID());
		SPLIT_HOPS.remove(entity.getUUID());
		Fight fight = fightOf(entity);
		if (fight == null) {
			return;
		}
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(fight.owner);
		if (player == null) {
			return;
		}
		if (fight.phase == Phase.BARTHOLOMEW && entity.getUUID().equals(fight.bartholomew) && fight.stage >= 3 && !fight.split) {
			// "Are we for real?": two smaller slimes, each with 10% of Phase 1's health.
			fight.split = true;
			double health = InterludeDefs.stage(fight.stage).health() * 0.1;
			for (int i = 0; i < 2; i++) {
				Bartholomew half = ModInterlude.BARTHOLOMEW.create(level, EntitySpawnReason.MOB_SUMMONED);
				if (half == null) {
					continue;
				}
				half.setSize(1, true);
				half.snapTo(entity.getX() + (i == 0 ? -0.5 : 0.5), entity.getY() + 0.2, entity.getZ(), entity.getYRot(), 0.0F);
				prepare(half, player, health);
				level.addFreshEntity(half);
				fight.splits.add(half.getUUID());
				CLUSTER_BODIES.add(half.getUUID());
			}
			level.playSound(null, entity.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.0F, 0.7F);
			return;
		}
		if (bodies(level, fight).isEmpty()) {
			win(player, fight);
		}
	}

	private static void win(ServerPlayer player, Fight fight) {
		FIGHTS.remove(player.getUUID());
		if (fight.bar != null) {
			fight.bar.removeAllPlayers();
		}
		HuntData data = QuestManager.data(player);
		HuntData.Progress progress = data.progress(InterludeDefs.SLIME_RIDER);
		boolean pomStayed = fight.phase == Phase.POM;
		List<Drop> drops = pomStayed ? InterludeDefs.pomDrops(fight.stage) : InterludeDefs.bartholomewDrops(fight.stage);
		RandomSource random = player.getRandom();
		for (Drop drop : drops) {
			// The Mystery Concoction also works on the Slime Rider's drops when Pom (the Equus) stays.
			double chance = HuntDefs.chance("animal", drop.chanceNum(), drop.chanceDen(), pomStayed && data.concoction, false);
			if (random.nextDouble() >= chance) {
				continue;
			}
			ItemStack stack = new ItemStack(drop.item(), drop.minCount());
			if (drop.id().equals("leaping_potion")) {
				stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.LEAPING));
			}
			progress.dropsSeen.add(drop.id());
			progress.dropCounts.merge(drop.id(), drop.minCount(), Integer::sum);
			QuestManager.giveStack(player, stack);
			if (drop.dropClass() > 0) {
				player.sendSystemMessage(Messages.rareDrop(drop.dropClass(), stack.getStyledHoverName()));
				QuestManager.playSound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F);
			}
		}
		progress.bossKills[fight.stage - 1]++;
		player.setAttached(QuestManager.DATA, data);
		player.sendSystemMessage(line("Complete", HuntDefs.DARK_GREEN, fight.stage));
		QuestManager.playSound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F);
		QuestManager.sync(player);
	}

	private static void end(MinecraftServer server, Fight fight) {
		FIGHTS.remove(fight.owner);
		if (fight.bar != null) {
			fight.bar.removeAllPlayers();
		}
		for (ServerLevel level : server.getAllLevels()) {
			for (UUID id : allIds(fight)) {
				Entity entity = level.getEntity(id);
				if (entity != null) {
					if (entity instanceof Pom pom) {
						pom.allowDeath = true;
					}
					ANIMAL_BODIES.remove(id);
					CLUSTER_BODIES.remove(id);
					entity.discard();
				}
			}
		}
	}

	private static List<UUID> allIds(Fight fight) {
		List<UUID> ids = new ArrayList<>(fight.splits);
		if (fight.pom != null) {
			ids.add(fight.pom);
		}
		if (fight.bartholomew != null) {
			ids.add(fight.bartholomew);
		}
		return ids;
	}

	private static void fail(ServerPlayer player, Fight fight) {
		end(player.level().getServer(), fight);
		player.sendSystemMessage(line("Failed", HuntDefs.DARK_RED, fight.stage));
		QuestManager.playSound(player, SoundEvents.BEACON_DEACTIVATE, 0.8F);
		QuestManager.sync(player);
	}

	/** Interlude bodies with no fight (left over after a restart) go away. */
	static void tickBody(Mob body, ServerLevel level) {
		if (body.tickCount % 20 != 0 || !isInterlude(body)) {
			return;
		}
		Fight fight = fightOf(body);
		if (fight == null || !allIds(fight).contains(body.getUUID())) {
			if (body instanceof Pom pom) {
				pom.allowDeath = true;
			}
			body.discard();
		}
	}

	public static void initialize() {
		ServerTickEvents.END_SERVER_TICK.register(SlimeRiderFight::tick);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(SlimeRiderFight::allowDamage);
		ServerLivingEntityEvents.AFTER_DEATH.register(SlimeRiderFight::onDeath);
	}
}
