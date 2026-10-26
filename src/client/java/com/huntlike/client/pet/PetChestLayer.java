package com.huntlike.client.pet;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * The chest a big pet carries, drawn inside its body: a small chest around the inner core, seen through the translucent shell.
 */
public class PetChestLayer<M extends EntityModel<? super PetRenderState>> extends RenderLayer<PetRenderState, M> {
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/chest/normal.png");
	/** Chest size inside the slime, in model pixels (the inner core is 6, the shell 8). */
	private static final float CHEST_PIXELS = 7.2F;

	private final ChestModel chest;

	public PetChestLayer(RenderLayerParent<PetRenderState, M> parent, EntityModelSet models) {
		super(parent);
		this.chest = new ChestModel(models.bakeLayer(ModelLayers.CHEST));
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, PetRenderState state, float yRot, float xRot) {
		if (!state.chest || state.isInvisible) {
			return;
		}
		pose.pushPose();
		// Model space: y points down, the shell spans 16 to 24 pixels. Centre the chest (14 pixels tall) in the body.
		float scale = CHEST_PIXELS / 14.0F;
		pose.translate(0.0F, 20.0F / 16.0F, 0.0F);
		pose.scale(-scale, -scale, scale);
		pose.translate(-0.5F, -7.0F / 16.0F, -0.5F);
		this.chest.setupAnim(0.0F);
		collector.submitModel(this.chest, 0.0F, pose, RenderTypes.entityCutout(TEXTURE), light,
				LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor);
		pose.popPose();
	}
}
