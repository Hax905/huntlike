package com.huntlike.client.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import com.huntlike.HuntLike;
import com.huntlike.towernaar.TowernaarUndead;

/** Towernaar the Undead: the enderman model with his own body and glowing eyes, always enraged. */
public class TowernaarUndeadRenderer extends MobRenderer<TowernaarUndead, EndermanRenderState, EndermanModel<EndermanRenderState>> {
	private static final Identifier TEXTURE = HuntLike.id("textures/entity/towernaar/towernaar.png");
	private static final RenderType EYES = RenderTypes.eyes(HuntLike.id("textures/entity/towernaar/towernaar_eyes.png"));

	public TowernaarUndeadRenderer(EntityRendererProvider.Context context) {
		super(context, new EndermanModel<>(context.bakeLayer(ModelLayers.ENDERMAN)), 0.5F);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return EYES;
			}
		});
	}

	@Override
	public EndermanRenderState createRenderState() {
		return new EndermanRenderState();
	}

	@Override
	public void extractRenderState(TowernaarUndead entity, EndermanRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTick, this.itemModelResolver);
		state.isCreepy = !entity.isEnding();
	}

	@Override
	public Identifier getTextureLocation(EndermanRenderState state) {
		return TEXTURE;
	}
}
