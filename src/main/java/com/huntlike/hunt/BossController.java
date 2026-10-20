package com.huntlike.hunt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;

import com.huntlike.entity.BossEgg;
import com.huntlike.hunt.HuntData.Quest;
import com.huntlike.hunt.HuntDefs.Hunt;
import com.huntlike.hunt.HuntDefs.Tier;
import com.huntlike.mixin.MobAccessor;

/** Spawns Hunt bosses and runs their fight. Bosses are vanilla mobs driven from here. */
public final class BossController {
	private BossController() {
	}

	public static final String BOSS_TAG = "huntlike_boss";
	public static final String OWNER_PREFIX = "huntlike_owner:";

	/** Players who damaged each boss (lootshare helpers). */
	private static final Map<UUID, Set<UUID>> HELPERS = new HashMap<>();
	/** Ticks until each boss can melee again. */
	private static final Map<UUID, Integer> MELEE_COOLDOWN = new HashMap<>();

	public static boolean isBoss(LivingEntity entity) {
		return entity.entityTags().contains(BOSS_TAG);
	}

	public static UUID ownerOf(Entity entity) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(OWNER_PREFIX)) {
				try {
					return UUID.fromString(tag.substring(OWNER_PREFIX.length()));
				} catch (IllegalArgumentException e) {
					return null;
				}
			}
		}
		return null;
	}

	public static Mob findBoss(ServerPlayer player, Quest quest) {
		if (quest.bossUuid.isEmpty()) {
			return null;
		}
		try {
			Entity entity = player.level().getEntity(UUID.fromString(quest.bossUuid));
			return entity instanceof Mob mob ? mob : null;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public static void discardBoss(ServerPlayer player, Quest quest) {
		Mob boss = findBoss(player, quest);
		if (boss != null) {
			HELPERS.remove(boss.getUUID());
			MELEE_COOLDOWN.remove(boss.getUUID());
			boss.discard();
		}
	}

	// ---------------------------------------------------------------- summoning

	public static void tickSummon(ServerPlayer player, HuntData data, Quest quest) {
		if (QuestManager.isPeaceful(player)) {
			QuestManager.resetToQuotaMinusOne(player, data);
			return;
		}
		ServerLevel level = player.level();
		Hunt hunt = HuntDefs.hunt(quest.hunt);
		double cx = quest.summonX + 0.5;
		double cy = quest.summonY + 0.1;
		double cz = quest.summonZ + 0.5;
		double progress = 1.0 - quest.summonTicks / (double) QuestManager.SUMMON_TICKS;
		double radius = 1.6 * (1.0 - progress) + 0.2;
		double angle = quest.summonTicks * 0.45;
		for (int arm = 0; arm < 4; arm++) {
			double a = angle + arm * Math.PI / 2.0;
			double y = cy + progress * 1.5 + arm * 0.1;
			level.sendParticles(new DustParticleOptions(hunt.color(), 1.0F), cx + Math.cos(a) * radius, y,
					cz + Math.sin(a) * radius, 1, 0, 0, 0, 0);
			level.sendParticles(ParticleTypes.WITCH, cx + Math.cos(a + 0.3) * radius, y, cz + Math.sin(a + 0.3) * radius,
					1, 0, 0, 0, 0);
		}
		quest.summonTicks--;
		if (quest.summonTicks <= 0) {
			spawnBoss(player, data, quest);
		}
	}

	private static void spawnBoss(ServerPlayer player, HuntData data, Quest quest) {
		ServerLevel level = player.level();
		Hunt hunt = HuntDefs.hunt(quest.hunt);
		Tier tier = hunt.tier(quest.tier);
		Mob boss = tier.mob().create(level, EntitySpawnReason.MOB_SUMMONED);
		if (boss == null) {
			QuestManager.resetToQuotaMinusOne(player, data);
			return;
		}
		boss.snapTo(quest.summonX + 0.5, quest.summonY, quest.summonZ + 0.5, player.getYRot() + 180.0F, 0.0F);
		boss.setBaby(false);
		setBase(boss, Attributes.MAX_HEALTH, tier.health());
		setBase(boss, Attributes.SCALE, tier.scale());
		setBase(boss, Attributes.MOVEMENT_SPEED, 0.3);
		setBase(boss, Attributes.FOLLOW_RANGE, 64.0);
		setBase(boss, Attributes.STEP_HEIGHT, 1.0);
		boss.setHealth(tier.health());
		MobAccessor accessor = (MobAccessor) boss;
		accessor.huntlike$getGoalSelector().removeAllGoals(goal -> !(goal instanceof FloatGoal));
		accessor.huntlike$getTargetSelector().removeAllGoals(goal -> true);
		boss.setPersistenceRequired();
		boss.setCustomName(Messages.c(tier.bossName(), tier.color()));
		boss.setCustomNameVisible(true);
		boss.addTag(BOSS_TAG);
		boss.addTag(OWNER_PREFIX + player.getUUID());

		quest.bossUuid = boss.getUUID().toString();
		quest.bossSpawnTime = level.getGameTime();
		quest.phase = HuntData.PHASE_BOSS;
		player.setAttached(QuestManager.DATA, data);
		level.addFreshEntity(boss);

		level.sendParticles(ParticleTypes.EXPLOSION, boss.getX(), boss.getY() + 0.5, boss.getZ(), 1, 0, 0, 0, 0);
		player.sendSystemMessage(Messages.bossSpawned(hunt, tier.number()));
		QuestManager.playSound(player, SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F);
		QuestManager.playSound(player, SoundEvents.TRIAL_SPAWNER_SPAWN_MOB, 0.8F);
		QuestManager.sync(player);
	}

	private static void setBase(Mob mob, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
			double value) {
		AttributeInstance instance = mob.getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	// ---------------------------------------------------------------- the fight

	public static void tickBoss(ServerPlayer player, HuntData data, Quest quest) {
		Mob boss = findBoss(player, quest);
		if (boss == null || !boss.isAlive()) {
			if (boss == null || boss.isRemoved()) {
				// The boss vanished without being killed: one kill short of calling it again.
				QuestManager.resetToQuotaMinusOne(player, data);
			}
			return;
		}
		if (QuestManager.isPeaceful(player)) {
			QuestManager.resetToQuotaMinusOne(player, data);
			return;
		}
		ServerLevel level = player.level();
		long elapsed = level.getGameTime() - quest.bossSpawnTime;
		if (elapsed > QuestManager.BOSS_TIME_LIMIT || boss.distanceTo(player) > QuestManager.FLEE_DISTANCE) {
			QuestManager.fail(player, data);
			return;
		}
		Tier tier = HuntDefs.hunt(quest.hunt).tier(quest.tier);

		// Chase the owner.
		boss.getLookControl().setLookAt(player);
		if (elapsed % 10 == 0) {
			boss.getNavigation().moveTo(player, 1.2);
		}

		// Basic melee.
		int cooldown = MELEE_COOLDOWN.getOrDefault(boss.getUUID(), 0);
		if (cooldown > 0) {
			MELEE_COOLDOWN.put(boss.getUUID(), cooldown - 1);
		} else if (player.isAlive() && !player.isSpectator() && !player.isCreative()) {
			double reach = boss.getBbWidth() / 2.0 + 1.6;
			if (boss.distanceTo(player) <= reach) {
				player.hurtServer(level, level.damageSources().mobAttack(boss), tier.melee());
				MELEE_COOLDOWN.put(boss.getUUID(), 20);
			}
		}

		if (elapsed % 20 == 0) {
			// Regeneration: health equal to the tier, every second.
			boss.heal(tier.regen());
			// Egg Blaster: one fast egg every second.
			if (boss.hasLineOfSight(player) && !player.isSpectator()) {
				BossEgg egg = new BossEgg(level, boss, tier.eggDamage());
				double dx = player.getX() - egg.getX();
				double dy = player.getEyeY() - 0.4 - egg.getY();
				double dz = player.getZ() - egg.getZ();
				egg.shoot(dx, dy, dz, 2.2F, 0.0F);
				level.addFreshEntity(egg);
				level.playSound(null, boss.blockPosition(), SoundEvents.EGG_THROW, net.minecraft.sounds.SoundSource.HOSTILE,
						1.0F, 1.4F);
			}
		}

		// Aura in the tier's color.
		if (elapsed % 4 == 0) {
			double w = boss.getBbWidth() / 2.0;
			double h = boss.getBbHeight();
			level.sendParticles(new DustParticleOptions(tier.color(), 1.2F), boss.getX(), boss.getY() + h / 2.0, boss.getZ(),
					3, w, h / 2.0, w, 0.0);
		}
	}

	public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (!isBoss(entity)) {
			return true;
		}
		return !(source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypes.IN_WALL));
	}

	public static void afterDamage(LivingEntity entity, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
		if (isBoss(entity) && source.getEntity() instanceof ServerPlayer player) {
			HELPERS.computeIfAbsent(entity.getUUID(), id -> new HashSet<>()).add(player.getUUID());
		}
	}

	public static void onBossDeath(LivingEntity entity) {
		Set<UUID> helpers = HELPERS.remove(entity.getUUID());
		MELEE_COOLDOWN.remove(entity.getUUID());
		UUID ownerId = ownerOf(entity);
		if (ownerId == null || !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
		if (owner == null) {
			return;
		}
		HuntData data = QuestManager.data(owner);
		if (data.quest == null || !data.quest.bossUuid.equals(entity.getUUID().toString())) {
			return;
		}
		QuestManager.complete(owner, data, helpers == null ? Set.of() : helpers);
	}
}
