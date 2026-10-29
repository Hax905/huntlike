package com.huntlike.towernaar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import com.huntlike.entity.ModEntities;
import com.huntlike.gear.ModShields;
import com.huntlike.hunt.HuntData;
import com.huntlike.hunt.HuntDefs;
import com.huntlike.hunt.Messages;
import com.huntlike.hunt.QuestManager;

/**
 * Towernaar's Final Chapter: start it from his menu, kill an enderman, and Towernaar the Undead appears. 10 minutes, and
 * running too far fails it (it can be tried again). Beaten, he stays at 1 health; 3 seconds later he thanks you, 3 seconds
 * after that he dies and drops the Magnetic Wand PRD. Won once, never again.
 */
public final class TowernaarFight {
	private TowernaarFight() {
	}

	public static final int TIME_LIMIT = 10 * 60 * 20;
	public static final double FLEE_DISTANCE = 64.0;
	public static final int PURPLE = 0xAA00AA;

	private static final class Fight {
		final UUID boss;
		final long spawnedAt;
		final ServerBossEvent bar;
		int endingTicks;

		Fight(UUID boss, long spawnedAt, ServerBossEvent bar) {
			this.boss = boss;
			this.spawnedAt = spawnedAt;
			this.bar = bar;
		}
	}

	/** Started from the menu, waiting for an enderman kill. */
	private static final Set<UUID> WAITING = new HashSet<>();

	/** The summoning before he appears: a five-pointed star of portal and red particles where he will stand. */
	public static final int RITUAL_TICKS = 40;

	private static final class Ritual {
		final ServerLevel level;
		final net.minecraft.world.phys.Vec3 at;
		int ticks;

		Ritual(ServerLevel level, net.minecraft.world.phys.Vec3 at) {
			this.level = level;
			this.at = at;
		}
	}

	private static final Map<UUID, Ritual> RITUALS = new HashMap<>();
	private static final Map<UUID, Fight> FIGHTS = new HashMap<>();

	/** 0 = nothing, 1 = waiting for an enderman kill, 2 = fighting (shown in his menu). */
	public static int phase(ServerPlayer player) {
		return FIGHTS.containsKey(player.getUUID()) || RITUALS.containsKey(player.getUUID()) ? 2
				: WAITING.contains(player.getUUID()) ? 1 : 0;
	}

	public static boolean canStart(HuntData data) {
		return data.towernaarDone == TowernaarScript.FINAL && TowernaarScript.canStart(TowernaarScript.FINAL, data);
	}

	public static void start(ServerPlayer player, HuntData data) {
		if (!canStart(data) || phase(player) != 0) {
			return;
		}
		if (data.quest != null) {
			player.sendSystemMessage(Messages.error("Finish your Hunting Quest first."));
			return;
		}
		WAITING.add(player.getUUID());
		player.sendSystemMessage(Messages.c("[Quest Started]", 0xFFFF55)
				.append(Messages.c(" - " + TowernaarScript.CHAPTERS.get(TowernaarScript.FINAL).title(), 0xFFFFFF)));
		player.sendSystemMessage(Messages.c("Slay an enderman to call Towernaar the Undead...", PURPLE));
		QuestManager.playSound(player, SoundEvents.ENDERMAN_STARE, 0.8F);
		QuestManager.sync(player);
	}

	/** An enderman slain by a waiting player calls the boss. */
	public static void onDeath(LivingEntity entity, DamageSource source) {
		if (entity.getType() != EntityTypes.ENDERMAN || !(source.getEntity() instanceof ServerPlayer player)
				|| !WAITING.remove(player.getUUID())) {
			return;
		}
		// The summoning: 2 seconds of the star, then he steps out of it.
		ServerLevel level = player.level();
		double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
		net.minecraft.world.phys.Vec3 at = new net.minecraft.world.phys.Vec3(player.getX() + Math.cos(angle) * 4.0, player.getY(),
				player.getZ() + Math.sin(angle) * 4.0);
		RITUALS.put(player.getUUID(), new Ritual(level, at));
		level.playSound(null, at.x, at.y, at.z, SoundEvents.PORTAL_TRIGGER, net.minecraft.sounds.SoundSource.HOSTILE, 0.6F, 0.7F);
		QuestManager.sync(player);
	}

	/** One frame of the summoning star: five points on a circle, each joined to the second one after it. */
	private static void drawStar(ServerLevel level, net.minecraft.world.phys.Vec3 at, int tick) {
		double radius = 2.2;
		double spin = tick * 0.03;
		net.minecraft.core.particles.DustParticleOptions red = new net.minecraft.core.particles.DustParticleOptions(0xD01010, 1.3F);
		double[][] points = new double[5][2];
		for (int i = 0; i < 5; i++) {
			double a = spin - Math.PI / 2 + Math.PI * 2.0 * i / 5;
			points[i][0] = at.x + Math.cos(a) * radius;
			points[i][1] = at.z + Math.sin(a) * radius;
		}
		for (int i = 0; i < 5; i++) {
			double[] from = points[i];
			double[] to = points[(i + 2) % 5];
			for (int k = 0; k <= 10; k++) {
				double x = from[0] + (to[0] - from[0]) * k / 10.0;
				double z = from[1] + (to[1] - from[1]) * k / 10.0;
				net.minecraft.core.particles.ParticleOptions particle = k % 2 == 0 ? red : ParticleTypes.PORTAL;
				level.sendParticles(particle, x, at.y + 0.1, z, 1, 0.0, 0.0, 0.0, 0.0);
			}
			// Each point: a column of portal particles rising as the summoning goes on.
			level.sendParticles(ParticleTypes.PORTAL, from[0], at.y + 0.2 + tick / 20.0, from[1], 3, 0.05, 0.3, 0.05, 0.3);
		}
	}

	public static TowernaarUndead spawn(ServerPlayer player) {
		double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
		return spawn(player, new net.minecraft.world.phys.Vec3(player.getX() + Math.cos(angle) * 4.0, player.getY(),
				player.getZ() + Math.sin(angle) * 4.0));
	}

	public static TowernaarUndead spawn(ServerPlayer player, net.minecraft.world.phys.Vec3 at) {
		ServerLevel level = player.level();
		RITUALS.remove(player.getUUID());
		TowernaarUndead boss = ModEntities.TOWERNAAR_UNDEAD.create(level, EntitySpawnReason.MOB_SUMMONED);
		boss.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
		boss.setHunted(player.getUUID());
		boss.setTarget(player);
		level.addFreshEntity(boss);
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, boss.getX(), boss.getY() + 1.4, boss.getZ(), 80, 0.5, 1.0,
				0.5, 0.4);
		ServerBossEvent bar = new ServerBossEvent(boss.getUUID(), Component.literal("Towernaar the Undead").withColor(PURPLE),
				BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
		bar.addPlayer(player);
		WAITING.remove(player.getUUID());
		FIGHTS.put(player.getUUID(), new Fight(boss.getUUID(), level.getGameTime(), bar));
		player.sendSystemMessage(Messages.c("Towernaar the Undead - The Dark Practicant has appeared!", PURPLE));
		QuestManager.playSound(player, SoundEvents.ENDERMAN_SCREAM, 0.6F);
		QuestManager.sync(player);
		return boss;
	}

	public static void tick(MinecraftServer server) {
		WAITING.removeIf(id -> server.getPlayerList().getPlayer(id) == null);
		for (Map.Entry<UUID, Ritual> entry : new ArrayList<>(RITUALS.entrySet())) {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			Ritual ritual = entry.getValue();
			if (player == null || !player.isAlive() || player.level() != ritual.level) {
				RITUALS.remove(entry.getKey());
				continue;
			}
			ritual.ticks++;
			if (ritual.ticks % 2 == 0) {
				drawStar(ritual.level, ritual.at, ritual.ticks);
			}
			if (ritual.ticks >= RITUAL_TICKS) {
				spawn(player, ritual.at);
			}
		}
		for (Map.Entry<UUID, Fight> entry : new ArrayList<>(FIGHTS.entrySet())) {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			Fight fight = entry.getValue();
			TowernaarUndead boss = find(server, fight.boss);
			if (player == null) {
				end(entry.getKey(), fight, boss);
				continue;
			}
			if (boss == null || !player.isAlive()) {
				fail(player, fight, boss);
				continue;
			}
			long elapsed = player.level().getGameTime() - fight.spawnedAt;
			if (boss.isEnding()) {
				fight.endingTicks++;
				pullPortal((ServerLevel) boss.level(), boss);
				fight.bar.setProgress(0.0F);
				if (fight.endingTicks == 60) {
					player.sendSystemMessage(Component.empty().append(Messages.c("Towernaar", PURPLE))
							.append(Messages.c(": Thank you " + player.getName().getString(), 0xFFFFFF)));
				} else if (fight.endingTicks >= 120) {
					win(player, fight, boss);
				}
				continue;
			}
			if (elapsed > TIME_LIMIT || boss.level() != player.level() || boss.distanceTo(player) > FLEE_DISTANCE) {
				fail(player, fight, boss);
				continue;
			}
			if (boss.getTarget() != player) {
				boss.setTarget(player);
			}
			int seconds = (int) Math.max(0, (TIME_LIMIT - elapsed) / 20);
			fight.bar.setProgress(boss.getHealth() / boss.getMaxHealth());
			fight.bar.setName(Component.literal("Towernaar the Undead").withColor(PURPLE)
					.append(Messages.c("  ❤ " + (int) Math.ceil(boss.getHealth()) + "/" + (int) boss.getMaxHealth(), HuntDefs.RED))
					.append(Messages.c(String.format("  %d:%02d", seconds / 60, seconds % 60), HuntDefs.GRAY)));
		}
	}

	/** His last moments: portal particles drawn into him from all around (a portal particle flies back to where it's sent from). */
	private static void pullPortal(ServerLevel level, TowernaarUndead boss) {
		net.minecraft.util.RandomSource random = boss.getRandom();
		for (int i = 0; i < 6; i++) {
			double yaw = random.nextDouble() * Math.PI * 2.0;
			double pitch = (random.nextDouble() - 0.5) * Math.PI;
			double distance = 2.5 + random.nextDouble() * 1.5;
			double dx = Math.cos(yaw) * Math.cos(pitch) * distance;
			double dy = Math.sin(pitch) * distance;
			double dz = Math.sin(yaw) * Math.cos(pitch) * distance;
			// Count 0: the offset is the particle's motion, so it starts out there and closes in on him.
			level.sendParticles(ParticleTypes.PORTAL, boss.getX(), boss.getY() + 1.4, boss.getZ(), 0, dx, dy, dz, 1.0);
		}
	}

	private static TowernaarUndead find(MinecraftServer server, UUID id) {
		for (ServerLevel level : server.getAllLevels()) {
			Entity entity = level.getEntity(id);
			if (entity instanceof TowernaarUndead boss) {
				return boss;
			}
		}
		return null;
	}

	/** A boss nobody is fighting (left over after a restart) leaves. */
	static void tickBoss(TowernaarUndead boss, ServerLevel level) {
		UUID player = boss.hunted();
		Fight fight = player == null ? null : FIGHTS.get(player);
		if (fight == null || !boss.getUUID().equals(fight.boss)) {
			boss.discard();
		}
	}

	private static void end(UUID player, Fight fight, TowernaarUndead boss) {
		FIGHTS.remove(player);
		fight.bar.removeAllPlayers();
		if (boss != null && boss.isAlive()) {
			boss.discard();
		}
	}

	private static void fail(ServerPlayer player, Fight fight, TowernaarUndead boss) {
		end(player.getUUID(), fight, boss);
		player.sendSystemMessage(Messages.c("[Quest Failed]", HuntDefs.RED)
				.append(Messages.c(" - " + TowernaarScript.CHAPTERS.get(TowernaarScript.FINAL).title(), 0xFFFFFF)));
		QuestManager.sync(player);
	}

	/** His last breath: the Magnetic Wand PRD drops and the questline is complete. */
	private static void win(ServerPlayer player, Fight fight, TowernaarUndead boss) {
		ServerLevel level = (ServerLevel) boss.level();
		// He goes out with a bang.
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, boss.getX(), boss.getY() + 1.4, boss.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
		level.playSound(null, boss.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.HOSTILE, 1.0F,
				0.9F);
		ItemEntity drop = new ItemEntity(level, boss.getX(), boss.getY() + 0.5, boss.getZ(), new ItemStack(ModShields.MAGNETIC_WAND_PRD));
		drop.setUnlimitedLifetime();
		level.addFreshEntity(drop);
		boss.allowDeath = true;
		boss.setNoAi(false);
		boss.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
		FIGHTS.remove(player.getUUID());
		fight.bar.removeAllPlayers();
		QuestManager.awardBossExperience(player, java.util.List.of());
		HuntData data = QuestManager.data(player);
		TowernaarManager.complete(player, data, TowernaarScript.CHAPTERS.get(TowernaarScript.FINAL));
	}

	public static void forget(ServerPlayer player) {
		WAITING.remove(player.getUUID());
		RITUALS.remove(player.getUUID());
	}

	public static void initialize() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(TowernaarFight::tick);
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register(TowernaarFight::onDeath);
	}
}
