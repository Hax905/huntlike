package com.huntlike.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import com.llamalad7.mixinextras.sugar.Local;

import com.huntlike.gear.GearEffects;
import com.huntlike.hunt.BossAbilities;
import com.huntlike.hunt.BossController;

/** Hunt bosses never drop their vanilla loot; HuntLike hands out their drops itself. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	/** Hunt Weapon bonuses and Hunt Armor damage reduction. */
	@ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
	private float huntlike$gearDamage(float amount, @Local(argsOnly = true) DamageSource source) {
		return GearEffects.adjustDamage((LivingEntity) (Object) this, source, amount);
	}

	/** Where the entity last stood (on the ground, in water or on a ladder): the height a fall is measured from. */
	@Unique
	private double huntlike$standY = Double.NaN;

	@Inject(method = "tick", at = @At("HEAD"))
	private void huntlike$trackStanding(CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		// On the ground and not on the way up from a bounce, or somewhere a fall can't build up.
		if (Double.isNaN(huntlike$standY) || self.onGround() && self.getDeltaMovement().y <= 0.0 || self.isInWater()
				|| self.onClimbable() || self.isFallFlying()) {
			huntlike$standY = self.getY();
		}
	}

	/**
	 * Slimy Rebounders: a landing more than 1 block below where the wearer last stood bounces like a slime block. Walking,
	 * stepping down a block or jumping on flat ground doesn't bounce, so moving around keeps its speed.
	 */
	@Inject(method = "getEntityBounciness", at = @At("RETURN"), cancellable = true)
	private void huntlike$rebounders(CallbackInfoReturnable<Double> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (com.huntlike.interlude.SlimyRebounders.bounces(self) && !Double.isNaN(huntlike$standY)
				&& huntlike$standY - self.getY() > com.huntlike.interlude.SlimyRebounders.MIN_DROP) {
			cir.setReturnValue(Math.max(cir.getReturnValueD(),
					net.minecraft.world.level.block.Blocks.SLIME_BLOCK.getBounceRestitution()));
		}
	}

	/** Slimy Rebounders: no fall damage unless sneaking (a sneaking landing doesn't bounce, as on a slime block). */
	@Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
	private void huntlike$reboundersNoFall(double distance, float multiplier, DamageSource source,
			CallbackInfoReturnable<Boolean> cir) {
		if (com.huntlike.interlude.SlimyRebounders.bounces((LivingEntity) (Object) this)) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
	private void huntlike$skipBossLoot(ServerLevel level, DamageSource source, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (BossController.isBoss(self) || BossAbilities.isHerd(self) || com.huntlike.interlude.SlimeRiderFight.isInterlude(self)) {
			ci.cancel();
		}
	}
}
