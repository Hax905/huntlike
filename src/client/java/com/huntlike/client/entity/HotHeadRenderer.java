package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

import com.huntlike.HuntLike;

/** Hot Head (Cluster Tier III), with your texture. */
public class HotHeadRenderer extends TexturedMagmaCubeRenderer {
	public HotHeadRenderer(EntityRendererProvider.Context context) {
		super(context, HuntLike.id("textures/entity/cluster/hot_head.png"));
	}
}
