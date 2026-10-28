package com.huntlike.interlude;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Pom, the Slime Rider's mount: a horse with undead magic (a zombie horse for now). Can't be tamed or ridden. In Phase 1
 * it carries the pair's health; when that runs out it doesn't die, the fight moves on to Phase 2.
 */
public class Pom extends ZombieHorse {
	/** Only true for a real death (Phase 2, or the fight cleaning up). */
	boolean allowDeath;

	public Pom(EntityType<? extends ZombieHorse> type, Level level) {
		super(type, level);
		this.setPersistenceRequired();
	}

	@Override
	public boolean isMobControlled() {
		return false;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		return InteractionResult.PASS;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public void checkDespawn() {
	}

	@Override
	public void die(DamageSource source) {
		// Phase 1 ending: the fight sets up Phase 2 (Pom keeps half the health, or leaves) instead of a death.
		if (!allowDeath && this.level() instanceof ServerLevel && SlimeRiderFight.onPomDefeated(this)) {
			return;
		}
		super.die(source);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel level) {
			SlimeRiderFight.tickBody(this, level);
		}
	}
}
