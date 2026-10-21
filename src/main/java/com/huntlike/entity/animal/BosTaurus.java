package com.huntlike.entity.animal;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.animal.cow.Cow;

/**
 * Herd Call animal for Aurochs, based on the cow.
 * Template entity: it behaves like the vanilla cow for now. Give it its own model and texture in
 * its renderer (client side) when the custom art is ready.
 */
public class BosTaurus extends Cow {
	public BosTaurus(EntityType<? extends Cow> type, Level level) {
		super(type, level);
	}
}
