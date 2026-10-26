package com.huntlike.client.pet;

import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

/** A pet's render state: the slime's, plus whether it carries its chest (and the chest's item model). */
public class PetRenderState extends SlimeRenderState {
	public boolean chest;
	public final ItemStackRenderState chestItem = new ItemStackRenderState();
}
