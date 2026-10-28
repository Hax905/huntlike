package com.huntlike.mixin;

import org.spongepowered.asm.mixin.Mixin;
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

	/** Slimy Rebounders: every block bounces the wearer back up like a slime block. */
	@Inject(method = "getEntityBounciness", at = @At("RETURN"), cancellable = true)
	private void huntlike$rebounders(CallbackInfoReturnable<Double> cir) {
		if (com.huntlike.interlude.SlimyRebounders.bounces((LivingEntity) (Object) this)) {
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
