package com.huntlike.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;

import com.huntlike.hunt.ClusterQuota;

/** A slime's or magma cube's pieces stay in its cluster (and keep its Spirit Lure owner). */
@Mixin(AbstractCubeMob.class)
public abstract class AbstractCubeMobMixin {
	@Inject(method = "setUpSplitCube", at = @At("HEAD"))
	private void huntlike$copyClusterTags(AbstractCubeMob child, int size, float offsetX, float offsetZ, CallbackInfo ci) {
		ClusterQuota.copyTags((AbstractCubeMob) (Object) this, child);
	}
}
