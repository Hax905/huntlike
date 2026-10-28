package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CowRenderState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.CowVariants;

import com.huntlike.HuntLike;

/** The Tier IV boss: the cow model (always the temperate one, whatever biome it spawns in) with its own skin. */
public class AurochsRenderer extends CowRenderer {
	private static final Identifier TEXTURE = HuntLike.id("textures/entity/animal/aurochs.png");

	public AurochsRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void extractRenderState(Cow entity, CowRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		entity.registryAccess().lookupOrThrow(Registries.COW_VARIANT).get(CowVariants.TEMPERATE)
				.ifPresent(variant -> state.variant = variant.value());
	}

	@Override
	public Identifier getTextureLocation(CowRenderState state) {
		return TEXTURE;
	}
}
