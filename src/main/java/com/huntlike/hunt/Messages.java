package com.huntlike.hunt;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import com.huntlike.hunt.HuntDefs.Hunt;

/** Every chat line HuntLike sends, with the colors from the design. */
public final class Messages {
	private Messages() {
	}

	public static MutableComponent c(String text, int rgb) {
		return Component.literal(text).withColor(rgb);
	}

	public static MutableComponent tierTag(int tier) {
		return c("[Tier " + HuntDefs.ROMAN[tier] + "]", HuntDefs.tierColor(tier));
	}

	private static MutableComponent questLine(String event, int eventColor, Hunt hunt, int tier) {
		return c("Hunting Quest ", HuntDefs.PREFIX)
				.append(c(event, eventColor))
				.append(c(" - ", HuntDefs.PREFIX))
				.append(c(hunt.name(), hunt.color()))
				.append(Component.literal(" "))
				.append(tierTag(tier));
	}

	public static Component started(Hunt hunt, int tier) {
		return questLine("Started", HuntDefs.PREFIX, hunt, tier);
	}

	public static Component bossSpawned(Hunt hunt, int tier) {
		return questLine("Boss Spawned", HuntDefs.AQUA, hunt, tier);
	}

	public static Component complete(Hunt hunt, int tier) {
		return questLine("Complete", HuntDefs.DARK_GREEN, hunt, tier);
	}

	public static Component failed(Hunt hunt, int tier) {
		return questLine("Failed", HuntDefs.DARK_RED, hunt, tier);
	}

	public static Component spiritLure(Hunt hunt) {
		return c("SPIRIT LURE! ", HuntDefs.AQUA)
				.append(c("Your quest is attracting ", HuntDefs.LIGHT_BLUE))
				.append(c(hunt.name(), hunt.color()))
				.append(c(" spirits", HuntDefs.PINK))
				.append(c(" to you...", HuntDefs.LIGHT_BLUE));
	}

	public static Component noSpiritsBiome(Hunt hunt) {
		return c("No " + hunt.name() + " spirits answer your call in this biome...", HuntDefs.GRAY);
	}

	public static Component noSpiritsDimension(Hunt hunt) {
		return c("No " + hunt.name() + " spirits answer your call in this dimension...", HuntDefs.DARK_PURPLE);
	}

	public static Component badTerrain() {
		return c("The Hunt Boss cannot be called upon from this terrain.", HuntDefs.RED);
	}

	public static Component peacefulStart() {
		return c("Cannot start quest because of the world's difficulty.", HuntDefs.RED);
	}

	public static Component quota(Hunt hunt, int kills, int quota) {
		return c(hunt.name() + " Quota ", hunt.color()).append(c(kills + "/" + quota, 0xFFFFFF));
	}

	public static Component rankUp(Hunt hunt, int level) {
		return c("RANK UP! ", HuntDefs.GOLD)
				.append(c(hunt.rankName(level), hunt.color()))
				.append(c(" [" + HuntDefs.ROMAN[level] + "]", HuntDefs.GOLD));
	}

	public static Component rareDrop(Component itemName) {
		return c("RARE DROP! ", HuntDefs.GOLD).append(itemName);
	}

	public static Component error(String text) {
		return c(text, HuntDefs.RED);
	}
}
