package com.huntlike.hunt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MobSpawnSettings;

import com.huntlike.hunt.HuntData.Quest;
import com.huntlike.hunt.HuntDefs.Hunt;

/** Draws the Hunt's animal spirits to a questing player: new animals keep appearing nearby. */
public final class SpiritLure {
	private SpiritLure() {
	}

	public static final String LURED_TAG = "huntlike_lured";
	public static final int CAP = 6;
	public static final double CAP_RADIUS = 48.0;
	private static final int HINT_DELAY = 20 * 30;

	/** Lured spawns since the quota last moved; capped so animals don't pile up forever. */
	public static final int SPAWNS_PER_KILL = 3;
	private static final Map<UUID, Integer> SPAWNS_SINCE_KILL = new HashMap<>();
	private static final Map<UUID, Integer> TIMERS = new HashMap<>();
	private static final Map<UUID, String> HINT_PLACE = new HashMap<>();
	private static final Map<UUID, Integer> HINT_TICKS = new HashMap<>();
	private static final Map<UUID, Set<String>> HINTS_SHOWN = new HashMap<>();

	public static boolean isLured(Entity entity) {
		return entity.entityTags().contains(LURED_TAG);
	}

	public static UUID ownerOf(Entity entity) {
		return BossController.ownerOf(entity);
	}

	public static boolean isLuredBy(Entity entity, ServerPlayer player) {
		return isLured(entity) && player.getUUID().equals(ownerOf(entity));
	}

	public static void reset(ServerPlayer player) {
		TIMERS.put(player.getUUID(), 0);
		SPAWNS_SINCE_KILL.put(player.getUUID(), 0);
	}

	/** A kill counted toward the quota: the lure may spawn again. */
	public static void onQuotaKill(ServerPlayer player) {
		SPAWNS_SINCE_KILL.put(player.getUUID(), 0);
	}

	public static void forget(ServerPlayer player) {
		TIMERS.remove(player.getUUID());
		SPAWNS_SINCE_KILL.remove(player.getUUID());
		HINT_PLACE.remove(player.getUUID());
		HINT_TICKS.remove(player.getUUID());
	}

	/** Removes this player's lured animals. */
	public static void cleanup(ServerPlayer player) {
		ServerLevel level = player.level();
		for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(256.0), m -> isLuredBy(m, player))) {
			mob.discard();
		}
	}

	/** Quota animals the current biome can spawn, or an empty list outside the Overworld. */
	public static List<EntityType<?>> huntableHere(ServerPlayer player) {
		List<EntityType<?>> types = new ArrayList<>();
		for (Weighted<MobSpawnSettings.SpawnerData> entry : spawnList(player.level(), player.blockPosition())) {
			types.add(entry.value().type());
		}
		return types;
	}

	private static List<Weighted<MobSpawnSettings.SpawnerData>> spawnList(ServerLevel level, BlockPos pos) {
		List<Weighted<MobSpawnSettings.SpawnerData>> list = new ArrayList<>();
		if (level.dimension() != Level.OVERWORLD) {
			return list;
		}
		for (Weighted<MobSpawnSettings.SpawnerData> entry : level.getChunkSource().getGenerator()
				.getMobsAt(level, level.structureManager(), MobCategory.CREATURE, pos).unwrap()) {
			if (entry.value().type().builtInRegistryHolder().is(QuestManager.QUOTA_ANIMALS)) {
				list.add(entry);
			}
		}
		return list;
	}

	public static void tick(ServerPlayer player, HuntData data, Quest quest) {
		ServerLevel level = player.level();
		Hunt hunt = HuntDefs.hunt(quest.hunt);
		UUID id = player.getUUID();
		boolean overworld = level.dimension() == Level.OVERWORLD;
		List<Weighted<MobSpawnSettings.SpawnerData>> here = spawnList(level, player.blockPosition());

		// No-spirits hint: once per biome or dimension, after 30 seconds there.
		if (!overworld || here.isEmpty()) {
			String place = overworld ? "biome:" + level.getBiome(player.blockPosition()).getRegisteredName()
					: "dimension:" + level.dimension().identifier();
			if (!place.equals(HINT_PLACE.get(id))) {
				HINT_PLACE.put(id, place);
				HINT_TICKS.put(id, 0);
			}
			int ticks = HINT_TICKS.merge(id, 1, Integer::sum);
			Set<String> shown = HINTS_SHOWN.computeIfAbsent(id, k -> new HashSet<>());
			if (ticks >= HINT_DELAY && shown.add(place)) {
				player.sendSystemMessage(overworld ? Messages.noSpiritsBiome(hunt) : Messages.noSpiritsDimension(hunt));
				QuestManager.playSound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F);
			}
		} else {
			HINT_PLACE.remove(id);
		}

		int rank = data.progress(quest.hunt).rank();
		// 10 seconds, one second less at Ranks II, IV, VI, VIII and X.
		int interval = (10 - rank / 2) * 20;
		int timer = TIMERS.merge(id, 1, Integer::sum);
		if (timer < interval) {
			return;
		}
		TIMERS.put(id, 0);
		if (!overworld || SPAWNS_SINCE_KILL.getOrDefault(id, 0) >= SPAWNS_PER_KILL) {
			return;
		}
		int nearby = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(CAP_RADIUS), m -> isLuredBy(m, player)).size();
		if (nearby >= CAP) {
			return;
		}
		RandomSource random = player.getRandom();
		for (int attempt = 0; attempt < 16; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double distance = 16.0 + random.nextDouble() * 16.0;
			int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
			int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
			BlockPos column = new BlockPos(x, player.blockPosition().getY(), z);
			if (!level.isLoaded(column)) {
				continue;
			}
			List<Weighted<MobSpawnSettings.SpawnerData>> options = spawnList(level, column);
			if (options.isEmpty()) {
				continue;
			}
			EntityType<?> type = pick(options, random);
			BlockPos pos = level.getHeightmapPos(SpawnPlacements.getHeightmapType(type), column);
			if (!SpawnPlacements.isSpawnPositionOk(type, level, pos)
					|| !SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, pos, random)
					|| !level.noCollision(type.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
				continue;
			}
			Entity entity = type.spawn(level, pos, EntitySpawnReason.NATURAL);
			if (entity == null) {
				continue;
			}
			if (entity instanceof Mob mob) {
				mob.setBaby(false);
			}
			entity.addTag(LURED_TAG);
			if (entity instanceof Mob glowing) {
				glowing.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 5, 0, false, false));
			}
			SPAWNS_SINCE_KILL.merge(id, 1, Integer::sum);
			entity.addTag(BossController.OWNER_PREFIX + id);
			level.sendParticles(new DustParticleOptions(hunt.color(), 1.5F), entity.getX(), entity.getY() + 0.5, entity.getZ(),
					20, 0.4, 0.5, 0.4, 0.0);
			player.sendSystemMessage(Messages.spiritLure(hunt));
			QuestManager.playSound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.8F);
			return;
		}
	}

	private static EntityType<?> pick(List<Weighted<MobSpawnSettings.SpawnerData>> options, RandomSource random) {
		int total = 0;
		for (Weighted<MobSpawnSettings.SpawnerData> entry : options) {
			total += Math.max(1, entry.weight());
		}
		int roll = random.nextInt(total);
		for (Weighted<MobSpawnSettings.SpawnerData> entry : options) {
			roll -= Math.max(1, entry.weight());
			if (roll < 0) {
				return entry.value().type();
			}
		}
		return options.get(0).value().type();
	}
}
