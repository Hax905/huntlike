package com.huntlike.client.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

import com.huntlike.HuntLike;
import com.huntlike.rogue.RogueHunter;

/** A Rogue Hunter: a hooded figure on the player model (a darker red one for the special hunter). */
public class RogueHunterRenderer extends HumanoidMobRenderer<RogueHunter, RogueHunterRenderer.State, HumanoidModel<RogueHunterRenderer.State>> {
	private static final Identifier TEXTURE = HuntLike.id("textures/entity/rogue/rogue_hunter.png");
	private static final Identifier SPECIAL = HuntLike.id("textures/entity/rogue/rogue_hunter_special.png");

	public static class State extends HumanoidRenderState {
		public boolean special;
	}

	public RogueHunterRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(RogueHunter entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.special = entity.isSpecial();
	}

	@Override
	public Identifier getTextureLocation(State state) {
		return state.special ? SPECIAL : TEXTURE;
	}
}
