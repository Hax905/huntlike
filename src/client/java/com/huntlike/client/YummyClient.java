package com.huntlike.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;

import com.huntlike.HuntLike;
import com.huntlike.hunt.HuntDefs;
import com.huntlike.network.HuntPayloads;

/**
 * Yummy!: while the player is consumed, an escape bar and the key sequence (shown with the player's own key
 * bindings) appear on screen, and the keys pressed are sent to the server. Wrong keys do nothing.
 */
public final class YummyClient {
	private YummyClient() {
	}

	public static boolean active;
	public static int[] keys = new int[0];
	public static int progress;

	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(HuntPayloads.Yummy.TYPE, (payload, context) -> {
			active = payload.active();
			String digits = payload.keys();
			keys = new int[digits.length()];
			for (int i = 0; i < digits.length(); i++) {
				keys[i] = digits.charAt(i) - '0';
			}
			progress = payload.progress();
		});
		ClientTickEvents.START_CLIENT_TICK.register(YummyClient::tick);
		HudElementRegistry.addLast(HuntLike.id("yummy"), (g, delta) -> draw(g));
	}

	private static KeyMapping[] mappings(Minecraft client) {
		var o = client.options;
		return new KeyMapping[] {o.keyUp, o.keyLeft, o.keyDown, o.keyRight, o.keyAttack, o.keyUse, o.keyJump};
	}

	/** Runs before vanilla reads the clicks, so attacking and using do nothing while consumed. */
	private static void tick(Minecraft client) {
		if (!active || client.player == null) {
			return;
		}
		KeyMapping[] mappings = mappings(client);
		for (int key = 0; key < mappings.length; key++) {
			while (mappings[key].consumeClick()) {
				if (progress < keys.length && keys[progress] == key) {
					progress++;
					ClientPlayNetworking.send(new HuntPayloads.Action("yummy_key", "cluster", key));
				}
			}
		}
	}

	/** Short label of a key from the player's bindings ("W", "Space", "LMB"...). */
	public static String label(Minecraft client, int key) {
		String text = mappings(client)[key].getTranslatedKeyMessage().getString();
		return switch (text) {
			case "Left Button" -> "LMB";
			case "Right Button" -> "RMB";
			case "Middle Button" -> "MMB";
			default -> text.length() > 5 ? text.substring(0, 5) : text;
		};
	}

	private static void draw(GuiGraphicsExtractor g) {
		if (!active || keys.length == 0) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		Font font = client.font;
		int gap = 3;
		int boxH = 16;
		int maxRowW = Math.min(g.guiWidth() - 24, 300);
		// Lay the keys out left to right, wrapping into rows.
		int[] widths = new int[keys.length];
		String[] labels = new String[keys.length];
		List<List<Integer>> rows = new ArrayList<>();
		List<Integer> row = new ArrayList<>();
		int rowW = 0;
		for (int i = 0; i < keys.length; i++) {
			labels[i] = label(client, keys[i]);
			widths[i] = Math.max(16, font.width(labels[i]) + 6);
			int add = (row.isEmpty() ? 0 : gap) + widths[i];
			if (!row.isEmpty() && rowW + add > maxRowW) {
				rows.add(row);
				row = new ArrayList<>();
				rowW = 0;
				add = widths[i];
			}
			row.add(i);
			rowW += add;
		}
		rows.add(row);
		int panelW = maxRowW + 16;
		int panelH = 28 + rows.size() * (boxH + gap) + 2;
		int x = (g.guiWidth() - panelW) / 2;
		int y = g.guiHeight() - 46 - panelH;
		g.fill(x, y, x + panelW, y + panelH, 0xC0101010);
		g.fill(x, y, x + panelW, y + 1, 0xFF6B4A2A);
		g.centeredText(font, Component.literal("Escape the cluster!").withColor(HuntDefs.BROWN), x + panelW / 2, y + 5, 0xFFFFFFFF);
		int barX = x + 8;
		int barW = panelW - 16;
		g.fill(barX, y + 17, barX + barW, y + 21, 0xFF3A2A1A);
		g.fill(barX, y + 17, barX + Math.round(barW * progress / (float) keys.length), y + 21, 0xFF55FF55);
		int keyY = y + 26;
		for (List<Integer> r : rows) {
			int total = -gap;
			for (int i : r) {
				total += widths[i] + gap;
			}
			int bx = x + (panelW - total) / 2;
			for (int i : r) {
				boolean done = i < progress;
				boolean next = i == progress;
				int border = next ? 0xFFFFFFFF : done ? 0xFF3C6E3C : 0xFF808080;
				int fill = next ? 0xFF4A3520 : done ? 0xFF1E3A1E : 0xFF2A2A2A;
				g.fill(bx, keyY, bx + widths[i], keyY + boxH, border);
				g.fill(bx + 1, keyY + 1, bx + widths[i] - 1, keyY + boxH - 1, fill);
				int color = done ? 0xFF6FA86F : next ? 0xFFFFFF55 : 0xFFDDDDDD;
				g.centeredText(font, Component.literal(labels[i]), bx + widths[i] / 2, keyY + 4, color);
				bx += widths[i] + gap;
			}
			keyY += boxH + gap;
		}
	}
}
