package com.huntlike.entity.animal;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.animal.cow.Cow;

/**
 * Tier IV boss, based on the cow.
 * Template entity: it behaves like the vanilla cow for now. Give it its own model and texture in
 * its renderer (client side) when the custom art is ready.
 */
public class Aurochs extends Cow {
	public Aurochs(EntityType<? extends Cow> type, Level level) {
		super(type, level);
	}
}
