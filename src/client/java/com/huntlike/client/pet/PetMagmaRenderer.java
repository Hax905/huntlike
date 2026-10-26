package com.huntlike.client.pet;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.MagmaCubeModel;
import net.minecraft.client.renderer.entity.AbstractCubeMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import com.huntlike.pet.PetMagma;

/** The Magma Cube pet: the vanilla magma cube look, always fully lit. */
public class PetMagmaRenderer extends AbstractCubeMobRenderer<PetMagma, SlimeRenderState, MagmaCubeModel> {
	private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/slime/magmacube.png");

	public PetMagmaRenderer(EntityRendererProvider.Context context) {
		super(context, new MagmaCubeModel(context.bakeLayer(ModelLayers.MAGMA_CUBE)));
	}

	@Override
	protected int getBlockLightLevel(PetMagma entity, BlockPos pos) {
		return 15;
	}

	@Override
	protected void scale(SlimeRenderState state, PoseStack pose) {
		this.applySizeAndSquish(state, pose);
	}

	@Override
	public Identifier getTextureLocation(SlimeRenderState state) {
		return TEXTURE;
	}

	@Override
	public SlimeRenderState createRenderState() {
		return new SlimeRenderState();
	}
}
