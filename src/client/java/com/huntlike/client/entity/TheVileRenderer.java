package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.cubemob.Slime;

import com.huntlike.HuntLike;
import com.huntlike.entity.cluster.TheVile;

/** The Vile (Cluster Tier V): the slime model with your Soul Cluster textures, frost or magma core. */
public class TheVileRenderer extends SlimeRenderer {
	private static final Identifier FROST = HuntLike.id("textures/entity/soul_cluster/frost.png");
	private static final Identifier MAGMA = HuntLike.id("textures/entity/soul_cluster/magma.png");

	public TheVileRenderer(EntityRendererProvider.Context context) {
		super(context);
		// The vanilla outer layer always uses the green slime texture: swap it for one with your Soul Cluster shell.
		this.layers.removeIf(layer -> layer instanceof net.minecraft.client.renderer.entity.layers.SlimeOuterLayer);
		this.addLayer(new OuterLayer(this, context.getModelSet()));
	}

	static Identifier texture(SlimeRenderState state) {
		return state instanceof VileRenderState vile && vile.magmaCore ? MAGMA : FROST;
	}

	/** The translucent outer shell, drawn with the core's texture. */
	static class OuterLayer extends net.minecraft.client.renderer.entity.layers.RenderLayer<SlimeRenderState,
			net.minecraft.client.model.monster.slime.SlimeModel> {
		private final net.minecraft.client.model.monster.slime.SlimeModel model;

		OuterLayer(net.minecraft.client.renderer.entity.RenderLayerParent<SlimeRenderState,
				net.minecraft.client.model.monster.slime.SlimeModel> parent, net.minecraft.client.model.geom.EntityModelSet models) {
			super(parent);
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
					net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(texture(state)), light,
					net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(state, 0.0F), state.outlineColor);
		}
	}

	/** Render state that also carries the core. */
	public static class VileRenderState extends SlimeRenderState {
		public boolean magmaCore;
	}

	@Override
	public SlimeRenderState createRenderState() {
		return new VileRenderState();
	}

	@Override
	public void extractRenderState(Slime slime, SlimeRenderState state, float partialTick) {
		super.extractRenderState(slime, state, partialTick);
		if (state instanceof VileRenderState vile && slime instanceof TheVile entity) {
			vile.magmaCore = entity.isMagmaCore();
		}
	}

	@Override
	public Identifier getTextureLocation(SlimeRenderState state) {
		return texture(state);
	}
}
