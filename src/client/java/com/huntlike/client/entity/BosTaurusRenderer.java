package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Renderer template for the Herd Call animal (Aurochs).
 *
 * For now it draws the vanilla cow model and texture. When custom art is ready:
 * - Texture: override getTextureLocation(...) to return HuntLike.id("textures/entity/<name>.png").
 * - Model: export the model from Blockbench as a Java entity model, register its layer, and extend
 *   MobRenderer with that model instead of CowRenderer.
 */
public class BosTaurusRenderer extends CowRenderer {
	public BosTaurusRenderer(EntityRendererProvider.Context context) {
		super(context);
	}
}
