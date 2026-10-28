package com.huntlike.entity.animal;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.Level;

/**
 * Tier V boss, based on the wolf. Once Apex Instinct kicks in it is enraged, and the client shows its enraged skin.
 */
public class EpicyonHaydeni extends Wolf {
	private static final EntityDataAccessor<Boolean> ENRAGED = SynchedEntityData.defineId(EpicyonHaydeni.class,
			EntityDataSerializers.BOOLEAN);

	public EpicyonHaydeni(EntityType<? extends Wolf> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ENRAGED, false);
	}

	public boolean isEnraged() {
		return this.entityData.get(ENRAGED);
	}

	public void setEnraged(boolean enraged) {
		this.entityData.set(ENRAGED, enraged);
	}
}
