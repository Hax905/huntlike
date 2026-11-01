package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.GhastRenderer;
import net.minecraft.client.renderer.entity.state.GhastRenderState;
import net.minecraft.resources.Identifier;

import com.huntlike.HuntLike;

/** The Spectral Sorrow: the ghast's model with its own red skin (its shooting face while it cries and breathes the ray). */
public class SpectralSorrowRenderer extends GhastRenderer {
	private static final Identifier TEXTURE = HuntLike.id("textures/entity/path/spectral_sorrow.png");
	private static final Identifier SHOOTING = HuntLike.id("textures/entity/path/spectral_sorrow_shooting.png");

	public SpectralSorrowRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public Identifier getTextureLocation(GhastRenderState state) {
		return state.isCharging ? SHOOTING : TEXTURE;
	}
}
