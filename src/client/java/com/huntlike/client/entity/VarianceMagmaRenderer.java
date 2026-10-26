package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import com.huntlike.HuntLike;

/** Variance's magma cube bodies (Cluster Tier IV), with your texture. */
public class VarianceMagmaRenderer extends TexturedMagmaCubeRenderer {
	public VarianceMagmaRenderer(EntityRendererProvider.Context context) {
		super(context, HuntLike.id("textures/entity/cluster/variance_magma.png"));
	}
}
