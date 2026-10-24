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
		return state instanceof VileRenderState vile && vile.magmaCore ? MAGMA : FROST;
	}
}
