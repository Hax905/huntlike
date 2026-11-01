package com.huntlike.path;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.level.Level;

/**
 * An Evilborn: a villager turned by the Blood Crystallization into one of ashes, fire and evil. A vindicator with horns
 * that never burns; slaying 10 is the Path of the Wildfire's tenth goal.
 */
public class Evilborn extends Vindicator {
	public static final float HEALTH = 30.0F;

	public Evilborn(EntityType<? extends Vindicator> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
		this.xpReward = 10;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Vindicator.createAttributes().add(Attributes.MAX_HEALTH, HEALTH);
	}
}
