package com.huntlike.towernaar;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** A chunk of end stone thrown by Towernaar the Undead: 10 damage, breaks on impact and never places a block. */
public class ThrownEndStone extends ThrowableItemProjectile {
	public static final float DAMAGE = 10.0F;

	public ThrownEndStone(EntityType<? extends ThrownEndStone> type, Level level) {
		super(type, level);
	}

	public ThrownEndStone(Level level, LivingEntity owner) {
		super(com.huntlike.entity.ModEntities.THROWN_END_STONE, owner, level, new ItemStack(Items.END_STONE));
	}

	@Override
	protected Item getDefaultItem() {
		return Items.END_STONE;
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return !(entity instanceof TowernaarUndead) && super.canHitEntity(entity);
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
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.END_STONE.defaultBlockState()), this.getX(), this.getY(),
					this.getZ(), 20, 0.2, 0.2, 0.2, 0.1);
			level.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.STONE_BREAK,
					net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.8F);
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
