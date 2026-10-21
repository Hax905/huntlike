package com.huntlike.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.PowderSnowBlock;

import com.huntlike.gear.HideBoots;

/** Hide Boots walk on Powder Snow like Leather Boots. */
@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {
	@Inject(method = "canEntityWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
	private static void huntlike$hideBoots(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (HideBoots.walksOnPowderSnow(entity)) {
			cir.setReturnValue(true);
		}
	}
}
