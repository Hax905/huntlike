package com.huntlike.path;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import com.huntlike.hunt.HuntData;
import com.huntlike.hunt.Messages;
import com.huntlike.hunt.QuestManager;

/**
 * The Path of the Wildfire, found in the Soul Nexus. Its goals, one after another; each gives a Wildfire point, and the
 * first three raise the Blazing Heart's drop chance by 1%. Points buy the Path's buffs (1 point each, free to take back).
 * Draft values, to measure how strong the buffs are.
 */
public final class WildfirePath {
	private WildfirePath() {
	}

	public static final String NAME = "Path of the Wildfire";
	public static final int COLOR = 0xFF8A2A;
	public static final List<String> GOALS = List.of("Find a Blazing Heart", "Consume a Blazing Heart", "Slay 100 blazes",
			"Defeat a Blazing Piglin", "Defeat a Blazing Wither Skeleton", "Survive the Blazing Spawner");
	/** The hint each goal shows while it's the next one (drafts, to be rewritten). */
	public static final List<String> HINTS = List.of(
			"Deep in the fortresses of the Nether, the fire spirits guard a pulse that should not beat. Let them fall, and listen.",
			"A heart this warm was never meant to be held. Some things are only understood once they are inside you.",
			"One flame is a spark; a hundred are a wildfire. Put the fire spirits out until their ashes cover the fortress floor.",
			"The gold-hungry wanderers of the wastes crave warmth above all else. Feed one what burns, and face what it becomes.",
			"Black bones still walk the fortress halls. A heart that has burned once could make them remember the flame.",
			"Bring the heart that burned three times back to the cage where the fire spirits are born, and endure what crawls out.");
	public static final int BLAZES = 100;
	/** Blazing Heart from a blaze: 2%, +1% per Looting level, +1% for each of the first three goals reached. */
	public static final int BASE_CHANCE = 2;
	public static final int BONUS_GOALS = 3;

	/**
	 * The Path's buffs (draft), as a tree: Fire Resistance first; then Burning Blows and Warm Heart; then Nether Strength
	 * (after Burning Blows) and Hot Heart (after Warm Heart). The tier is the numeral shown in its square.
	 */
	public enum Buff {
		FIRE_RESISTANCE("Fire Resistance", "Take 25% less fire and lava damage.", null, 1),
		BURNING_BLOWS("Burning Blows", "In the Nether, your melee hits set targets on fire for 2 s.", FIRE_RESISTANCE, 2),
		NETHER_STRENGTH("Nether Strength", "Deal 5% more damage in the Nether.", BURNING_BLOWS, 3),
		WARM_HEART("Warm Heart", "Eat Blazing Hearts 50% faster; the fire beam lasts 1 s longer.", FIRE_RESISTANCE, 2),
		HOT_HEART("Hot Heart", "The Blazing Heart's fire beam deals 50% more damage.", WARM_HEART, 3);

		public final String title;
		public final String description;
		/** The buff it needs first, or null. */
		public final Buff parent;
		public final int tier;

		Buff(String title, String description, Buff parent, int tier) {
			this.title = title;
			this.description = description;
			this.parent = parent;
			this.tier = tier;
		}

		public int bit() {
			return 1 << ordinal();
		}
	}

	public static final float FIRE_RESISTANCE = 0.25F;
	public static final int BURNING_BLOWS_SECONDS = 2;
	public static final float NETHER_STRENGTH = 0.05F;
	public static final int BEAM_TICKS = 3 * 20;
	public static final int WARM_HEART_TICKS = 20;
	public static final float BEAM_DAMAGE = 2.0F;
	public static final int BEAM_HIT_TICKS = 5;
	public static final double BEAM_RANGE = 12.0;
	public static final float HOT_HEART = 0.5F;

	/** Client side: whether the local player has Warm Heart (set by the client, for the eating animation). */
	public static java.util.function.BooleanSupplier clientWarmHeart = () -> false;

	public static boolean clientHasWarmHeart() {
		return clientWarmHeart.getAsBoolean();
	}

	public static boolean discovered(HuntData data) {
		return data.wildfireGoals > 0;
	}

	public static boolean has(HuntData data, Buff buff) {
		return (data.wildfireBuffs & buff.bit()) != 0;
	}

	/** Whether a buff that needs this one is taken (then this one can't be given back). */
	public static boolean hasChild(HuntData data, Buff buff) {
		for (Buff other : Buff.values()) {
			if (other.parent == buff && has(data, other)) {
				return true;
			}
		}
		return false;
	}

	/** Most points the Path can give (one per goal). */
	public static int maxPoints() {
		return GOALS.size();
	}

	public static int points(HuntData data) {
		return data.wildfireGoals;
	}

	public static int spent(HuntData data) {
		return Integer.bitCount(data.wildfireBuffs);
	}

	public static int free(HuntData data) {
		return Math.max(0, points(data) - spent(data));
	}

	/** The Blazing Heart's drop chance from a blaze, in percent. */
	public static int heartChance(HuntData data, int looting) {
		return BASE_CHANCE + Math.min(BONUS_GOALS, data.wildfireGoals) + Math.max(0, looting);
	}

	/** Reaches goal `index` if it is the next one. */
	public static void reach(ServerPlayer player, HuntData data, int index) {
		if (data.wildfireGoals != index) {
			return;
		}
		data.wildfireGoals++;
		player.setAttached(QuestManager.DATA, data);
		if (index == 0) {
			player.sendSystemMessage(Messages.c("You discovered the " + NAME + "! (Soul Nexus)", COLOR));
		}
		String reward = index < BONUS_GOALS ? " +1 Wildfire point, Blazing Heart chance +1%" : " +1 Wildfire point";
		player.sendSystemMessage(Messages.c(NAME + ": " + GOALS.get(index) + "!" + reward, COLOR));
		player.level().playSound(null, player.blockPosition(), SoundEvents.BLAZE_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.4F);
		QuestManager.playSound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F);
		QuestManager.sync(player);
	}

	/** Buys (delta > 0) or takes back (delta < 0) a buff. */
	public static void change(ServerPlayer player, HuntData data, int buff, int delta) {
		if (buff < 0 || buff >= Buff.values().length || !discovered(data)) {
			return;
		}
		Buff b = Buff.values()[buff];
		boolean owned = has(data, b);
		if (delta > 0 && !owned && free(data) > 0 && (b.parent == null || has(data, b.parent))) {
			data.wildfireBuffs |= b.bit();
		} else if (delta < 0 && owned && !hasChild(data, b)) {
			data.wildfireBuffs &= ~b.bit();
		} else {
			QuestManager.playSound(player, SoundEvents.CHAIN_BREAK, 1.0F);
			return;
		}
		player.setAttached(QuestManager.DATA, data);
		player.level().playSound(null, player.blockPosition(), delta > 0 ? SoundEvents.FIRECHARGE_USE : SoundEvents.FIRE_EXTINGUISH,
				SoundSource.PLAYERS, 0.8F, 1.2F);
	}

	// ---------------------------------------------------------------- damage

	/** Fire Resistance, Burning Blows and Nether Strength; the Blazing Wither Skeleton's skulls deal 12. */
	public static float adjust(LivingEntity target, DamageSource source, float amount) {
		// Imps' fireballs hit twice as hard as a blaze's; the Demonling's 50% harder.
		if (source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball) {
			if (source.getEntity() instanceof Imp) {
				amount *= 2.0F;
			} else if (source.getEntity() instanceof Demonling) {
				amount *= 1.5F;
			}
		}
		if (source.getEntity() instanceof ServerPlayer attacker && attacker != target && attacker.level().dimension() == Level.NETHER) {
			HuntData data = QuestManager.data(attacker);
			if (has(data, Buff.NETHER_STRENGTH)) {
				amount *= 1.0F + NETHER_STRENGTH;
			}
			if (has(data, Buff.BURNING_BLOWS) && source.getDirectEntity() == attacker) {
				target.igniteForSeconds(BURNING_BLOWS_SECONDS);
			}
		}
		if (target instanceof ServerPlayer player && source.is(DamageTypeTags.IS_FIRE) && has(QuestManager.data(player), Buff.FIRE_RESISTANCE)) {
			amount *= 1.0F - FIRE_RESISTANCE;
		}
		return amount;
	}

	// ---------------------------------------------------------------- the Blazing Heart

	/** Players breathing fire: ticks left and the damage per hit. */
	private static final Map<UUID, float[]> BEAMS = new HashMap<>();

	public static boolean beaming(ServerPlayer player) {
		return BEAMS.containsKey(player.getUUID());
	}

	public static void onHeartEaten(ServerPlayer player) {
		HuntData data = QuestManager.data(player);
		player.igniteForSeconds(5);
		int ticks = BEAM_TICKS + (has(data, Buff.WARM_HEART) ? WARM_HEART_TICKS : 0);
		float damage = BEAM_DAMAGE * (has(data, Buff.HOT_HEART) ? 1.0F + HOT_HEART : 1.0F);
		BEAMS.put(player.getUUID(), new float[] {ticks, damage});
		player.level().playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
		reach(player, data, 0);
		reach(player, data, 1);
	}

	private static void tickBeams(MinecraftServer server) {
		for (Iterator<Map.Entry<UUID, float[]>> it = BEAMS.entrySet().iterator(); it.hasNext();) {
			Map.Entry<UUID, float[]> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			float[] beam = entry.getValue();
			if (player == null || !player.isAlive() || --beam[0] < 0) {
				it.remove();
				continue;
			}
			ServerLevel level = player.level();
			Vec3 eye = player.getEyePosition();
			Vec3 end = eye.add(player.getLookAngle().scale(BEAM_RANGE));
			HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			if (hit.getType() != HitResult.Type.MISS) {
				end = hit.getLocation();
			}
			Vec3 dir = end.subtract(eye);
			double length = dir.length();
			for (double d = 0.8; d < length; d += 0.5) {
				Vec3 at = eye.add(dir.scale(d / length));
				level.sendParticles(ParticleTypes.FLAME, at.x, at.y - 0.15, at.z, 1, 0.05, 0.05, 0.05, 0.01);
			}
			if ((int) beam[0] % 10 == 0) {
				level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.6F, 1.0F);
			}
			if ((int) beam[0] % BEAM_HIT_TICKS != 0) {
				continue;
			}
			Vec3 tip = end;
			for (Entity entity : level.getEntities(player, new AABB(eye, tip).inflate(1.0),
					e -> e instanceof LivingEntity living && living.isAlive() && e.getBoundingBox().inflate(0.4).clip(eye, tip).isPresent())) {
				LivingEntity living = (LivingEntity) entity;
				living.hurtServer(level, level.damageSources().playerAttack(player), beam[1]);
				living.igniteForSeconds(3);
			}
		}
	}

	// ---------------------------------------------------------------- blazes and the Blazing mobs

	private static void onDeath(LivingEntity entity, DamageSource source) {
		if (!(source.getEntity() instanceof ServerPlayer player) || !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		collectBlood(player, entity);
		HuntData data = QuestManager.data(player);
		if (entity.getType() == EntityTypes.BLAZE) {
			var looting = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING);
			int chance = heartChance(data, EnchantmentHelper.getEnchantmentLevel(looting, player));
			if (player.getRandom().nextInt(100) < chance) {
				entity.spawnAtLocation(level, new ItemStack(ModPaths.BLAZING_HEART));
			}
			if (data.wildfireGoals == 2) {
				data.wildfireBlazes++;
				player.setAttached(QuestManager.DATA, data);
				if (data.wildfireBlazes >= BLAZES) {
					reach(player, data, 2);
				} else if (data.wildfireBlazes % 10 == 0) {
					QuestManager.sync(player);
				}
			}
		} else if (entity instanceof BlazingPiglin) {
			reach(player, data, 3);
		} else if (entity instanceof BlazingWitherSkeleton) {
			reach(player, data, 4);
		}
	}

	/**
	 * Feeding hearts: a Blazing Heart turns a piglin into a Blazing Piglin; a Blazing Heart ★ makes a Blazing Wither
	 * Skeleton; a Blazing Heart ★★ turns a blaze into a Demonling.
	 */
	public static InteractionResult feed(ServerPlayer player, Mob target, ItemStack stack) {
		ServerLevel level = player.level();
		if (target.getType() == EntityTypes.PIGLIN && !target.isBaby() && stack.is(ModPaths.BLAZING_HEART)) {
			stack.consume(1, player);
			transform(player, target, new BlazingPiglin(ModPaths.BLAZING_PIGLIN, level));
			return InteractionResult.SUCCESS;
		}
		if (target.getType() == EntityTypes.WITHER_SKELETON && stack.is(ModPaths.BLAZING_HEART_STAR)) {
			stack.consume(1, player);
			transform(player, target, new BlazingWitherSkeleton(ModPaths.BLAZING_WITHER_SKELETON, level));
			return InteractionResult.SUCCESS;
		}
		if (target.getType() == EntityTypes.BLAZE && stack.is(ModPaths.BLAZING_HEART_STAR2)) {
			stack.consume(1, player);
			transform(player, target, new Demonling(ModPaths.DEMONLING, level));
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}


	private static void transform(ServerPlayer player, Mob from, Mob to) {
		ServerLevel level = player.level();
		to.snapTo(from.getX(), from.getY(), from.getZ(), from.getYRot(), from.getXRot());
		to.setYHeadRot(from.getYHeadRot());
		BlazingMob.setFeeder(to, player.getUUID());
		BlazingMob.setBorn(to, level.getGameTime());
		from.discard();
		level.addFreshEntity(to);
		to.setTarget(player);
		level.sendParticles(ParticleTypes.LAVA, to.getX(), to.getY() + 1.0, to.getZ(), 20, 0.4, 0.8, 0.4, 0.0);
		level.sendParticles(ParticleTypes.FLAME, to.getX(), to.getY() + 1.0, to.getZ(), 40, 0.4, 0.9, 0.4, 0.05);
		level.playSound(null, to.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.5F, 0.6F);
	}

	private static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hit) {
		if (player.isSpectator() || !(entity instanceof Mob mob)) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(ModPaths.BLAZING_HEART) && !stack.is(ModPaths.BLAZING_HEART_STAR) && !stack.is(ModPaths.BLAZING_HEART_STAR2)) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			return feed(serverPlayer, mob, stack);
		}
		boolean feedable = mob.getType() == EntityTypes.PIGLIN && stack.is(ModPaths.BLAZING_HEART)
				|| mob.getType() == EntityTypes.WITHER_SKELETON && stack.is(ModPaths.BLAZING_HEART_STAR)
				|| mob.getType() == EntityTypes.BLAZE && stack.is(ModPaths.BLAZING_HEART_STAR2);
		return feedable ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	/** A Blazing Heart ★★★ on a blaze spawner (only those: it sends the player to a fortress): the Blazing Spawner's three rounds. */
	public static boolean upgradeSpawner(ServerPlayer player, net.minecraft.core.BlockPos pos, ItemStack stack) {
		ServerLevel level = player.level();
		if (!(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner)
				|| !stack.is(ModPaths.BLAZING_HEART_STAR3)
				|| !spawner.saveWithoutMetadata(level.registryAccess()).toString().contains("minecraft:blaze")) {
			return false;
		}
		level.setBlockAndUpdate(pos, ModPaths.BLAZING_SPAWNER.defaultBlockState());
		if (level.getBlockEntity(pos) instanceof BlazingSpawnerBlockEntity blazing) {
			blazing.setOwner(player.getUUID());
		}
		stack.consume(1, player);
		level.sendParticles(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 20, 0.4, 0.4, 0.4, 0.0);
		level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 40, 0.5, 0.5, 0.5, 0.03);
		level.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.5F, 0.5F);
		player.sendSystemMessage(Messages.c("The spawner burns hotter... something is coming out of it.", COLOR));
		return true;
	}

	/** Creatures with flesh: animals (hoglins and striders too), zombies of every kind, piglins, villagers and illagers. */
	public static boolean hasFlesh(LivingEntity entity) {
		return entity instanceof net.minecraft.world.entity.animal.Animal || entity instanceof net.minecraft.world.entity.monster.zombie.Zombie
				|| entity instanceof net.minecraft.world.entity.monster.Zoglin
				|| entity instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin
				|| entity instanceof net.minecraft.world.entity.npc.villager.AbstractVillager
				|| entity instanceof net.minecraft.world.entity.raid.Raider;
	}

	/** A fleshy kill with the Chalice in the off-hand: one drop of blood; at 100 it becomes the Full Chalice. */
	private static void collectBlood(ServerPlayer player, LivingEntity victim) {
		ItemStack chalice = player.getOffhandItem();
		if (!chalice.is(ModPaths.CHALICE_ITEM) || !hasFlesh(victim)) {
			return;
		}
		int blood = Math.min(ChaliceItem.MAX_BLOOD, ChaliceItem.blood(chalice) + 1);
		if (blood >= ChaliceItem.MAX_BLOOD) {
			player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(ModPaths.FULL_CHALICE));
			player.sendSystemMessage(Messages.c("The chalice is full.", 0xAA0000));
			player.level().playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0F, 0.5F);
			return;
		}
		chalice.set(ModPaths.BLOOD, blood);
		player.level().sendParticles(new net.minecraft.core.particles.DustParticleOptions(0x6A0000, 1.0F), victim.getX(),
				victim.getY() + victim.getBbHeight() / 2, victim.getZ(), 8, 0.2, 0.3, 0.2, 0.0);
	}

	private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isSpectator() || !stack.is(ModPaths.BLAZING_HEART_STAR3)
				|| !(level.getBlockEntity(hit.getBlockPos()) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity)) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			return upgradeSpawner(serverPlayer, hit.getBlockPos(), stack) ? InteractionResult.SUCCESS : InteractionResult.PASS;
		}
		return InteractionResult.SUCCESS;
	}

	public static void initialize() {
		ServerLivingEntityEvents.AFTER_DEATH.register(WildfirePath::onDeath);
		UseEntityCallback.EVENT.register(WildfirePath::onUseEntity);
		net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register(WildfirePath::onUseBlock);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			tickBeams(server);
			if (server.getTickCount() % 20 != 0) {
				return;
			}
			// Holding a Blazing Heart discovers the Path.
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				HuntData data = QuestManager.data(player);
				if (data.wildfireGoals == 0 && player.getInventory().contains(new ItemStack(ModPaths.BLAZING_HEART))) {
					reach(player, data, 0);
				}
			}
		});
	}
}
