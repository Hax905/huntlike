package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Renderer template for the Tier IV boss.
 *
 * For now it draws the vanilla cow model and texture. When custom art is ready:
 * - Texture: override getTextureLocation(...) to return HuntLike.id("textures/entity/<name>.png").
 * - Model: export the model from Blockbench as a Java entity model, register its layer, and extend
 *   MobRenderer with that model instead of CowRenderer.
 */
public class AurochsRenderer extends CowRenderer {
	public AurochsRenderer(EntityRendererProvider.Context context) {
		super(context);
	}
}
