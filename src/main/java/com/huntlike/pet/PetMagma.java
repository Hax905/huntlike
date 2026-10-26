package com.huntlike.pet;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** The Magma Cube form of a player's pet: fire immune, swims in lava, sets what it hits on fire. */
public class PetMagma extends PetCube {
	public PetMagma(EntityType<? extends PetMagma> type, Level level) {
		super(type, level);
	}

	@Override
	public boolean isMagma() {
		return true;
	}

	@Override
	protected ParticleOptions getParticleType() {
		return ParticleTypes.FLAME;
	}

	@Override
	protected SoundEvent getJumpSound() {
		return this.isTiny() ? SoundEvents.MAGMA_CUBE_JUMP : SoundEvents.MAGMA_CUBE_JUMP;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return this.isTiny() ? SoundEvents.MAGMA_CUBE_HURT_SMALL : SoundEvents.MAGMA_CUBE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return this.isTiny() ? SoundEvents.MAGMA_CUBE_DEATH_SMALL : SoundEvents.MAGMA_CUBE_DEATH;
	}

	@Override
	protected SoundEvent getSquishSound() {
		return this.isTiny() ? SoundEvents.MAGMA_CUBE_SQUISH_SMALL : SoundEvents.MAGMA_CUBE_SQUISH;
	}

	@Override
	protected SoundEvent getAttackSound() {
		return SoundEvents.SLIME_ATTACK;
	}
}
