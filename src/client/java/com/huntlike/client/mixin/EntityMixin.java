package com.huntlike.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;

import com.huntlike.client.ClientHuntState;

/**
 * Spiritual animals (lured for the Animal Hunt, or found by the Spiritual Catalyst or Compass) glow light blue. The mobs
 * changed by Blazing Hearts (Blazing Piglin, Blazing Wither Skeleton, Demonling) glow white, yellow, orange, red and
 * back, half a second each.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	/** White, yellow, orange, red, orange, yellow (then again). */
	private static final int[] FIRE_CYCLE = {0xFFFFFF, 0xFFFF55, 0xFFAA00, 0xFF5555, 0xFFAA00, 0xFFFF55};
	private static final int FIRE_STEP_TICKS = 10;

	@Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
	private void huntlike$spiritColor(CallbackInfoReturnable<Integer> cir) {
		Entity entity = (Entity) (Object) this;
		if (!entity.level().isClientSide()) {
			return;
		}
		if (entity instanceof com.huntlike.path.BlazingPiglin || entity instanceof com.huntlike.path.BlazingWitherSkeleton
				|| entity instanceof com.huntlike.path.Demonling) {
			cir.setReturnValue(FIRE_CYCLE[(int) (entity.level().getGameTime() / FIRE_STEP_TICKS % FIRE_CYCLE.length)]);
			return;
		}
		if (entity instanceof com.huntlike.path.Evilborn) {
			cir.setReturnValue(0xAA0000);
			return;
		}
		Long until = ClientHuntState.highlights.get(entity.getId());
		if (until != null && entity.level().getGameTime() < until) {
			cir.setReturnValue(ClientHuntState.SPIRIT_COLOR);
		}
	}
}
