package com.huntlike.entity.animal;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.animal.wolf.Wolf;

/**
 * Herd Call animal for Epicyon Haydeni, based on the wolf.
 * Template entity: it behaves like the vanilla wolf for now. Give it its own model and texture in
 * its renderer (client side) when the custom art is ready.
 */
public class CanisLupus extends Wolf {
	public CanisLupus(EntityType<? extends Wolf> type, Level level) {
		super(type, level);
	}
}
