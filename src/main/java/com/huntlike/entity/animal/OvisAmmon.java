package com.huntlike.entity.animal;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.animal.sheep.Sheep;

/**
 * Tier III boss, based on the sheep.
 * Template entity: it behaves like the vanilla sheep for now. Give it its own model and texture in
 * its renderer (client side) when the custom art is ready.
 */
public class OvisAmmon extends Sheep {
	public OvisAmmon(EntityType<? extends Sheep> type, Level level) {
		super(type, level);
	}
}
