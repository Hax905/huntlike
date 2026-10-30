package com.huntlike.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;

import com.huntlike.client.ClientHuntState;

/** Spiritual animals (lured for the Animal Hunt, or found by the Spiritual Catalyst or Compass) glow light blue. */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
	private void huntlike$spiritColor(CallbackInfoReturnable<Integer> cir) {
		Entity entity = (Entity) (Object) this;
		if (!entity.level().isClientSide()) {
			return;
		}
		Long until = ClientHuntState.highlights.get(entity.getId());
		if (until != null && entity.level().getGameTime() < until) {
			cir.setReturnValue(ClientHuntState.SPIRIT_COLOR);
		}
	}
}
