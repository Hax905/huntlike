package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MagmaCubeRenderer;

/**
 * Renderer template for Variance's magma cube bodies (Cluster Tier IV). For now it draws the vanilla magma cube. When custom art is ready, override
 * getTextureLocation(...) to return HuntLike.id("textures/entity/<name>.png").
 */
public class VarianceMagmaRenderer extends MagmaCubeRenderer {
	public VarianceMagmaRenderer(EntityRendererProvider.Context context) {
		super(context);
	}
}
