package com.huntlike.pet;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** The slime form of a player's pet. */
public class PetSlime extends PetCube {
	public PetSlime(EntityType<? extends PetSlime> type, Level level) {
		super(type, level);
	}

	@Override
	public boolean isMagma() {
		return false;
	}

	@Override
	protected ParticleOptions getParticleType() {
		return ParticleTypes.ITEM_SLIME;
	}

	@Override
	protected SoundEvent getJumpSound() {
		return this.isTiny() ? SoundEvents.SLIME_JUMP_SMALL : SoundEvents.SLIME_JUMP;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return this.isTiny() ? SoundEvents.SLIME_HURT_SMALL : SoundEvents.SLIME_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return this.isTiny() ? SoundEvents.SLIME_DEATH_SMALL : SoundEvents.SLIME_DEATH;
	}

	@Override
	protected SoundEvent getSquishSound() {
		return this.isTiny() ? SoundEvents.SLIME_SQUISH_SMALL : SoundEvents.SLIME_SQUISH;
	}

	@Override
	protected SoundEvent getAttackSound() {
		return SoundEvents.SLIME_ATTACK;
	}
}
