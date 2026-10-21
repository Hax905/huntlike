package com.huntlike.entity.animal;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.animal.chicken.Chicken;

/**
 * Tier I boss, based on the chicken.
 * Template entity: it behaves like the vanilla chicken for now. Give it its own model and texture in
 * its renderer (client side) when the custom art is ready.
 */
public class Sylviornis extends Chicken {
	public Sylviornis(EntityType<? extends Chicken> type, Level level) {
		super(type, level);
	}
}
