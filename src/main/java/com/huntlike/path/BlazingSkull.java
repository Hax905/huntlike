package com.huntlike.path;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** The Blazing Wither Skeleton's skull: 12 damage and only 5 seconds of Wither (a normal wither skull's lasts much longer). */
public class BlazingSkull extends WitherSkull {
	public static final int WITHER_TICKS = 5 * 20;

	public BlazingSkull(Level level, LivingEntity owner, Vec3 direction) {
		super(level, owner, direction);
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}
		Entity target = result.getEntity();
		boolean hurt = target.hurtServer(level, this.damageSources().witherSkull(this, this.getOwner()), BlazingWitherSkeleton.SKULL_DAMAGE);
		if (hurt && target instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_TICKS, 1), this.getEffectSource());
		}
	}
}
