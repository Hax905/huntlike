package com.huntlike.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.animal.sheep.SheepFurModel;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SheepWoolLayer;
import net.minecraft.client.renderer.entity.layers.SheepWoolUndercoatLayer;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.Identifier;

import com.huntlike.HuntLike;

/** The Tier III boss: the sheep model with its own body skin and its own wool, dyed in the boss's (random) color. */
public class OvisAmmonRenderer extends SheepRenderer {
	private static final Identifier TEXTURE = HuntLike.id("textures/entity/animal/ovis_ammon.png");
	private static final Identifier WOOL = HuntLike.id("textures/entity/animal/ovis_ammon_wool.png");

	public OvisAmmonRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.addLayer(new com.huntlike.client.entity.horns.Horns.Layer<>(this,
				com.huntlike.client.entity.horns.Horns.Layer.bake(context, com.huntlike.client.entity.horns.Horns.OVIS_AMMON), TEXTURE));
		this.layers.removeIf(layer -> layer instanceof SheepWoolLayer || layer instanceof SheepWoolUndercoatLayer);
		this.addLayer(new Wool(this, new SheepFurModel(context.bakeLayer(ModelLayers.SHEEP_WOOL))));
	}

	@Override
	public Identifier getTextureLocation(SheepRenderState state) {
		return TEXTURE;
	}

	private static final class Wool extends RenderLayer<SheepRenderState, SheepModel> {
		private final SheepFurModel model;

		Wool(RenderLayerParent<SheepRenderState, SheepModel> parent, SheepFurModel model) {
			super(parent);
			this.model = model;
		}

		@Override
		public void submit(PoseStack pose, SubmitNodeCollector collector, int light, SheepRenderState state, float yRot, float xRot) {
			if (state.isSheared || state.isInvisible) {
				return;
			}
			coloredCutoutModelCopyLayerRender(this.model, WOOL, pose, collector, light, state, state.getWoolColor(), 1);
		}
	}
}
