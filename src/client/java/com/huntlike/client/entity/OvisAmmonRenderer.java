package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Renderer template for the Tier III boss.
 *
 * For now it draws the vanilla sheep model and texture. When custom art is ready:
 * - Texture: override getTextureLocation(...) to return HuntLike.id("textures/entity/<name>.png").
 * - Model: export the model from Blockbench as a Java entity model, register its layer, and extend
 *   MobRenderer with that model instead of SheepRenderer.
 */
public class OvisAmmonRenderer extends SheepRenderer {
	public OvisAmmonRenderer(EntityRendererProvider.Context context) {
		super(context);
	}
}
