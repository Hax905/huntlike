package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Renderer template for the Tier II boss.
 *
 * For now it draws the vanilla pig model and texture. When custom art is ready:
 * - Texture: override getTextureLocation(...) to return HuntLike.id("textures/entity/<name>.png").
 * - Model: export the model from Blockbench as a Java entity model, register its layer, and extend
 *   MobRenderer with that model instead of PigRenderer.
 */
public class HylochoerusRenderer extends PigRenderer {
	public HylochoerusRenderer(EntityRendererProvider.Context context) {
		super(context);
	}
}
