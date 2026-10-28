package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.wolf.Wolf;

import com.huntlike.HuntLike;
import com.huntlike.entity.animal.EpicyonHaydeni;

/** The Tier V boss: the wolf model with its own skin, and its enraged skin once Apex Instinct kicks in. */
public class EpicyonHaydeniRenderer extends WolfRenderer {
	private static final Identifier TEXTURE = HuntLike.id("textures/entity/animal/epicyon_haydeni.png");
	private static final Identifier ENRAGED = HuntLike.id("textures/entity/animal/epicyon_haydeni_enraged.png");

	public EpicyonHaydeniRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void extractRenderState(Wolf entity, WolfRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.texture = entity instanceof EpicyonHaydeni epicyon && epicyon.isEnraged() ? ENRAGED : TEXTURE;
	}

	@Override
	public Identifier getTextureLocation(WolfRenderState state) {
		return state.texture;
	}
}
