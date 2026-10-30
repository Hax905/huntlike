package com.huntlike.path;

import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/** Shared bits of the Blazing mobs: they hunt the player who fed them the heart (kept as an entity tag). */
public final class BlazingMob {
	public static final String FEEDER_PREFIX = "huntlike_feeder:";
	public static final String BORN_PREFIX = "huntlike_born:";
	/** The Blazing Piglin and Wither Skeleton burn out after 3 minutes. */
	public static final long LIFETIME = 3 * 60 * 20;

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

	public static void setBorn(Entity entity, long gameTime) {
		for (String tag : java.util.List.copyOf(entity.entityTags())) {
			if (tag.startsWith(BORN_PREFIX)) {
				entity.removeTag(tag);
			}
		}
		entity.addTag(BORN_PREFIX + gameTime);
	}

	public static long born(Entity entity) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(BORN_PREFIX)) {
				try {
					return Long.parseLong(tag.substring(BORN_PREFIX.length()));
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		}
		return -1;
	}

	/** After 3 minutes: ash, a message for nearby players (and the feeder), and gone (no drops). True if it burned out. */
	public static boolean burnOut(Mob mob, ServerLevel level, String name) {
		long born = born(mob);
		if (born < 0) {
			setBorn(mob, level.getGameTime());
			return false;
		}
		if (level.getGameTime() - born < LIFETIME) {
			return false;
		}
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.ASH, mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(), 80,
				mob.getBbWidth() / 2, mob.getBbHeight() / 2, mob.getBbWidth() / 2, 0.02);
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.WHITE_ASH, mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(),
				60, mob.getBbWidth() / 2, mob.getBbHeight() / 2, mob.getBbWidth() / 2, 0.02);
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, mob.getX(), mob.getY() + 0.5, mob.getZ(), 15, 0.3, 0.5,
				0.3, 0.01);
		level.playSound(null, mob.blockPosition(), net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.HOSTILE,
				1.0F, 0.7F);
		net.minecraft.network.chat.Component message = com.huntlike.hunt.Messages.c("The " + name + " has turned into ashes...", 0xAAAAAA);
		UUID feeder = feeder(mob);
		for (net.minecraft.server.level.ServerPlayer player : level.players()) {
			if (player.distanceToSqr(mob) < 48 * 48 || player.getUUID().equals(feeder)) {
				player.sendSystemMessage(message);
			}
		}
		mob.discard();
		return true;
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
