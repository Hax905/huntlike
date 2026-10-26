package com.huntlike.client.pet;

import net.minecraft.client.renderer.entity.state.SlimeRenderState;

/** A pet's render state: the slime's, plus whether it carries its chest. */
public class PetRenderState extends SlimeRenderState {
	public boolean chest;
}
