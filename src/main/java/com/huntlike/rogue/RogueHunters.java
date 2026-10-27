package com.huntlike.rogue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;

import com.huntlike.entity.ModEntities;
import com.huntlike.hunt.HuntData;
import com.huntlike.hunt.HuntDefs;
import com.huntlike.hunt.Messages;
import com.huntlike.hunt.QuestManager;
import com.huntlike.hunt.SpiritLure;

/**
 * Rogue Hunter encounters. Once any Hunt reaches Rank V, each Spirit Lure kill during a quest's killing phase has a 1 in 30
 * chance (the 30th is certain) to call a Rogue Hunter: the lured mobs vanish, the lure and the quota pause, and a second
 * later the hunter appears. One in 10 (the 10th is certain) is a special one. It leaves after 5 minutes, or if the player
 * dies; killing it resumes the quest.
 */
public final class RogueHunters {
	private RogueHunters() {
	}

	public static final int RANK = 5;
	public static final int LURE_ODDS = 30;
	public static final int SPECIAL_ODDS = 10;
	public static final int SPAWN_DELAY = 20;
	public static final int LIFETIME = 5 * 60 * 20;
	private static final int DARK_RED = 0xAA0000;

	/** A running encounter: the hunter (null until it spawns), when it started, and when it spawns. */
	private static final class Encounter {
		UUID hunter;
		final boolean special;
		final long spawnAt;
		long spawnedAt;

		Encounter(boolean special, long spawnAt) {
			this.special = special;
			this.spawnAt = spawnAt;
		}
	}

	private static final Map<UUID, Encounter> ENCOUNTERS = new HashMap<>();

	/** Rogue Hunters come once any Hunt is at Rank V. */
	public static boolean eligible(HuntData data) {
		for (HuntDefs.Hunt hunt : HuntDefs.HUNTS) {
			if (data.progress(hunt.id()).rank() >= RANK) {
				return true;
			}
		}
		return false;
	}

	/** A Rogue Hunter is on its way or hunting this player: the Spirit Lure and the quota wait. */
	public static boolean active(ServerPlayer player) {
		return ENCOUNTERS.containsKey(player.getUUID());
	}

	/** One Spirit Lure kill: maybe a Rogue Hunter comes. */
	public static void onLuredKill(ServerPlayer player, HuntData data) {
		HuntData.Quest quest = data.quest;
		if (quest == null || quest.phase != HuntData.PHASE_KILLING || !eligible(data) || active(player)) {
			return;
		}
		data.rogueLures++;
		if (data.rogueLures >= LURE_ODDS || player.getRandom().nextInt(LURE_ODDS) == 0) {
			// Next tick, so the kill that called it still counts for the quota.
			PENDING.add(player.getUUID());
		}
	}

	private static final java.util.Set<UUID> PENDING = new java.util.HashSet<>();

	/** Starts an encounter now (also used by the test and commands). */
	public static void trigger(ServerPlayer player, HuntData data) {
		data.rogueLures = 0;
		data.rogueSinceSpecial++;
		boolean special = data.rogueSinceSpecial >= SPECIAL_ODDS || player.getRandom().nextInt(SPECIAL_ODDS) == 0;
		if (special) {
			data.rogueSinceSpecial = 0;
		}
		player.setAttached(QuestManager.DATA, data);
		SpiritLure.cleanup(player);
		player.sendSystemMessage(special ? gradient("A stronger presence than usual is felt... You are being hunted", DARK_RED, 0xFF5555)
				: Messages.c("A terrifying presence is stalking you from a distance... Get rid of it...", DARK_RED));
		player.level().playSound(null, player.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F,
				0.8F + player.getRandom().nextFloat() * 0.4F);
		ENCOUNTERS.put(player.getUUID(), new Encounter(special, player.level().getGameTime() + SPAWN_DELAY));
	}

	/** Text fading from one color to another, letter by letter. */
	public static MutableComponent gradient(String text, int from, int to) {
		MutableComponent out = Component.empty();
		int n = Math.max(1, text.length() - 1);
		for (int i = 0; i < text.length(); i++) {
			float t = i / (float) n;
			int r = Math.round(((from >> 16) & 0xFF) + t * (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)));
			int g = Math.round(((from >> 8) & 0xFF) + t * (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)));
			int b = Math.round((from & 0xFF) + t * ((to & 0xFF) - (from & 0xFF)));
			out.append(Component.literal(String.valueOf(text.charAt(i))).withColor((r << 16) | (g << 8) | b));
		}
		return out;
	}

	public static void tick(MinecraftServer server) {
		for (UUID id : new ArrayList<>(PENDING)) {
			PENDING.remove(id);
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player != null && player.isAlive() && !active(player)) {
				HuntData data = QuestManager.data(player);
				if (data.quest != null && data.quest.phase == HuntData.PHASE_KILLING) {
					trigger(player, data);
				}
			}
		}
		for (Map.Entry<UUID, Encounter> entry : new ArrayList<>(ENCOUNTERS.entrySet())) {
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			Encounter encounter = entry.getValue();
			RogueHunter hunter = encounter.hunter == null ? null : find(server, encounter.hunter);
			if (player == null || !player.isAlive()) {
				end(entry.getKey(), hunter);
				continue;
			}
			ServerLevel level = player.level();
			long now = level.getGameTime();
			if (encounter.hunter == null) {
				if (now >= encounter.spawnAt) {
					RogueHunter spawned = spawn(player, encounter.special);
					encounter.hunter = spawned.getUUID();
					encounter.spawnedAt = now;
				}
				continue;
			}
			if (hunter == null || hunter.level() != level || now - encounter.spawnedAt > LIFETIME) {
				if (hunter != null && now - encounter.spawnedAt > LIFETIME) {
					player.sendSystemMessage(Messages.c("The presence fades away...", 0x777777));
				}
				end(entry.getKey(), hunter);
				continue;
			}
			if (hunter.getTarget() != player) {
				hunter.setTarget(player);
			}
		}
	}

	private static void end(UUID player, RogueHunter hunter) {
		ENCOUNTERS.remove(player);
		if (hunter != null && hunter.isAlive()) {
			hunter.discard();
		}
	}

	private static RogueHunter find(MinecraftServer server, UUID id) {
		for (ServerLevel level : server.getAllLevels()) {
			Entity entity = level.getEntity(id);
			if (entity instanceof RogueHunter hunter) {
				return hunter;
			}
		}
		return null;
	}

	/** A hunter nobody is waiting for (left over after a restart) leaves. */
	static void tickHunter(RogueHunter hunter, ServerLevel level) {
		UUID player = hunter.hunted();
		Encounter encounter = player == null ? null : ENCOUNTERS.get(player);
		if (encounter == null || !hunter.getUUID().equals(encounter.hunter)) {
			hunter.discard();
		}
	}

	/** 10 to 14 blocks away, on the ground: stalking from a distance. */
	private static RogueHunter spawn(ServerPlayer player, boolean special) {
		ServerLevel level = player.level();
		RandomSource random = player.getRandom();
		BlockPos at = player.blockPosition();
		search:
		for (int attempt = 0; attempt < 24; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double distance = 10.0 + random.nextDouble() * 4.0;
			int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
			int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
			for (int dy = 4; dy >= -6; dy--) {
				BlockPos pos = new BlockPos(x, player.blockPosition().getY() + dy, z);
				if (level.getBlockState(pos.below()).isSolid() && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
					at = pos;
					break search;
				}
			}
		}
		RogueHunter hunter = ModEntities.ROGUE_HUNTER.create(level, EntitySpawnReason.MOB_SUMMONED);
		hunter.setHunted(player.getUUID());
		hunter.setSpecial(special);
		hunter.setInvisible(true);
		hunter.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
		hunter.setTarget(player);
		level.addFreshEntity(hunter);
		return hunter;
	}

	/** A Rogue Hunter died: count it for its player and let the quest go on. */
	public static void onDeath(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof RogueHunter hunter) || !(entity.level() instanceof ServerLevel level)) {
			return;
		}
		UUID id = hunter.hunted();
		ServerPlayer player = id == null ? null : level.getServer().getPlayerList().getPlayer(id);
		if (id != null) {
			ENCOUNTERS.remove(id);
		}
		if (player == null) {
			return;
		}
		HuntData data = QuestManager.data(player);
		data.rogueKills++;
		if (hunter.isSpecial()) {
			data.specialRogueKills++;
		}
		player.setAttached(QuestManager.DATA, data);
		com.huntlike.gear.ModShields.onRogueKill(player, data, source);
		QuestManager.sync(player);
	}

	public static void initialize() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(RogueHunters::tick);
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register(RogueHunters::onDeath);
	}
}
