package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.client.renderer.entity.state.PigRenderState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.pig.PigVariants;

import com.huntlike.HuntLike;

/** The Tier II boss: the pig model (always the temperate one, whatever biome it spawns in) with its own skin. */
public class HylochoerusRenderer extends PigRenderer {
	private static final Identifier TEXTURE = HuntLike.id("textures/entity/animal/hylochoerus.png");

	public HylochoerusRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void extractRenderState(Pig entity, PigRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		entity.registryAccess().lookupOrThrow(Registries.PIG_VARIANT).get(PigVariants.TEMPERATE)
				.ifPresent(variant -> state.variant = variant.value());
	}

	@Override
	public Identifier getTextureLocation(PigRenderState state) {
		return TEXTURE;
	}
}
