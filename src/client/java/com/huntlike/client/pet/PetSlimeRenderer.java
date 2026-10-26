package com.huntlike.client.pet;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.entity.AbstractCubeMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;

import com.huntlike.pet.PetSlime;

/** The slime pet: the vanilla slime look (the pet is its own entity so it isn't a hostile mob). */
public class PetSlimeRenderer extends AbstractCubeMobRenderer<PetSlime, SlimeRenderState, SlimeModel> {
	public PetSlimeRenderer(EntityRendererProvider.Context context) {
		super(context, new SlimeModel(context.bakeLayer(ModelLayers.SLIME)));
		this.addLayer(new SlimeOuterLayer(this, context.getModelSet()));
	}

	@Override
	protected void scale(SlimeRenderState state, PoseStack pose) {
		this.downscaleSlightly(pose);
		this.applySizeAndSquish(state, pose);
	}

	@Override
	public Identifier getTextureLocation(SlimeRenderState state) {
		return SlimeRenderer.SLIME_LOCATION;
	}

	@Override
	public SlimeRenderState createRenderState() {
		return new SlimeRenderState();
	}
}
