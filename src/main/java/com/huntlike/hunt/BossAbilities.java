package com.huntlike.hunt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.phys.Vec3;

import com.huntlike.HuntLike;
import com.huntlike.hunt.HuntDefs.Tier;
import com.huntlike.mixin.MobAccessor;

/**
 * The Animal bosses' abilities after Egg Blaster: Soul Strike, Animalia Lunge, Herd Call and Apex Instinct.
 * Every ability targets only the quest owner.
 */
public final class BossAbilities {
	private BossAbilities() {
	}

	public static final String HERD_TAG = "huntlike_herd";
	public static final ResourceKey<DamageType> SOUL_STRIKE = ResourceKey.create(Registries.DAMAGE_TYPE, HuntLike.id("soul_strike"));

	private static final int LUNGE_INTERVAL = 20 * 10;
	private static final int LUNGE_WINDOW = 12;
	private static final double LUNGE_SPEED = 1.1;
	private static final double LUNGE_KNOCKBACK = 2.6;

	/** Live state of one boss fight. */
	private static final class State {
		boolean herdCalled;
		boolean soulStruck;
		boolean apex;
		final List<UUID> herd = new ArrayList<>();
		int lungeTicks;
		final Map<UUID, Integer> herdCooldown = new HashMap<>();
	}

	private static final Map<UUID, State> STATES = new HashMap<>();

	private static State state(Mob boss) {
		return STATES.computeIfAbsent(boss.getUUID(), id -> new State());
	}

	public static void forget(UUID boss) {
		STATES.remove(boss);
	}

	/** Whether this herd animal belongs to a fight that is still running. */
	public static boolean isTrackedHerd(Entity entity) {
		for (State state : STATES.values()) {
			if (state.herd.contains(entity.getUUID())) {
				return true;
			}
		}
		return false;
	}

	public static boolean isHerd(Entity entity) {
		return entity.entityTags().contains(HERD_TAG);
	}

	/** Herd Call makes the boss immune until every herd animal is dead. */
	public static boolean herdAlive(Mob boss) {
		State state = STATES.get(boss.getUUID());
		if (state == null || state.herd.isEmpty()) {
			return false;
		}
		ServerLevel level = (ServerLevel) boss.level();
		state.herd.removeIf(id -> {
			Entity entity = level.getEntity(id);
			return entity == null || !entity.isAlive();
		});
		return !state.herd.isEmpty();
	}

	public static boolean apex(Mob boss) {
		State state = STATES.get(boss.getUUID());
		return state != null && state.apex;
	}

	public static float meleeMultiplier(Mob boss) {
		return apex(boss) ? 1.5F : 1.0F;
	}

	public static int regen(Mob boss, Tier tier) {
		if (herdAlive(boss)) {
			return 0;
		}
		return apex(boss) ? 6 : tier.regen();
	}

	/** Health thresholds round up: 127.5 becomes 128. */
	private static float threshold(Mob boss, double fraction) {
		return (float) Math.ceil(boss.getMaxHealth() * fraction - 1.0E-6);
	}

	/** Called after the boss takes damage: triggers the health-threshold abilities in order. */
	public static void afterDamage(Mob boss, ServerPlayer owner, Tier tier) {
		State state = state(boss);
		ServerLevel level = (ServerLevel) boss.level();
		if (tier.number() >= 4 && !state.herdCalled) {
			float mark = threshold(boss, 0.75);
			if (boss.getHealth() <= mark) {
				boss.setHealth(Math.max(boss.getHealth(), mark));
				state.herdCalled = true;
				herdCall(boss, owner, tier, state, level);
				return;
			}
		}
		if (tier.number() >= 2 && !state.soulStruck && boss.getHealth() <= threshold(boss, 0.5)) {
			state.soulStruck = true;
			soulStrike(boss, owner, tier, level);
		}
		if (tier.number() >= 5 && !state.apex && boss.getHealth() <= threshold(boss, 0.4)) {
			state.apex = true;
			apexInstinct(boss, owner, level);
		}
	}

	// ---------------------------------------------------------------- Soul Strike

	private static void soulStrike(Mob boss, ServerPlayer owner, Tier tier, ServerLevel level) {
		float damage = (tier.number() * 2 - 1) * 2;
		play(level, owner, SoundEvents.WARDEN_HEARTBEAT, 1.0F);
		play(level, owner, SoundEvents.ELDER_GUARDIAN_CURSE, 1.2F);
		Vec3 from = boss.position().add(0, boss.getBbHeight() * 0.6, 0);
		Vec3 to = owner.position().add(0, owner.getBbHeight() * 0.5, 0);
		for (int i = 0; i <= 16; i++) {
			Vec3 point = from.lerp(to, i / 16.0);
			level.sendParticles(ParticleTypes.SOUL, point.x, point.y, point.z, 2, 0.05, 0.05, 0.05, 0.0);
		}
		if (owner.isCreative() || owner.isSpectator()) {
			return;
		}
		DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(SOUL_STRIKE), boss);
		owner.hurtServer(level, source, damage);
	}

	// ---------------------------------------------------------------- Animalia Lunge

	/** Runs every tick of a Tier III+ fight. */
	public static void tickLunge(Mob boss, ServerPlayer owner, Tier tier, long elapsed) {
		if (tier.number() < 3 || herdAlive(boss)) {
			return;
		}
		State state = state(boss);
		ServerLevel level = (ServerLevel) boss.level();
		if (state.lungeTicks > 0) {
			state.lungeTicks--;
			double reach = boss.getBbWidth() / 2.0 + 1.4;
			if (boss.distanceTo(owner) <= reach) {
				state.lungeTicks = 0;
				if (!owner.isCreative() && !owner.isSpectator()) {
					float damage = tier.melee() * 4 * meleeMultiplier(boss);
					owner.hurtServer(level, level.damageSources().mobAttack(boss), damage);
					Vec3 push = owner.position().subtract(boss.position()).multiply(1, 0, 1).normalize();
					double strength = LUNGE_KNOCKBACK * (1.0 - knockbackResistance(owner));
					owner.setDeltaMovement(push.x * strength, 0.55, push.z * strength);
					owner.connection.send(new ClientboundSetEntityMotionPacket(owner));
				}
				play(level, owner, SoundEvents.GOAT_RAM_IMPACT, 0.8F);
			}
			return;
		}
		if (elapsed > 0 && elapsed % LUNGE_INTERVAL == 0 && boss.hasLineOfSight(owner)) {
			Vec3 dir = owner.position().subtract(boss.position()).multiply(1, 0, 1).normalize();
			double speed = LUNGE_SPEED * (apex(boss) ? 1.2 : 1.0);
			boss.setDeltaMovement(dir.x * speed, 0.25, dir.z * speed);
			state.lungeTicks = LUNGE_WINDOW;
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, boss.getX(), boss.getY() + 0.5, boss.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
			play(level, owner, SoundEvents.RAVAGER_ROAR, 1.6F);
		}
	}

	/** Ecocide Armor's Lunge knockback resistance. */
	private static double knockbackResistance(ServerPlayer owner) {
		return com.huntlike.gear.GearEffects.lungeResistance(owner);
	}

	// ---------------------------------------------------------------- Herd Call

	private static void herdCall(Mob boss, ServerPlayer owner, Tier tier, State state, ServerLevel level) {
		boolean wolves = tier.number() >= 5;
		EntityType<? extends Mob> type = wolves ? EntityTypes.WOLF : EntityTypes.COW;
		int count = wolves ? 6 : 5;
		float health = wolves ? 20.0F : 15.0F;
		String name = wolves ? "Canis Lupus" : "Bos Taurus";
		for (int i = 0; i < count; i++) {
			Mob mob = type.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (mob == null) {
				continue;
			}
			double angle = Math.PI * 2 * i / count;
			mob.snapTo(boss.getX() + Math.cos(angle) * 2.5, boss.getY(), boss.getZ() + Math.sin(angle) * 2.5, 0.0F, 0.0F);
			mob.setBaby(false);
			AttributeInstance max = mob.getAttribute(Attributes.MAX_HEALTH);
			if (max != null) {
				max.setBaseValue(health);
			}
			mob.setHealth(health);
			MobAccessor accessor = (MobAccessor) mob;
			accessor.huntlike$getGoalSelector().removeAllGoals(goal -> !(goal instanceof FloatGoal));
			accessor.huntlike$getTargetSelector().removeAllGoals(goal -> true);
			mob.setPersistenceRequired();
			mob.setCustomName(Messages.c(name, tier.color()));
			mob.setCustomNameVisible(true);
			mob.addTag(HERD_TAG);
			mob.addTag(BossController.OWNER_PREFIX + owner.getUUID());
			if (mob instanceof Wolf wolf) {
				wolf.setPersistentAngerTarget(EntityReference.of(owner));
				wolf.setTimeToRemainAngry(20L * 60 * 10);
			}
			state.herd.add(mob.getUUID());
			level.addFreshEntity(mob);
			level.sendParticles(ParticleTypes.CLOUD, mob.getX(), mob.getY() + 0.5, mob.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
		}
		play(level, owner, SoundEvents.EVOKER_CAST_SPELL, 0.7F);
	}

	/** Drives the herd: chase the owner and hit for 1 (cows) or 2 (wolves). Shield particles on the boss. */
	public static void tickHerd(Mob boss, ServerPlayer owner, long elapsed) {
		State state = STATES.get(boss.getUUID());
		if (state == null || !herdAlive(boss)) {
			return;
		}
		ServerLevel level = (ServerLevel) boss.level();
		for (UUID id : state.herd) {
			if (!(level.getEntity(id) instanceof Mob mob)) {
				continue;
			}
			mob.getLookControl().setLookAt(owner);
			if (elapsed % 10 == 0) {
				mob.getNavigation().moveTo(owner, 1.3);
			}
			int cooldown = state.herdCooldown.getOrDefault(id, 0);
			if (cooldown > 0) {
				state.herdCooldown.put(id, cooldown - 1);
			} else if (mob.distanceTo(owner) <= mob.getBbWidth() / 2.0 + 1.5 && !owner.isCreative() && !owner.isSpectator()) {
				owner.hurtServer(level, level.damageSources().mobAttack(mob), mob instanceof Wolf ? 2.0F : 1.0F);
				state.herdCooldown.put(id, 20);
			}
		}
		if (elapsed % 5 == 0) {
			level.sendParticles(ParticleTypes.ENCHANTED_HIT, boss.getX(), boss.getY() + boss.getBbHeight() / 2.0, boss.getZ(),
					6, boss.getBbWidth() / 2.0, boss.getBbHeight() / 2.0, boss.getBbWidth() / 2.0, 0.0);
		}
	}

	public static void discardHerd(Mob boss) {
		State state = STATES.get(boss.getUUID());
		if (state == null) {
			return;
		}
		ServerLevel level = (ServerLevel) boss.level();
		for (UUID id : state.herd) {
			Entity entity = level.getEntity(id);
			if (entity != null) {
				entity.discard();
			}
		}
		state.herd.clear();
	}

	// ---------------------------------------------------------------- Apex Instinct

	private static void apexInstinct(Mob boss, ServerPlayer owner, ServerLevel level) {
		boss.setHealth(Math.max(boss.getHealth(), threshold(boss, 0.45)));
		AttributeInstance speed = boss.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.setBaseValue(speed.getBaseValue() * 1.2);
		}
		if (boss instanceof Wolf wolf) {
			wolf.setPersistentAngerTarget(EntityReference.of(owner));
			wolf.setTimeToRemainAngry(20L * 60 * 10);
		}
		level.sendParticles(ParticleTypes.ANGRY_VILLAGER, boss.getX(), boss.getY() + boss.getBbHeight(), boss.getZ(), 10, 0.6, 0.4, 0.6, 0.0);
		play(level, owner, SoundEvents.RAVAGER_ROAR, 0.8F);
	}

	private static void play(ServerLevel level, LivingEntity at, SoundEvent sound, float pitch) {
		level.playSound(null, at.blockPosition(), sound, SoundSource.HOSTILE, 1.0F, pitch);
	}
}
