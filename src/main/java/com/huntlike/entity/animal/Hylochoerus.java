package com.huntlike.entity.animal;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.animal.pig.Pig;

/**
 * Tier II boss, based on the pig.
 * Template entity: it behaves like the vanilla pig for now. Give it its own model and texture in
 * its renderer (client side) when the custom art is ready.
 */
public class Hylochoerus extends Pig {
	public Hylochoerus(EntityType<? extends Pig> type, Level level) {
		super(type, level);
	}
}
