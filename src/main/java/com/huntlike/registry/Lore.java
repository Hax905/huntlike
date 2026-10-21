package com.huntlike.registry;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import com.huntlike.HuntLike;

/** Builds item tooltips: wrapped lines, blank lines between sections. */
public final class Lore {
	private Lore() {
	}

	/** Longest tooltip line, in characters. */
	public static final int MAX_LINE = 38;
	public static final int ABILITY_COLOR = 0xFFAA00;

	private static Map<String, String> english;

	/** English text of a lang key, read from the mod's en_us.json. */
	public static String english(String key) {
		if (english == null) {
			try (var stream = HuntLike.class.getResourceAsStream("/assets/huntlike/lang/en_us.json")) {
				english = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8),
						new TypeToken<Map<String, String>>() { }.getType());
			} catch (Exception e) {
				HuntLike.LOGGER.error("Could not read HuntLike lang file", e);
				english = Map.of();
			}
		}
		return english.getOrDefault(key, key);
	}

	/** Splits text into lines of at most MAX_LINE characters, breaking between words. */
	public static List<String> wrap(String text) {
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			int visible = line.toString().replaceAll("\u00a7.", "").length() + word.replaceAll("\u00a7.", "").length();
			if (line.length() > 0 && visible + 1 > MAX_LINE) {
				lines.add(line.toString());
				line = new StringBuilder();
			}
			if (line.length() > 0) {
				line.append(' ');
			}
			line.append(word);
		}
		if (line.length() > 0) {
			lines.add(line.toString());
		}
		return lines;
	}

	public static void gray(List<Component> lore, String text) {
		for (String line : wrap(text)) {
			lore.add(Component.literal(line).withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false)));
		}
	}

	/** An ability: a blank line, its name in gold, then its wrapped description. */
	public static void ability(List<Component> lore, String name, String description) {
		if (!lore.isEmpty()) {
			lore.add(Component.empty());
		}
		lore.add(Component.literal(name).withStyle(style -> style.withColor(ABILITY_COLOR).withItalic(false)));
		gray(lore, description);
	}
}
