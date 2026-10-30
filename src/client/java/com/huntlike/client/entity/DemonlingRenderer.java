package com.huntlike.client.entity;

import net.minecraft.client.renderer.entity.BlazeRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import com.huntlike.HuntLike;
import com.huntlike.client.entity.horns.Horns;

/** The Demonling: the blaze model with a small horn from its forehead. */
public class DemonlingRenderer extends BlazeRenderer {
	public DemonlingRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.addLayer(new Horns.Layer<>(this, Horns.Layer.bake(context, Horns.DEMONLING),
				HuntLike.id("textures/entity/path/demonling_horn.png")));
	}
}
