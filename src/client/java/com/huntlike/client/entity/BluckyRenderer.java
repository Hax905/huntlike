package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import com.huntlike.HuntLike;

/** Blucky (Cluster Tier II), with your texture. */
public class BluckyRenderer extends TexturedSlimeRenderer {
	public BluckyRenderer(EntityRendererProvider.Context context) {
		super(context, HuntLike.id("textures/entity/cluster/blucky.png"));
	}
}
