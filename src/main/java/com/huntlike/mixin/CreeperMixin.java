package com.huntlike.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.monster.Creeper;

import com.huntlike.block.SoulMonolithBlock;

/** Creepers can't explode inside a Soul Monolith's area. */
@Mixin(Creeper.class)
public abstract class CreeperMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void huntlike$calmNearMonolith(CallbackInfo ci) {
		Creeper self = (Creeper) (Object) this;
		if (!self.level().isClientSide() && SoulMonolithBlock.protects(self.level(), self.blockPosition())) {
			self.setSwellDir(-1);
		}
	}

	@Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true, require = 0)
	private void huntlike$noExplosionNearMonolith(CallbackInfo ci) {
		Creeper self = (Creeper) (Object) this;
		if (SoulMonolithBlock.protects(self.level(), self.blockPosition())) {
			ci.cancel();
		}
	}
}
