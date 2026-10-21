package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Renderer template for the Tier V boss.
 *
 * For now it draws the vanilla wolf model and texture. When custom art is ready:
 * - Texture: override getTextureLocation(...) to return HuntLike.id("textures/entity/<name>.png").
 * - Model: export the model from Blockbench as a Java entity model, register its layer, and extend
 *   MobRenderer with that model instead of WolfRenderer.
 */
public class EpicyonHaydeniRenderer extends WolfRenderer {
	public EpicyonHaydeniRenderer(EntityRendererProvider.Context context) {
		super(context);
	}
}
