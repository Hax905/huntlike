package com.huntlike.interlude;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.level.Level;

/** Bartholomew, the slime prince who rides Pom. Never splits like a vanilla slime; the fight decides what happens. */
public class Bartholomew extends Slime {
	public Bartholomew(EntityType<? extends Slime> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
	}

	@Override
	public void remove(RemovalReason reason) {
		// A vanilla slime splits when it dies; Bartholomew's splitting is the fight's "Are we for real?".
		if (this.isDeadOrDying() && this.getSize() > 1) {
			this.setSize(1, false);
		}
		super.remove(reason);
	}

	/** Contact damage comes from the fight (its melee), not from the vanilla slime touch. */
	@Override
	public void playerTouch(net.minecraft.world.entity.player.Player player) {
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public void checkDespawn() {
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel level) {
			SlimeRiderFight.tickBody(this, level);
		}
	}
}
