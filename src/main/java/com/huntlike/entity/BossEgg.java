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

/** Egg Blaster's egg: a fast egg that only hits players and never hatches. */
public class BossEgg extends ThrowableItemProjectile {
	private float damage = 0.25F;

	public BossEgg(EntityType<? extends BossEgg> type, Level level) {
		super(type, level);
	}

	public BossEgg(Level level, LivingEntity owner, float damage) {
		super(ModEntities.BOSS_EGG, owner, level, new ItemStack(Items.EGG));
		this.damage = damage;
	}

	@Override
	protected Item getDefaultItem() {
		return Items.EGG;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
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
			level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.EGG),
					this.getX(), this.getY(), this.getZ(), 6, 0.1, 0.1, 0.1, 0.05);
			this.discard();
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.tickCount > 100 && !this.level().isClientSide()) {
			this.discard();
		}
	}
}
