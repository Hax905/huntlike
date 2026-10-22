package com.huntlike.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import com.huntlike.client.ClientHuntState;

/** The quest owner, and only the owner, sees their boss glow; the Spiritual Catalyst user sees the soul it found. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
	private void huntlike$bossGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (ClientHuntState.bossEntityId != -1 && entity.getId() == ClientHuntState.bossEntityId) {
			cir.setReturnValue(true);
		} else if (ClientHuntState.highlightId != -1 && entity.getId() == ClientHuntState.highlightId) {
			if (entity.level().getGameTime() < ClientHuntState.highlightUntil) {
				cir.setReturnValue(true);
			} else {
				ClientHuntState.highlightId = -1;
			}
		}
	}
}
