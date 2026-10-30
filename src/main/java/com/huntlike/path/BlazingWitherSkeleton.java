package com.huntlike.path;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A wither skeleton fed two Blazing Hearts ★: 80 health, 12 contact damage, 30% bigger. Launches a wither skull (12
 * damage) as the fight starts and every 10 seconds after. Drops a Wither Skeleton Skull and a Blazing Heart ★★.
 */
public class BlazingWitherSkeleton extends WitherSkeleton {
	public static final float HEALTH = 80.0F;
	public static final float DAMAGE = 12.0F;
	public static final float SKULL_DAMAGE = 12.0F;
	public static final int SKULL_TICKS = 10 * 20;

	private int skullTimer;
	/** Skulls launched so far (for the tests). */
	public int skullsShot;
	private boolean fighting;

	public BlazingWitherSkeleton(EntityType<? extends WitherSkeleton> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
		this.xpReward = 30;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return net.minecraft.world.entity.monster.skeleton.AbstractSkeleton.createAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.ATTACK_DAMAGE, DAMAGE)
				.add(Attributes.SCALE, 1.3).add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		// Only its feeder is a target.
		this.targetSelector.removeAllGoals(goal -> true);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (BlazingMob.burnOut(this, level, "Blazing Wither Skeleton")) {
			return;
		}
		BlazingMob.huntFeeder(this, level);
		LivingEntity target = this.getTarget();
		if (target == null) {
			fighting = false;
			return;
		}
		if (!fighting) {
			fighting = true;
			skullTimer = 0;
		}
		if (--skullTimer <= 0) {
			skullTimer = SKULL_TICKS;
			launchSkull(level, target);
		}
	}

	private void launchSkull(ServerLevel level, LivingEntity target) {
		Vec3 from = this.getEyePosition();
		Vec3 dir = target.getEyePosition().subtract(from).normalize();
		WitherSkull skull = new WitherSkull(level, this, dir);
		skull.setPos(from.add(dir.scale(0.8)));
		level.addFreshEntity(skull);
		skullsShot++;
		level.playSound(null, this.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		this.spawnAtLocation(level, new ItemStack(Items.WITHER_SKELETON_SKULL));
		this.spawnAtLocation(level, new ItemStack(ModPaths.BLAZING_HEART_STAR2));
	}
}
