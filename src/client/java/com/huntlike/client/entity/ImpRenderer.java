package com.huntlike.client.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.blaze.BlazeModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import com.huntlike.HuntLike;
import com.huntlike.client.entity.horns.Horns;
import com.huntlike.path.Imp;

/** The Imp: a small, very dark red blaze with only three rods circling it, and the Demonling's horn. Always fully lit. */
public class ImpRenderer extends MobRenderer<Imp, LivingEntityRenderState, BlazeModel> {
	private static final Identifier TEXTURE = HuntLike.id("textures/entity/path/imp.png");

	/** The blaze model with three rods, 120° apart, circling a little closer than a blaze's. */
	static class ImpModel extends BlazeModel {
		private final ModelPart[] rods = new ModelPart[12];

		ImpModel(ModelPart root) {
			super(root);
			for (int i = 0; i < 12; i++) {
				rods[i] = root.getChild("part" + i);
			}
		}

		@Override
		public void setupAnim(LivingEntityRenderState state) {
			super.setupAnim(state);
			float age = state.ageInTicks;
			float angle = age * (float) Math.PI * -0.1F;
			for (int i = 0; i < 12; i++) {
				ModelPart rod = rods[i];
				rod.visible = i < 3;
				if (i < 3) {
					rod.x = (float) Math.cos(angle) * 7.0F;
					rod.y = 1.0F + (float) Math.cos((i * 2 + age) * 0.25F);
					rod.z = (float) Math.sin(angle) * 7.0F;
					angle += (float) (Math.PI * 2.0 / 3.0);
				}
			}
		}
	}

	public ImpRenderer(EntityRendererProvider.Context context) {
		super(context, new ImpModel(context.bakeLayer(ModelLayers.BLAZE)), 0.3F);
		this.addLayer(new Horns.Layer<>(this, Horns.Layer.bake(context, Horns.DEMONLING),
				HuntLike.id("textures/entity/path/demonling_horn.png")));
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
