package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SlimeRenderer;

/**
 * Renderer template for Cubert (Cluster Tier I). For now it draws the vanilla slime. When custom art is ready, override
 * getTextureLocation(...) to return HuntLike.id("textures/entity/<name>.png").
 */
public class CubertRenderer extends SlimeRenderer {
	public CubertRenderer(EntityRendererProvider.Context context) {
		super(context);
	}
}
