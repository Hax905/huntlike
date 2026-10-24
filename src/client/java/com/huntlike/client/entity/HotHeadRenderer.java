package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MagmaCubeRenderer;

/**
 * Renderer template for Hot Head (Cluster Tier III). For now it draws the vanilla magma cube. When custom art is ready, override
 * getTextureLocation(...) to return HuntLike.id("textures/entity/<name>.png").
 */
public class HotHeadRenderer extends MagmaCubeRenderer {
	public HotHeadRenderer(EntityRendererProvider.Context context) {
		super(context);
	}
}
