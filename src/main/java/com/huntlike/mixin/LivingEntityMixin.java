package com.huntlike.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import com.huntlike.hunt.BossController;

/** Hunt bosses never drop their vanilla loot; HuntLike hands out their drops itself. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
	private void huntlike$skipBossLoot(ServerLevel level, DamageSource source, CallbackInfo ci) {
		if (BossController.isBoss((LivingEntity) (Object) this)) {
			ci.cancel();
		}
	}
}
