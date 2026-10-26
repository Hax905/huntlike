package com.huntlike.client.pet;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.huntlike.pet.PetCube;

/** The chest a big pet carries, drawn in the middle of its body and seen through the translucent shell. */
public class PetChestLayer<M extends EntityModel<? super PetRenderState>> extends RenderLayer<PetRenderState, M> {
	private static final ItemStack CHEST = new ItemStack(Items.CHEST);
	/** Chest size inside the slime, in model pixels (the inner core is 6, the shell 8). */
	private static final float CHEST_PIXELS = 7.0F;

	public PetChestLayer(RenderLayerParent<PetRenderState, M> parent, EntityModelSet models) {
		super(parent);
	}

	/** Fills the render state's chest item (called from the renderer's extractRenderState). */
	public static void extract(ItemModelResolver resolver, PetCube pet, PetRenderState state) {
		state.chest = pet.hasChest();
		state.chestItem.clear();
		if (state.chest) {
			resolver.updateForLiving(state.chestItem, CHEST, ItemDisplayContext.NONE, pet);
		}
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, PetRenderState state, float yRot, float xRot) {
		if (!state.chest || state.chestItem.isEmpty() || state.isInvisible) {
			return;
		}
		pose.pushPose();
		// Model space points down; the shell spans 16 to 24 pixels. The item is a 1-block cube centred on the origin.
		pose.translate(0.0F, 20.0F / 16.0F, 0.0F);
		// Turned upright again (a half turn around Z is flipping x and y).
		float scale = CHEST_PIXELS / 16.0F;
		pose.scale(-scale, -scale, scale);
		state.chestItem.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
		pose.popPose();
	}
}
