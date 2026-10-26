package com.huntlike.entity;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Baller's projectile: a slow, heavy slimeball (magma cream for magma bodies, a snowball for a frost core).
 * Only hits players; a shield blocks it like any projectile.
 */
public class ClusterBall extends ThrowableItemProjectile {
	private float damage = 8.0F;
	/** A slime pet's ball: hits mobs, never players or its owner's pets. */
	private boolean petBall;

	public ClusterBall(EntityType<? extends ClusterBall> type, Level level) {
		super(type, level);
	}

	public ClusterBall(Level level, LivingEntity owner, Item item, float damage) {
		this(level, owner, item, damage, false);
	}

	public ClusterBall(Level level, LivingEntity owner, Item item, float damage, boolean petBall) {
		super(ModEntities.CLUSTER_BALL, owner, level, new ItemStack(item));
		this.damage = damage;
		this.petBall = petBall;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.SLIME_BALL;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		if (petBall) {
			return entity instanceof LivingEntity && !(entity instanceof Player) && !(entity instanceof com.huntlike.pet.PetCube)
					&& super.canHitEntity(entity);
		}
		return entity instanceof Player && super.canHitEntity(entity);
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		if (this.level() instanceof ServerLevel level) {
			result.getEntity().hurtServer(level, this.damageSources().thrown(this, this.getOwner()), this.damage);
		}
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (this.level() instanceof ServerLevel level) {
			level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, this.getItem().getItem()),
					this.getX(), this.getY(), this.getZ(), 10, 0.15, 0.15, 0.15, 0.08);
			this.discard();
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.tickCount > 200 && !this.level().isClientSide()) {
			this.discard();
		}
	}
}
