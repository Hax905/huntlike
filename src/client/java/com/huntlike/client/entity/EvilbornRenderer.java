package com.huntlike.client.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VindicatorRenderer;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.resources.Identifier;

import com.huntlike.HuntLike;
import com.huntlike.client.entity.horns.Horns;

/**
 * The Evilborn: a villager turned by the Blood Crystallization. The vindicator's model with a pair of horns; uses
 * textures/entity/path/evilborn.png when the mod has one (64x64, the vindicator's layout), otherwise the vindicator's skin.
 */
public class EvilbornRenderer extends VindicatorRenderer {
	private static final Identifier TEXTURE = HuntLike.id("textures/entity/path/evilborn.png");

	public EvilbornRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.addLayer(new Horns.Layer<>(this, Horns.Layer.bake(context, Horns.EVILBORN), HuntLike.id("textures/entity/path/evilborn_horn.png")));
	}

	@Override
	public Identifier getTextureLocation(IllagerRenderState state) {
		return Minecraft.getInstance().getResourceManager().getResource(TEXTURE).isPresent() ? TEXTURE : super.getTextureLocation(state);
	}
}
