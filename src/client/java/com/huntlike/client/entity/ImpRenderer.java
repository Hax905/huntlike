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
import com.huntlike.client.entity.horns.Horns;
import com.huntlike.path.Imp;

/**
 * The Imp: a very dark red demon with a blaze's head, six thick rods circling it in two rings (turning opposite ways)
 * and a big horn. Its own 64x32 sheet: head at 0,0 (8x8x8), rods at 0,16 (3x10x3). Always fully lit.
 */
public class ImpRenderer extends MobRenderer<Imp, LivingEntityRenderState, ImpRenderer.ImpModel> {
	private static final Identifier TEXTURE = HuntLike.id("textures/entity/path/imp.png");
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(HuntLike.id("imp"), "main");
	public static final int RODS = 6;

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
		for (int i = 0; i < RODS; i++) {
			root.addOrReplaceChild("rod" + i, CubeListBuilder.create().texOffs(0, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 10.0F, 3.0F), PartPose.ZERO);
		}
		return LayerDefinition.create(mesh, 64, 32);
	}

	public static void registerLayer() {
		ModelLayerRegistry.registerModelLayer(LAYER, ImpRenderer::createLayer);
	}

	public static class ImpModel extends EntityModel<LivingEntityRenderState> {
		private final ModelPart head;
		private final ModelPart[] rods = new ModelPart[RODS];

		ImpModel(ModelPart root) {
			super(root);
			this.head = root.getChild("head");
			for (int i = 0; i < RODS; i++) {
				rods[i] = root.getChild("rod" + i);
			}
		}

		@Override
		public void setupAnim(LivingEntityRenderState state) {
			super.setupAnim(state);
			float age = state.ageInTicks;
			// Upper ring (radius 8) turns one way, lower ring (radius 6) the other; three rods each, 120 degrees apart.
			for (int i = 0; i < RODS; i++) {
				boolean upper = i < 3;
				float angle = (upper ? age * -0.25F : age * 0.18F + 1.0F) + (float) (Math.PI * 2.0 / 3.0) * (i % 3);
				float radius = upper ? 8.0F : 6.0F;
				ModelPart rod = rods[i];
				rod.x = (float) Math.cos(angle) * radius;
				rod.y = (upper ? -1.0F : 6.0F) + (float) Math.cos((i * 2 + age) * 0.25F);
				rod.z = (float) Math.sin(angle) * radius;
			}
			head.yRot = state.yRot * ((float) Math.PI / 180.0F);
			head.xRot = state.xRot * ((float) Math.PI / 180.0F);
		}
	}

	public ImpRenderer(EntityRendererProvider.Context context) {
		super(context, new ImpModel(context.bakeLayer(LAYER)), 0.4F);
		this.addLayer(new Horns.Layer<>(this, Horns.Layer.bake(context, Horns.IMP), HuntLike.id("textures/entity/path/imp_horn.png")));
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
