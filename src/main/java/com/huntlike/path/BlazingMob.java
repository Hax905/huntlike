package com.huntlike.path;

import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/** Shared bits of the Blazing mobs: they hunt the player who fed them the heart (kept as an entity tag). */
public final class BlazingMob {
	public static final String FEEDER_PREFIX = "huntlike_feeder:";

	private BlazingMob() {
	}

	public static void setFeeder(Entity entity, UUID player) {
		for (String tag : java.util.List.copyOf(entity.entityTags())) {
			if (tag.startsWith(FEEDER_PREFIX)) {
				entity.removeTag(tag);
			}
		}
		entity.addTag(FEEDER_PREFIX + player);
	}

	public static UUID feeder(Entity entity) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(FEEDER_PREFIX)) {
				try {
					return UUID.fromString(tag.substring(FEEDER_PREFIX.length()));
				} catch (IllegalArgumentException e) {
					return null;
				}
			}
		}
		return null;
	}

	/** Keeps the mob on its feeder (within 48 blocks, alive, not in creative or spectator). */
	public static void huntFeeder(Mob mob, ServerLevel level) {
		UUID id = feeder(mob);
		if (id == null) {
			return;
		}
		Player player = level.getPlayerByUUID(id);
		boolean valid = player != null && player.isAlive() && !player.isCreative() && !player.isSpectator()
				&& player.distanceToSqr(mob) < 48 * 48;
		if (valid && mob.getTarget() != player) {
			mob.setTarget(player);
		} else if (!valid && mob.getTarget() != null) {
			mob.setTarget(null);
		}
	}
}
