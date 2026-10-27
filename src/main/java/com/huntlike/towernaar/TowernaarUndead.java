package com.huntlike.towernaar;

import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Towernaar the Undead, "The Dark Practicant" (Final Chapter): 300 health, 10 damage. Teleports every 10 seconds and throws
 * end stone every 4. When beaten he stays at 1 health, still, until his last words.
 */
public class TowernaarUndead extends Monster {
	public static final float HEALTH = 300.0F;
	public static final float DAMAGE = 10.0F;
	public static final int TELEPORT_TICKS = 10 * 20;
	public static final int THROW_TICKS = 4 * 20;

	private static final EntityDataAccessor<String> TARGET = SynchedEntityData.defineId(TowernaarUndead.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Boolean> ENDING = SynchedEntityData.defineId(TowernaarUndead.class, EntityDataSerializers.BOOLEAN);

	private int teleportTimer = TELEPORT_TICKS;
	private int throwTimer = THROW_TICKS;
	/** Only true for the final blow after his last words. */
	boolean allowDeath;

	public TowernaarUndead(EntityType<? extends TowernaarUndead> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, HEALTH)
				.add(Attributes.ATTACK_DAMAGE, DAMAGE)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FOLLOW_RANGE, 64.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
				.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(TARGET, "");
		builder.define(ENDING, false);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
	}

	public UUID hunted() {
		String id = this.entityData.get(TARGET);
		try {
			return id.isEmpty() ? null : UUID.fromString(id);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public void setHunted(UUID player) {
		this.entityData.set(TARGET, player.toString());
	}

	public boolean isEnding() {
		return this.entityData.get(ENDING);
	}

	@Override
	public int getTeamColor() {
		return 0xAA00AA;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (isEnding()) {
			return;
		}
		LivingEntity target = this.getTarget();
		if (target == null || !target.isAlive()) {
			return;
		}
		if (--teleportTimer <= 0) {
			teleportTimer = TELEPORT_TICKS;
			teleportNear(level, target);
		}
		if (--throwTimer <= 0) {
			throwTimer = THROW_TICKS;
			throwBlock(level, target);
		}
	}

	/** Teleports somewhere 3 to 8 blocks from the target (breaks combos). */
	private void teleportNear(ServerLevel level, LivingEntity target) {
		for (int attempt = 0; attempt < 16; attempt++) {
			double angle = this.random.nextDouble() * Math.PI * 2.0;
			double distance = 3.0 + this.random.nextDouble() * 5.0;
			double x = target.getX() + Math.cos(angle) * distance;
			double z = target.getZ() + Math.sin(angle) * distance;
			double y = target.getY() + this.random.nextInt(5) - 2;
			double fromX = this.getX();
			double fromY = this.getY();
			double fromZ = this.getZ();
			if (this.randomTeleport(x, y, z, true, state -> true)) {
				level.sendParticles(ParticleTypes.PORTAL, fromX, fromY + 1.4, fromZ, 30, 0.3, 0.8, 0.3, 0.2);
				level.playSound(null, fromX, fromY, fromZ, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.8F);
				return;
			}
		}
	}

	private void throwBlock(ServerLevel level, LivingEntity target) {
		ThrownEndStone stone = new ThrownEndStone(level, this);
		stone.setPos(this.getX(), this.getEyeY() - 0.3, this.getZ());
		double dx = target.getX() - stone.getX();
		double dz = target.getZ() - stone.getZ();
		double dy = target.getY(0.5) - stone.getY();
		double flat = Math.sqrt(dx * dx + dz * dz);
		stone.shoot(dx, dy + flat * 0.15, dz, 1.2F, 1.0F);
		level.addFreshEntity(stone);
		this.swingForAttack(net.minecraft.world.InteractionHand.MAIN_HAND);
		level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 0.8F, 0.7F);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (isEnding() && !allowDeath) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	/** Beaten: he stays at 1 health, still, until the fight lets him go. */
	@Override
	public void die(DamageSource source) {
		if (!allowDeath) {
			this.setHealth(1.0F);
			this.entityData.set(ENDING, true);
			this.setNoAi(true);
			this.setTarget(null);
			this.getNavigation().stop();
			return;
		}
		super.die(source);
	}

	@Override
	public void checkDespawn() {
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel level) {
			TowernaarFight.tickBoss(this, level);
		}
	}
}
