package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Renderer template for the Tier I boss.
 *
 * For now it draws the vanilla chicken model and texture. When custom art is ready:
 * - Texture: override getTextureLocation(...) to return HuntLike.id("textures/entity/<name>.png").
 * - Model: export the model from Blockbench as a Java entity model, register its layer, and extend
 *   MobRenderer with that model instead of ChickenRenderer.
 */
public class SylviornisRenderer extends ChickenRenderer {
	public SylviornisRenderer(EntityRendererProvider.Context context) {
		super(context);
	}
}
