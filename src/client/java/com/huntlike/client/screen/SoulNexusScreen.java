package com.huntlike.client.screen;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.huntlike.HuntLike;
import com.huntlike.client.ClientHuntState;
import com.huntlike.hunt.HuntData;
import com.huntlike.network.HuntPayloads;
import com.huntlike.nexus.SoulNexus;

/**
 * The Soul Nexus: your soul in the middle of a triangle with Health, Defense and Damage in the corners. Right-click a
 * corner to add a point, left-click to take one back; Shift does 10. The cyan shape inside shows where the points are.
 */
public class SoulNexusScreen extends Screen {
	private static final int W = 300;
	private static final int H = 230;
	private static final int RADIUS = 58;
	private static final int CORNER_W = 78;
	private static final int CORNER_H = 34;
	private static final int CYAN = 0xFF55FFFF;
	private static final int DIM = 0xFF3A6A78;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFFAAAAAA;
	private static final String[] CORNERS = {SoulNexus.HEALTH, SoulNexus.DEFENSE, SoulNexus.DAMAGE};
	private static final String[] NAMES = {"Health", "Defense", "Damage"};
	private static final int[] COLORS = {0xFFFF5555, 0xFF55AAFF, 0xFFFFAA00};

	private int left;
	private int top;
	private int cx;
	private int cy;

	public SoulNexusScreen() {
		super(Component.literal("Soul Nexus"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/** Corner i's point of the triangle: Health on top, Defense bottom left, Damage bottom right. */
	private double[] vertex(int i, double scale) {
		double angle = Math.toRadians(-90 + 120 * (i == 0 ? 0 : i == 1 ? 2 : 1));
		return new double[] {cx + Math.cos(angle) * RADIUS * scale, cy + Math.sin(angle) * RADIUS * scale};
	}

	/** The corner button's box: x, y, w, h. */
	private int[] box(int i) {
		double[] v = vertex(i, 1.0);
		int x = (int) v[0] - CORNER_W / 2;
		int y = i == 0 ? (int) v[1] - CORNER_H - 2 : (int) v[1] + 4;
		if (i == 1) {
			x = Math.max(left + 8, x - 10);
		} else if (i == 2) {
			x = Math.min(left + W - 8 - CORNER_W, x + 10);
		}
		return new int[] {x, y, CORNER_W, CORNER_H};
	}

	private static boolean inside(int[] box, double x, double y) {
		return x >= box[0] && x < box[0] + box[2] && y >= box[1] && y < box[1] + box[3];
	}

	private boolean overSoul(double x, double y) {
		return Math.hypot(x - cx, y - cy) <= 12;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		HuntData data = ClientHuntState.data;
		left = (this.width - W) / 2;
		top = (this.height - H) / 2;
		cx = left + W / 2;
		cy = top + 114;
		g.blitSprite(RenderPipelines.GUI_TEXTURED, HuntLike.id("menu/window"), left, top, W, H);
		g.text(this.font, "Soul Nexus", left + 10, top + 7, 0xFF404040, false);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, HuntLike.id("menu/content"), left + 6, top + 18, W - 12, H - 24);

		int free = SoulNexus.free(data);
		int earned = SoulNexus.earned(data);
		g.centeredText(this.font, Component.literal("Soul Points: ").withColor(0xFFFFFF)
				.append(Component.literal(free + " free").withColor(free > 0 ? 0x55FFFF : 0xAAAAAA))
				.append(Component.literal(" / " + earned + " earned").withColor(0xAAAAAA)), cx, top + 188, WHITE);

		// The triangle, then the cyan shape of the points spent (each corner's share of the cap).
		for (int i = 0; i < 3; i++) {
			line(g, vertex(i, 1.0), vertex((i + 1) % 3, 1.0), DIM, 2);
			line(g, vertex(i, 0.0), vertex(i, 1.0), 0xFF24434C, 1);
		}
		double[][] shape = new double[3][];
		for (int i = 0; i < 3; i++) {
			shape[i] = vertex(i, Math.max(0.06, SoulNexus.get(data, CORNERS[i]) / (double) SoulNexus.CAP));
		}
		for (int i = 0; i < 3; i++) {
			line(g, shape[i], shape[(i + 1) % 3], CYAN, 2);
		}

		// The soul: a glowing orb that slowly pulses.
		double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 400.0);
		drawOrb(g, cx, cy, 11 + (int) Math.round(pulse * 2), 0x2255FFFF);
		drawOrb(g, cx, cy, 9, 0x6655FFFF);
		drawOrb(g, cx, cy, 7, 0xFF55FFFF);
		drawOrb(g, cx, cy, 4, 0xFFCCFFFF);
		drawOrb(g, cx - 2, cy - 2, 1, 0xFFFFFFFF);

		for (int i = 0; i < 3; i++) {
			int[] b = box(i);
			boolean hover = inside(b, mouseX, mouseY);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, HuntLike.id(hover ? "menu/panel" : "menu/panel_dark"), b[0], b[1], b[2], b[3]);
			int points = SoulNexus.get(data, CORNERS[i]);
			g.centeredText(this.font, Component.literal(NAMES[i]).withColor(COLORS[i] & 0xFFFFFF), b[0] + b[2] / 2, b[1] + 4, WHITE);
			g.centeredText(this.font, Component.literal(points + "/" + SoulNexus.CAP).withColor(points >= SoulNexus.CAP ? 0xFFAA00 : 0xFFFFFF),
					b[0] + b[2] / 2, b[1] + 14, WHITE);
			g.centeredText(this.font, Component.literal(effect(i, points)).withColor(0x55FFFF), b[0] + b[2] / 2, b[1] + 24, WHITE);
			if (hover) {
				g.setTooltipForNextFrame(this.font, List.of(Component.literal(NAMES[i]).withColor(COLORS[i] & 0xFFFFFF),
						Component.literal(describe(i)).withColor(0xAAAAAA),
						Component.literal("Right-click: add a point").withColor(0xAAAAAA),
						Component.literal("Left-click: take a point back").withColor(0xAAAAAA),
						Component.literal("Shift: 10 at once").withColor(0xAAAAAA)), java.util.Optional.empty(), mouseX, mouseY);
			}
		}
		if (overSoul(mouseX, mouseY)) {
			g.setTooltipForNextFrame(this.font, List.of(Component.literal("Your soul").withColor(0x55FFFF),
					Component.literal("The Paths will open from here.").withColor(0xAAAAAA)), java.util.Optional.empty(), mouseX, mouseY);
		}
		g.centeredText(this.font, Component.literal("1 point for every boss tier beaten for the first time").withColor(0x808080), cx,
				top + 200, WHITE);
		g.centeredText(this.font, Component.literal("Right-click: add · Left-click: take back · Shift: x10").withColor(0x808080), cx,
				top + 212, WHITE);
	}

	private static String effect(int i, int points) {
		return switch (i) {
			case 0 -> "+" + points / 2 + " max health";
			case 1 -> "-" + trim(points * 0.25) + "% damage taken";
			default -> "+" + trim(points * 0.5) + "% damage";
		};
	}

	private static String describe(int i) {
		return switch (i) {
			case 0 -> "2 points: +1 max health";
			case 1 -> "1 point: 0.25% less damage taken (not Soul Strike)";
			default -> "1 point: 0.5% more damage dealt";
		};
	}

	private static String trim(double value) {
		return value == Math.floor(value) ? Integer.toString((int) value) : Double.toString(value);
	}

	/** A filled circle of the given radius, row by row. */
	private static void drawOrb(GuiGraphicsExtractor g, int x, int y, int r, int color) {
		for (int dy = -r; dy <= r; dy++) {
			int dx = (int) Math.floor(Math.sqrt(r * r - dy * dy + 0.5));
			g.fill(x - dx, y + dy, x + dx + 1, y + dy + 1, color);
		}
	}

	/** A line of small squares from a to b. */
	private static void line(GuiGraphicsExtractor g, double[] a, double[] b, int color, int size) {
		double dx = b[0] - a[0];
		double dy = b[1] - a[1];
		int steps = Math.max(1, (int) Math.ceil(Math.hypot(dx, dy)));
		for (int s = 0; s <= steps; s++) {
			int x = (int) Math.round(a[0] + dx * s / steps);
			int y = (int) Math.round(a[1] + dy * s / steps);
			g.fill(x, y, x + size, y + size, color);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		boolean add = event.button() == InputConstants.MOUSE_BUTTON_RIGHT;
		boolean take = event.button() == InputConstants.MOUSE_BUTTON_LEFT;
		if (add || take) {
			int amount = (event.modifiers() & InputConstants.MOD_SHIFT) != 0 ? 10 : 1;
			for (int i = 0; i < 3; i++) {
				if (inside(box(i), event.x(), event.y())) {
					ClientPlayNetworking.send(new HuntPayloads.Action("nexus", CORNERS[i], add ? amount : -amount));
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}
}
