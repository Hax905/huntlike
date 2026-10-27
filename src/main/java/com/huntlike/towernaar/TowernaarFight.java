package com.huntlike.towernaar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
	private static final Map<UUID, Fight> FIGHTS = new HashMap<>();

	/** 0 = nothing, 1 = waiting for an enderman kill, 2 = fighting (shown in his menu). */
	public static int phase(ServerPlayer player) {
		return FIGHTS.containsKey(player.getUUID()) ? 2 : WAITING.contains(player.getUUID()) ? 1 : 0;
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
		spawn(player);
	}

	public static TowernaarUndead spawn(ServerPlayer player) {
		ServerLevel level = player.level();
		TowernaarUndead boss = ModEntities.TOWERNAAR_UNDEAD.create(level, EntitySpawnReason.MOB_SUMMONED);
		double angle = player.getRandom().nextDouble() * Math.PI * 2.0;
		boss.snapTo(player.getX() + Math.cos(angle) * 4.0, player.getY(), player.getZ() + Math.sin(angle) * 4.0, 0.0F, 0.0F);
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
		ItemEntity drop = new ItemEntity(level, boss.getX(), boss.getY() + 0.5, boss.getZ(), new ItemStack(ModShields.MAGNETIC_WAND_PRD));
		drop.setUnlimitedLifetime();
		level.addFreshEntity(drop);
		boss.allowDeath = true;
		boss.setNoAi(false);
		boss.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
		FIGHTS.remove(player.getUUID());
		fight.bar.removeAllPlayers();
		HuntData data = QuestManager.data(player);
		TowernaarManager.complete(player, data, TowernaarScript.CHAPTERS.get(TowernaarScript.FINAL));
	}

	public static void forget(ServerPlayer player) {
		WAITING.remove(player.getUUID());
	}

	public static void initialize() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(TowernaarFight::tick);
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register(TowernaarFight::onDeath);
	}
}
