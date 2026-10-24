package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SlimeRenderer;

/**
 * Renderer template for Variance's slime bodies (Cluster Tier IV). For now it draws the vanilla slime. When custom art is ready, override
 * getTextureLocation(...) to return HuntLike.id("textures/entity/<name>.png").
 */
public class VarianceSlimeRenderer extends SlimeRenderer {
	public VarianceSlimeRenderer(EntityRendererProvider.Context context) {
		super(context);
	}
}
