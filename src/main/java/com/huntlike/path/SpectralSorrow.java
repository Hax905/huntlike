package com.huntlike.path;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Spectral Sorrow: a red ghast that rises from a dried ghast fed the Full Chalice. 110 health. Instead of fireballs it
 * breathes a ray of fire (like the Blazing Heart's) that slowly follows its target: 6 damage each second in the ray.
 * Drops a Blood Crystallization; killing it reaches the Path of the Wildfire's eighth goal.
 */
public class SpectralSorrow extends Ghast {
	public static final float HEALTH = 110.0F;
	public static final float RAY_DAMAGE = 6.0F;
	/** Ticks between two hits of the ray on the same target. */
	public static final int RAY_HIT_TICKS = 20;
	public static final double RAY_RANGE = 48.0;
	/** Warning (the ghast's cry), then the ray, then a pause. */
	public static final int CHARGE_TICKS = 20;
	public static final int RAY_TICKS = 4 * 20;
	public static final int PAUSE_TICKS = 2 * 20;
	/** How fast the ray turns toward its target (0-1 per tick); low enough to outrun it. */
	public static final double RAY_TURN = 0.12;

	public SpectralSorrow(EntityType<? extends Ghast> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
		this.xpReward = 50;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Ghast.createAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.FOLLOW_RANGE, 64.0);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		// The ray replaces the fireballs.
		this.goalSelector.removeAllGoals(goal -> goal.getClass().getSimpleName().equals("GhastShootFireballGoal"));
		this.goalSelector.addGoal(7, new RayGoal(this));
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		this.spawnAtLocation(level, new ItemStack(ModPaths.BLOOD_CRYSTALLIZATION));
	}

	/**
	 * One tick of the ray from the mouth along `direction`: flames and red dust up to the first block, and every
	 * RAY_HIT_TICKS (when `hit`) 6 damage and 3 s of fire to every living thing in it. Returns the ray's end.
	 */
	public Vec3 ray(ServerLevel level, Vec3 direction, boolean hit) {
		Vec3 from = this.position().add(0.0, 1.6, 0.0).add(direction.scale(2.2));
		Vec3 end = from.add(direction.scale(RAY_RANGE));
		HitResult blocks = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		if (blocks.getType() != HitResult.Type.MISS) {
			end = blocks.getLocation();
		}
		Vec3 dir = end.subtract(from);
		double length = dir.length();
		for (double d = 0.0; d < length; d += 0.6) {
			Vec3 at = from.add(dir.scale(d / length));
			level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 1, 0.08, 0.08, 0.08, 0.005);
			if (d % 1.8 < 0.6) {
				level.sendParticles(new DustParticleOptions(0x8A0303, 1.4F), at.x, at.y, at.z, 1, 0.1, 0.1, 0.1, 0.0);
			}
		}
		if (hit) {
			Vec3 tip = end;
			for (Entity entity : level.getEntities(this, new AABB(from, tip).inflate(1.0),
					e -> e instanceof LivingEntity living && living.isAlive() && !(e instanceof SpectralSorrow)
							&& e.getBoundingBox().inflate(0.3).clip(from, tip).isPresent())) {
				LivingEntity living = (LivingEntity) entity;
				living.hurtServer(level, this.damageSources().mobAttack(this), RAY_DAMAGE);
				living.igniteForSeconds(3);
			}
		}
		return end;
	}

	/** Cry, breathe the ray at the target (it turns slowly toward it), rest; again while there is a target in sight. */
	static class RayGoal extends Goal {
		private final SpectralSorrow sorrow;
		private int timer;
		private Vec3 aim;

		RayGoal(SpectralSorrow sorrow) {
			this.sorrow = sorrow;
		}

		@Override
		public boolean canUse() {
			return sorrow.getTarget() != null;
		}

		@Override
		public void start() {
			timer = 0;
			aim = null;
		}

		@Override
		public void stop() {
			sorrow.setCharging(false);
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity target = sorrow.getTarget();
			if (target == null || !(sorrow.level() instanceof ServerLevel level)) {
				return;
			}
			Vec3 mouth = sorrow.position().add(0.0, 1.6, 0.0);
			Vec3 wanted = target.getEyePosition().subtract(mouth).normalize();
			boolean seen = sorrow.hasLineOfSight(target) && target.distanceToSqr(sorrow) < RAY_RANGE * RAY_RANGE;
			if (timer == 0 && !seen) {
				return;
			}
			timer++;
			if (timer == 1) {
				sorrow.setCharging(true);
				level.playSound(null, sorrow.blockPosition(), SoundEvents.GHAST_WARN, SoundSource.HOSTILE, 3.0F, 0.6F);
			} else if (timer <= CHARGE_TICKS) {
				aim = wanted;
			} else if (timer <= CHARGE_TICKS + RAY_TICKS) {
				aim = aim == null ? wanted : aim.lerp(wanted, RAY_TURN).normalize();
				int t = timer - CHARGE_TICKS;
				sorrow.ray(level, aim, t % RAY_HIT_TICKS == 0);
				if (t % 10 == 1) {
					level.playSound(null, sorrow.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, 0.5F);
				}
			} else if (timer == CHARGE_TICKS + RAY_TICKS + 1) {
				sorrow.setCharging(false);
			} else if (timer >= CHARGE_TICKS + RAY_TICKS + PAUSE_TICKS) {
				timer = 0;
			}
		}
	}
}
