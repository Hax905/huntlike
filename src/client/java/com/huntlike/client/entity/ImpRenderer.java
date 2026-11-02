package com.huntlike.client.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import com.huntlike.HuntLike;
import com.huntlike.path.Imp;

/**
 * The Imp, from the user's Blockbench model (imp.bbmodel, 64x64 sheet): a blaze-like head with two horns, four rods
 * circling above (one turn every 2 s, bobbing 1 pixel up and down every 2 s) and four below (one turn per second,
 * bobbing 2 pixels). The model was made facing south, so it is built as drawn and turned around to face forward.
 * Always fully lit.
 */
public class ImpRenderer extends MobRenderer<Imp, LivingEntityRenderState, ImpRenderer.ImpModel> {
	private static final Identifier TEXTURE = HuntLike.id("textures/entity/path/imp.png");
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(HuntLike.id("imp"), "main");
	/** Ticks per turn of the top and bottom rings; ticks per bob (up and back down). */
	public static final float TOP_TURN = 40.0F;
	public static final float BOTTOM_TURN = 20.0F;
	public static final float BOB = 40.0F;

	/** Blockbench (y up, 24 = ground) to model space (y down). */
	private static float y(float bb) {
		return 24.0F - bb;
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		// Everything sits in "turn", rotated half a turn so the painted face (south in Blockbench) looks forward.
		PartDefinition turn = root.addOrReplaceChild("turn", CubeListBuilder.create(), PartPose.rotation(0.0F, (float) Math.PI, 0.0F));
		// Head: 8x8x8 at Blockbench y 14-22, pivot in its middle (y 18).
		PartDefinition head = turn.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
				PartPose.offset(0.0F, y(18.0F), 0.0F));
		// Horns: a base leaning 45 degrees toward the face, then a straight tip.
		float[][] horns = {{-3.0F, 32.0F, 0.0F, 32.0F, 9.0F}, {2.0F, 32.0F, 3.0F, 32.0F, 6.0F}};
		for (int i = 0; i < 2; i++) {
			float x = horns[i][0];
			head.addOrReplaceChild("horn_base_" + i, CubeListBuilder.create().texOffs((int) horns[i][1], (int) horns[i][2])
					.addBox(x, -9.0F, -1.0F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, y(16.0F) - y(18.0F), -1.0F,
							(float) Math.toRadians(-45.0), 0.0F, 0.0F));
			head.addOrReplaceChild("horn_tip_" + i, CubeListBuilder.create().texOffs((int) horns[i][3], (int) horns[i][4])
					.addBox(x, y(24.36396F) - y(18.0F), 4.36396F, 1.0F, 2.0F, 1.0F), PartPose.ZERO);
		}
		// Rods: the top ring (Blockbench y 8-16) and the bottom ring (y 1-9), each turning around the middle.
		PartDefinition top = turn.addOrReplaceChild("top_rods", CubeListBuilder.create(), PartPose.ZERO);
		float[][] topRods = {{-1.0F, -7.0F, 0.0F}, {-7.0F, -1.0F, 16.0F}, {5.0F, -1.0F, 24.0F}, {-1.0F, 5.0F, 8.0F}};
		for (int i = 0; i < 4; i++) {
			top.addOrReplaceChild("rod" + i, CubeListBuilder.create().texOffs((int) topRods[i][2], 16)
					.addBox(topRods[i][0], y(16.0F), topRods[i][1], 2.0F, 8.0F, 2.0F), PartPose.ZERO);
		}
		PartDefinition bottom = turn.addOrReplaceChild("bottom_rods", CubeListBuilder.create(), PartPose.ZERO);
		float[][] bottomRods = {{2.0F, -4.0F, 0.0F}, {-4.0F, 2.0F, 8.0F}, {-4.0F, -4.0F, 16.0F}, {2.0F, 2.0F, 24.0F}};
		for (int i = 0; i < 4; i++) {
			bottom.addOrReplaceChild("rod" + i, CubeListBuilder.create().texOffs((int) bottomRods[i][2], 26)
					.addBox(bottomRods[i][0], y(9.0F), bottomRods[i][1], 2.0F, 8.0F, 2.0F), PartPose.ZERO);
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	public static void registerLayer() {
		ModelLayerRegistry.registerModelLayer(LAYER, ImpRenderer::createLayer);
	}

	/** 0 -> 1 -> 0 over `period` ticks, in straight lines (up for half, down for half). */
	private static float triangle(float age, float period) {
		float t = (age % period) / period;
		return t < 0.5F ? t * 2.0F : 2.0F - t * 2.0F;
	}

	public static class ImpModel extends EntityModel<LivingEntityRenderState> {
		private final ModelPart head;
		private final ModelPart top;
		private final ModelPart bottom;

		ImpModel(ModelPart root) {
			super(root);
			ModelPart turn = root.getChild("turn");
			this.head = turn.getChild("head");
			this.top = turn.getChild("top_rods");
			this.bottom = turn.getChild("bottom_rods");
		}

		@Override
		public void setupAnim(LivingEntityRenderState state) {
			super.setupAnim(state);
			float age = state.ageInTicks;
			top.yRot = age / TOP_TURN * (float) (Math.PI * 2.0);
			bottom.yRot = age / BOTTOM_TURN * (float) (Math.PI * 2.0);
			top.y = -1.0F * triangle(age, BOB);
			bottom.y = -2.0F * triangle(age, BOB);
			head.yRot = state.yRot * ((float) Math.PI / 180.0F);
			// Inside the half turn, looking up and down is mirrored.
			head.xRot = -state.xRot * ((float) Math.PI / 180.0F);
		}
	}

	public ImpRenderer(EntityRendererProvider.Context context) {
		super(context, new ImpModel(context.bakeLayer(LAYER)), 0.5F);
	}

	@Override
	public LivingEntityRenderState createRenderState() {
		return new LivingEntityRenderState();
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return TEXTURE;
	}

	@Override
	protected int getBlockLightLevel(Imp entity, BlockPos pos) {
		return 15;
	}
}
