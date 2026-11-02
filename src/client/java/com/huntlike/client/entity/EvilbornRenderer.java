package com.huntlike.client.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import com.huntlike.HuntLike;
import com.huntlike.path.Evilborn;

/**
 * The Evilborn: for now a plain villager with glowing red eyes (and a dark red outline, see the mixins). It still fights
 * like a vindicator.
 */
public class EvilbornRenderer extends MobRenderer<Evilborn, VillagerRenderState, VillagerModel> {
	private static final Identifier VILLAGER = Identifier.withDefaultNamespace("textures/entity/villager/villager.png");
	private static final Identifier EYES = HuntLike.id("textures/entity/path/evilborn_eyes.png");

	public EvilbornRenderer(EntityRendererProvider.Context context) {
		super(context, new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return RenderTypes.eyes(EYES);
			}
		});
	}

	@Override
	public VillagerRenderState createRenderState() {
		return new VillagerRenderState();
	}

	@Override
	public Identifier getTextureLocation(VillagerRenderState state) {
		return VILLAGER;
	}
}
