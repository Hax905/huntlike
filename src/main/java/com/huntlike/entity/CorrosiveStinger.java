package com.huntlike.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import com.huntlike.gear.ModKoro;

/** Koro Whakamate's Corrosive Stinger: a black stinger flying like a fully drawn arrow, 12 damage on hit. */
public class CorrosiveStinger extends ThrowableItemProjectile {
	public static final float DAMAGE = 12.0F;

	public CorrosiveStinger(EntityType<? extends CorrosiveStinger> type, Level level) {
		super(type, level);
	}

	public CorrosiveStinger(Level level, LivingEntity owner) {
		super(ModEntities.CORROSIVE_STINGER, owner, level, new ItemStack(ModKoro.CORROSIVE_STINGER));
	}

	@Override
	protected Item getDefaultItem() {
		return ModKoro.CORROSIVE_STINGER;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.05;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return entity instanceof LivingEntity && super.canHitEntity(entity);
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		if (this.level() instanceof ServerLevel level) {
			result.getEntity().hurtServer(level, this.damageSources().thrown(this, this.getOwner()), DAMAGE);
		}
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (this.level() instanceof ServerLevel level) {
			level.sendParticles(new DustParticleOptions(0x151515, 1.4F), getX(), getY(), getZ(), 12, 0.2, 0.2, 0.2, 0.0);
			this.discard();
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel level) {
			level.sendParticles(new DustParticleOptions(0x202020, 0.9F), getX(), getY(), getZ(), 1, 0.0, 0.0, 0.0, 0.0);
			if (this.tickCount > 200) {
				this.discard();
			}
		}
	}
}
