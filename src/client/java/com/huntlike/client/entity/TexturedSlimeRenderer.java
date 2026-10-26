package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;

/** A Cluster boss drawn with the slime model and its own texture (inner body and translucent shell). */
public class TexturedSlimeRenderer extends SlimeRenderer {
	private final Identifier texture;

	public TexturedSlimeRenderer(EntityRendererProvider.Context context, Identifier texture) {
		super(context);
		this.texture = texture;
		// The vanilla outer layer always uses the green slime texture: draw the shell with this boss's texture instead.
		this.layers.removeIf(layer -> layer instanceof net.minecraft.client.renderer.entity.layers.SlimeOuterLayer);
		this.addLayer(new OuterLayer(this, context.getModelSet(), texture));
	}

	@Override
	public Identifier getTextureLocation(SlimeRenderState state) {
		return texture;
	}

	static class OuterLayer extends net.minecraft.client.renderer.entity.layers.RenderLayer<SlimeRenderState,
			net.minecraft.client.model.monster.slime.SlimeModel> {
		private final net.minecraft.client.model.monster.slime.SlimeModel model;
		private final Identifier texture;

		OuterLayer(net.minecraft.client.renderer.entity.RenderLayerParent<SlimeRenderState,
				net.minecraft.client.model.monster.slime.SlimeModel> parent, net.minecraft.client.model.geom.EntityModelSet models,
				Identifier texture) {
			super(parent);
			this.texture = texture;
			this.model = new net.minecraft.client.model.monster.slime.SlimeModel(
					models.bakeLayer(net.minecraft.client.model.geom.ModelLayers.SLIME_OUTER));
		}

		@Override
		public void submit(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector,
				int light, SlimeRenderState state, float yRot, float xRot) {
			if (state.isInvisible) {
				return;
			}
			this.model.setupAnim(state);
			collector.submitModel(this.model, state, pose,
					net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(texture), light,
					net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor);
		}
	}
}
