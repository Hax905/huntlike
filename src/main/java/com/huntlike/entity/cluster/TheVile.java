package com.huntlike.entity.cluster;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.huntlike.hunt.ClusterFight;

/**
 * Tier V Cluster boss, The Vile, Soul Cluster of Conquest. Immune to fire and freezing; its core switches between frost and magma (Thermodynamic).
 * Template entity: it looks like the vanilla slime with your Soul Cluster textures for now. Its splitting (Mitosis) and abilities are run by
 * ClusterFight, never by vanilla splitting.
 */
public class TheVile extends Slime {
	public TheVile(EntityType<? extends Slime> type, Level level) {
		super(type, level);
	}

	@Override
	protected int getSplitCount() {
		return 0;
	}

	@Override
	protected void addTargetingGoals() {
	}

	@Override
	public void jumpFromGround() {
		super.jumpFromGround();
		ClusterFight.onJump(this);
	}

	@Override
	public void playerTouch(Player player) {
		if (ClusterFight.canTouch(this, player)) {
			super.playerTouch(player);
		}
	}

	@Override
	protected boolean canDealDamage() {
		return this.isEffectiveAi();
	}

	@Override
	protected float getAttackDamage() {
		return (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
	}

	private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> MAGMA_CORE =
			net.minecraft.network.syncher.SynchedEntityData.defineId(TheVile.class,
					net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(MAGMA_CORE, false);
	}

	/** True: orange magma core (weak to negative temperature). False: light-blue frost core. */
	public boolean isMagmaCore() {
		return this.entityData.get(MAGMA_CORE);
	}

	public void setMagmaCore(boolean magma) {
		this.entityData.set(MAGMA_CORE, magma);
	}

	@Override
	public boolean canFreeze() {
		return false;
	}
}
