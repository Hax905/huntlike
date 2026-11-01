package com.huntlike.client.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

import com.huntlike.HuntLike;
import com.huntlike.client.entity.horns.Horns;

/**
 * The Demonling: the blaze model with a small horn from its forehead. Uses textures/entity/path/demonling.png when the
 * mod has one (64x32, the blaze's layout), otherwise the blaze's own skin.
 */
public class DemonlingRenderer extends BlazeRenderer {
	private static final Identifier TEXTURE = HuntLike.id("textures/entity/path/demonling.png");

	public DemonlingRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.addLayer(new Horns.Layer<>(this, Horns.Layer.bake(context, Horns.DEMONLING),
				HuntLike.id("textures/entity/path/demonling_horn.png")));
	}

	@Override
	public Identifier getTextureLocation(LivingEntityRenderState state) {
		return Minecraft.getInstance().getResourceManager().getResource(TEXTURE).isPresent() ? TEXTURE : super.getTextureLocation(state);
	}
}
