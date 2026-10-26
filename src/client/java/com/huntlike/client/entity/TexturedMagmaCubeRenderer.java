package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MagmaCubeRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;

/** A Cluster boss drawn with the magma cube model and its own texture. */
public class TexturedMagmaCubeRenderer extends MagmaCubeRenderer {
	private final Identifier texture;

	public TexturedMagmaCubeRenderer(EntityRendererProvider.Context context, Identifier texture) {
		super(context);
		this.texture = texture;
	}

	@Override
	public Identifier getTextureLocation(SlimeRenderState state) {
		return texture;
	}
}
