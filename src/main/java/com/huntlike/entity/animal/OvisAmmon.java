package com.huntlike.entity.animal;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/** Tier III boss, based on the sheep. Its wool comes in a random dye color every time it spawns. */
public class OvisAmmon extends Sheep {
	public OvisAmmon(EntityType<? extends Sheep> type, Level level) {
		super(type, level);
		if (!level.isClientSide()) {
			this.randomizeColor();
		}
	}

	private void randomizeColor() {
		DyeColor[] colors = DyeColor.values();
		this.setColor(colors[this.getRandom().nextInt(colors.length)]);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			SpawnGroupData data) {
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
		this.randomizeColor();
		return result;
	}
}
