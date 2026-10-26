package com.huntlike.client.pet;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.entity.AbstractCubeMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.resources.Identifier;

import com.huntlike.pet.PetSlime;

/** The slime pet: the vanilla slime look (the pet is its own entity so it isn't a hostile mob). */
public class PetSlimeRenderer extends AbstractCubeMobRenderer<PetSlime, PetRenderState, SlimeModel> {
	public PetSlimeRenderer(EntityRendererProvider.Context context) {
		super(context, new SlimeModel(context.bakeLayer(ModelLayers.SLIME)));
		this.addLayer(new PetChestLayer<>(this, context.getModelSet()));
		this.addLayer(new OuterLayer(this, context.getModelSet()));
	}

	/** The translucent shell, drawn after the chest so the chest shows through it. */
	static class OuterLayer extends net.minecraft.client.renderer.entity.layers.RenderLayer<PetRenderState, SlimeModel> {
		private final SlimeModel model;

		OuterLayer(net.minecraft.client.renderer.entity.RenderLayerParent<PetRenderState, SlimeModel> parent,
				net.minecraft.client.model.geom.EntityModelSet models) {
			super(parent);
			this.model = new SlimeModel(models.bakeLayer(ModelLayers.SLIME_OUTER));
		}

		@Override
		public void submit(PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector, int light, PetRenderState state,
				float yRot, float xRot) {
			if (state.isInvisible) {
				return;
			}
			this.model.setupAnim(state);
			collector.submitModel(this.model, state, pose,
					net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(SlimeRenderer.SLIME_LOCATION), light,
					net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor);
		}
	}

	@Override
	protected void scale(PetRenderState state, PoseStack pose) {
		this.downscaleSlightly(pose);
		this.applySizeAndSquish(state, pose);
	}

	@Override
	public Identifier getTextureLocation(PetRenderState state) {
		return SlimeRenderer.SLIME_LOCATION;
	}

	@Override
	public PetRenderState createRenderState() {
		return new PetRenderState();
	}

	@Override
	public void extractRenderState(PetSlime pet, PetRenderState state, float partialTick) {
		super.extractRenderState(pet, state, partialTick);
		state.chest = pet.hasChest();
	}
}
