package com.huntlike.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.level.Level;

import com.huntlike.path.Demonling;
import com.huntlike.path.Imp;

/** Imps and Demonlings don't smoke: the blaze's thick smoke would hide their models. */
@Mixin(Blaze.class)
public abstract class BlazeMixin {
	@WrapWithCondition(method = "aiStep", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
	private boolean huntlike$thinSmoke(Level level, ParticleOptions particle, double x, double y, double z, double dx, double dy, double dz) {
		return !((Object) this instanceof Imp || (Object) this instanceof Demonling);
	}
}
