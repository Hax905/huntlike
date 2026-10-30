package com.huntlike.nexus;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import com.huntlike.HuntLike;
import com.huntlike.hunt.HuntData;
import com.huntlike.hunt.HuntDefs;
import com.huntlike.hunt.QuestManager;
import com.huntlike.interlude.InterludeDefs;

/**
 * The Soul Nexus: 1 point for every boss tier (and Interlude stage) beaten for the first time, spent on three corners
 * of the soul, each capped at 10. Health: 2 points = +1 max health. Defense: 1 point = 0.25% less damage taken (Soul
 * Strike still lands in full). Damage: 1 point = +0.5% damage dealt. Points move freely between corners.
 */
public final class SoulNexus {
	public static final int CAP = 10;
	public static final String HEALTH = "health";
	public static final String DEFENSE = "defense";
	public static final String DAMAGE = "damage";
	/** The Hunt menu tab value that opens the Soul Nexus screen instead. */
	public static final int SCREEN_TAB = 10;
	private static final Identifier HEALTH_MODIFIER = HuntLike.id("soul_nexus_health");

	private SoulNexus() {
	}

	/** Points earned: every boss tier of every Hunt, and every Interlude stage, beaten at least once. */
	public static int earned(HuntData data) {
		int points = 0;
		for (HuntDefs.Hunt hunt : HuntDefs.HUNTS) {
			for (int kills : data.progress(hunt.id()).bossKills) {
				points += kills > 0 ? 1 : 0;
			}
		}
		int[] interlude = data.progress(InterludeDefs.SLIME_RIDER).bossKills;
		for (int i = 0; i < InterludeDefs.STAGES.size(); i++) {
			points += interlude[i] > 0 ? 1 : 0;
		}
		return points;
	}

	public static int spent(HuntData data) {
		return data.nexusHealth + data.nexusDefense + data.nexusDamage;
	}

	public static int free(HuntData data) {
		return Math.max(0, earned(data) - spent(data));
	}

	public static int get(HuntData data, String corner) {
		return switch (corner) {
			case HEALTH -> data.nexusHealth;
			case DEFENSE -> data.nexusDefense;
			case DAMAGE -> data.nexusDamage;
			default -> 0;
		};
	}

	private static void set(HuntData data, String corner, int value) {
		switch (corner) {
			case HEALTH -> data.nexusHealth = value;
			case DEFENSE -> data.nexusDefense = value;
			case DAMAGE -> data.nexusDamage = value;
			default -> {
			}
		}
	}

	/** Adds (delta > 0) or takes back (delta < 0) points on a corner, as far as the free points and the cap allow. */
	public static void change(ServerPlayer player, HuntData data, String corner, int delta) {
		int now = get(data, corner);
		int target = delta > 0 ? Math.min(CAP, now + Math.min(delta, free(data))) : Math.max(0, now + delta);
		if (target == now) {
			QuestManager.playSound(player, SoundEvents.VILLAGER_NO, 1.0F);
			return;
		}
		set(data, corner, target);
		player.setAttached(QuestManager.DATA, data);
		apply(player, data);
		player.level().playSound(null, player.blockPosition(), target > now ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.SOUL_ESCAPE.value(),
				SoundSource.PLAYERS, 1.0F, target > now ? 1.2F : 0.8F);
	}

	/** Max health from the Health corner: +1 per 2 points. */
	public static int bonusHealth(HuntData data) {
		return data.nexusHealth / 2;
	}

	/** Damage-taken multiplier from the Defense corner: 0.25% less per point. */
	public static float defenseFactor(HuntData data) {
		return 1.0F - 0.0025F * data.nexusDefense;
	}

	/** Damage-dealt multiplier from the Damage corner: 0.5% more per point. */
	public static float damageFactor(HuntData data) {
		return 1.0F + 0.005F * data.nexusDamage;
	}

	/** Keeps the Health corner's max health modifier on the player. */
	public static void apply(ServerPlayer player, HuntData data) {
		AttributeInstance max = player.getAttribute(Attributes.MAX_HEALTH);
		if (max == null) {
			return;
		}
		int bonus = bonusHealth(data);
		AttributeModifier current = max.getModifier(HEALTH_MODIFIER);
		if (bonus == 0) {
			if (current != null) {
				max.removeModifier(HEALTH_MODIFIER);
				player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
			}
			return;
		}
		if (current == null || current.amount() != bonus) {
			max.addOrUpdateTransientModifier(new AttributeModifier(HEALTH_MODIFIER, bonus, AttributeModifier.Operation.ADD_VALUE));
			player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
		}
	}

	/** The damage a player takes or deals, with the Defense and Damage corners. */
	public static float adjust(net.minecraft.world.entity.LivingEntity target, net.minecraft.world.damagesource.DamageSource source,
			float amount) {
		if (source.getEntity() instanceof ServerPlayer attacker && attacker != target) {
			amount *= damageFactor(QuestManager.data(attacker));
		}
		if (target instanceof ServerPlayer player && !source.is(com.huntlike.hunt.BossAbilities.SOUL_STRIKE)) {
			amount *= defenseFactor(QuestManager.data(player));
		}
		return amount;
	}

	public static void initialize() {
		// Re-applied every second: covers joining, respawning and points earned elsewhere.
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register((MinecraftServer server) -> {
			if (server.getTickCount() % 20 != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				apply(player, QuestManager.data(player));
			}
		});
	}

	public static boolean isCorner(String corner) {
		return HEALTH.equals(corner) || DEFENSE.equals(corner) || DAMAGE.equals(corner);
	}
}
