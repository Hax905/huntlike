package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import com.huntlike.HuntLike;

/** Variance's slime bodies (Cluster Tier IV), with your texture. */
public class VarianceSlimeRenderer extends TexturedSlimeRenderer {
	public VarianceSlimeRenderer(EntityRendererProvider.Context context) {
		super(context, HuntLike.id("textures/entity/cluster/variance_slime.png"));
	}
}
