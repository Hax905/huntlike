package com.huntlike.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.huntlike.HuntLike;
import com.huntlike.client.ClientHuntState;
import com.huntlike.hunt.HuntData;
import com.huntlike.network.HuntPayloads;
import com.huntlike.path.ModPaths;
import com.huntlike.path.WildfirePath;
import com.huntlike.path.WildfirePath.Buff;

/**
 * The Paths, opened from the soul in the Soul Nexus: a dark map you drag around. Each Path is a chain of goals (reached
 * ones lit, the next one pulsing, later ones hidden) with its buffs below. Right-click a buff to take it, left-click to
 * give it back.
 */
public class PathsScreen extends Screen {
	private static final int W = 320;
	private static final int H = 212;
	private static final int NODE = 18;
	private static final int GAP = 46;
	private static final int BUFF = 22;
	private static final int WHITE = 0xFFFFFFFF;
	private static final Item[] BUFF_ICONS = {Items.MAGMA_CREAM, Items.BLAZE_ROD, Items.NETHERITE_SWORD, ModPaths.BLAZING_HEART, Items.FIRE_CHARGE};

	private final Screen parent;
	private int left;
	private int top;
	private double panX;
	private double panY;
	private boolean dragging;
	private double dragged;

	public PathsScreen(Screen parent) {
		super(Component.literal("Paths"));
		this.parent = parent;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(parent);
	}

	private int mapX0() {
		return left + 10;
	}

	private int mapY0() {
		return top + 20;
	}

	private int mapX1() {
		return left + W - 10;
	}

	private int mapY1() {
		return top + H - 10;
	}

	/** Where the Path's first goal sits, before panning. */
	private int originX() {
		return (int) (left + W / 2 - GAP * 2 + panX);
	}

	private int originY() {
		return (int) (top + 70 + panY);
	}

	private int[] node(int i) {
		return new int[] {originX() + i * GAP - NODE / 2, originY() - NODE / 2, NODE, NODE};
	}

	private int[] buff(int i) {
		int count = Buff.values().length;
		int span = count * BUFF + (count - 1) * 8;
		return new int[] {originX() + GAP * 2 - span / 2 + i * (BUFF + 8), originY() + 52, BUFF, BUFF};
	}

	private boolean inMap(double x, double y) {
		return x >= mapX0() && x < mapX1() && y >= mapY0() && y < mapY1();
	}

	private static boolean inside(int[] box, double x, double y) {
		return x >= box[0] && x < box[0] + box[2] && y >= box[1] && y < box[1] + box[3];
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		HuntData data = ClientHuntState.data;
		left = (this.width - W) / 2;
		top = (this.height - H) / 2;
		g.blitSprite(RenderPipelines.GUI_TEXTURED, HuntLike.id("menu/window"), left, top, W, H);
		Component title = Component.literal("Paths").withStyle(style -> style.withBold(true)).withColor(0x404040);
		g.text(this.font, title, left + W / 2 - this.font.width(title) / 2, top + 7, 0xFF404040, false);
		g.fill(mapX0(), mapY0(), mapX1(), mapY1(), 0xFF0C0A12);
		g.enableScissor(mapX0(), mapY0(), mapX1(), mapY1());
		// Faint stars that drift with the map.
		for (int i = 0; i < 60; i++) {
			int sx = Math.floorMod((int) (i * 97 + panX * 0.5), mapX1() - mapX0()) + mapX0();
			int sy = Math.floorMod((int) (i * 53 + panY * 0.5), mapY1() - mapY0()) + mapY0();
			g.fill(sx, sy, sx + 1, sy + 1, (i % 3 == 0 ? 0x66FFFFFF : 0x33AACCFF));
		}
		List<Component> tooltip = null;
		if (!WildfirePath.discovered(data)) {
			Component none = Component.literal("No Paths discovered yet...").withStyle(style -> style.withItalic(true)).withColor(0x808080);
			g.text(this.font, none, left + W / 2 - this.font.width(none) / 2, top + H / 2, WHITE, false);
		} else {
			tooltip = drawWildfire(g, data, mouseX, mouseY);
		}
		g.disableScissor();
		if (tooltip != null && inMap(mouseX, mouseY)) {
			g.setTooltipForNextFrame(this.font, tooltip, Optional.empty(), mouseX, mouseY);
		}
	}

	private List<Component> drawWildfire(GuiGraphicsExtractor g, HuntData data, int mouseX, int mouseY) {
		List<Component> tooltip = null;
		int color = 0xFF000000 | WildfirePath.COLOR;
		Component name = Component.literal(WildfirePath.NAME).withColor(WildfirePath.COLOR);
		g.text(this.font, name, originX() + GAP * 2 - this.font.width(name) / 2, originY() - 34, WHITE, true);
		// The points: a small flame and the free / total count.
		Component points = Component.literal("♨ " + WildfirePath.free(data) + " / " + WildfirePath.points(data)).withColor(0xFFCC66);
		int px = originX() + GAP * 2 - this.font.width(points) / 2;
		int py = originY() - 22;
		g.text(this.font, points, px, py, WHITE, true);
		if (mouseX >= px && mouseX < px + this.font.width(points) && mouseY >= py && mouseY < py + 9) {
			tooltip = List.of(Component.literal("Gain a ").withColor(0xFFFFFF).append(Component.literal("Wildfire point").withColor(WildfirePath.COLOR))
					.append(Component.literal(" for every goal on this Path!").withColor(0xFFFFFF)));
		}
		int goals = WildfirePath.GOALS.size();
		int reached = data.wildfireGoals;
		double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 300.0);
		for (int i = 0; i < goals - 1; i++) {
			int[] a = node(i);
			int lineColor = i < reached ? color : i == reached ? 0xFF5A3A22 : 0xFF2A2230;
			g.fill(a[0] + NODE, originY() - 1, a[0] + GAP, originY() + 1, lineColor);
		}
		for (int i = 0; i < goals; i++) {
			int[] b = node(i);
			boolean done = i < reached;
			boolean next = i == reached;
			int border = done ? color : next ? (0xFF000000 | (int) (0x60 + pulse * 0x9F) << 16 | (int) (0x40 + pulse * 0x50) << 8 | 0x20) : 0xFF3A3344;
			g.fill(b[0] - 1, b[1] - 1, b[0] + b[2] + 1, b[1] + b[3] + 1, border);
			g.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], done ? 0xFF5A2410 : 0xFF17131F);
			String label = done ? "✔" : next ? "!" : "?";
			Component mark = Component.literal(label).withColor(done ? 0xFFDD88 : next ? 0xFFAA00 : 0x6A6278);
			g.text(this.font, mark, b[0] + b[2] / 2 - this.font.width(mark) / 2 + 1, b[1] + 5, WHITE, false);
			if (inside(b, mouseX, mouseY)) {
				tooltip = goalTooltip(data, i);
			}
		}
		// The buffs.
		for (Buff buff : Buff.values()) {
			int[] b = buff(buff.ordinal());
			boolean owned = WildfirePath.has(data, buff);
			boolean hover = inside(b, mouseX, mouseY);
			g.fill(b[0] - 1, b[1] - 1, b[0] + b[2] + 1, b[1] + b[3] + 1, owned ? color : hover ? 0xFF8A6A50 : 0xFF3A3344);
			g.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], owned ? 0xFF4A1E0C : 0xFF17131F);
			g.item(new ItemStack(BUFF_ICONS[buff.ordinal()]), b[0] + 3, b[1] + 3);
			if (hover) {
				List<Component> lines = new ArrayList<>();
				lines.add(Component.literal(buff.title).withColor(WildfirePath.COLOR));
				lines.add(Component.literal(buff.description).withColor(0xAAAAAA));
				lines.add(owned ? Component.literal("Taken").withColor(0x55FF55)
						: Component.literal("Cost: 1 Wildfire point").withColor(WildfirePath.free(data) > 0 ? 0xFFCC66 : 0xFF5555));
				tooltip = lines;
			}
		}
		return tooltip;
	}

	private static List<Component> goalTooltip(HuntData data, int i) {
		int reached = data.wildfireGoals;
		List<Component> lines = new ArrayList<>();
		if (i > reached) {
			lines.add(Component.literal("???").withColor(0x808080));
			lines.add(Component.literal("Reach the goals before it first.").withColor(0x808080));
			return lines;
		}
		lines.add(Component.literal(WildfirePath.GOALS.get(i)).withColor(WildfirePath.COLOR));
		if (i < reached) {
			lines.add(Component.literal("Done").withColor(0x55FF55));
		} else if (i == 2) {
			lines.add(Component.literal("Blazes: " + Math.min(WildfirePath.BLAZES, data.wildfireBlazes) + "/" + WildfirePath.BLAZES).withColor(0xFFFFFF));
		} else {
			lines.add(Component.literal("Next goal").withColor(0xFFAA00));
		}
		lines.add(Component.literal("+1 Wildfire point").withColor(0xFFCC66));
		if (i < WildfirePath.BONUS_GOALS) {
			lines.add(Component.literal("Blazing Heart drop chance +1%").withColor(0xFFCC66));
		}
		return lines;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		HuntData data = ClientHuntState.data;
		if (WildfirePath.discovered(data) && inMap(event.x(), event.y())) {
			boolean add = event.button() == InputConstants.MOUSE_BUTTON_RIGHT;
			boolean take = event.button() == InputConstants.MOUSE_BUTTON_LEFT;
			for (Buff buff : Buff.values()) {
				if ((add || take) && inside(buff(buff.ordinal()), event.x(), event.y())) {
					ClientPlayNetworking.send(new HuntPayloads.Action("wildfire_buff", Integer.toString(buff.ordinal()), add ? 1 : -1));
					return true;
				}
			}
		}
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && inMap(event.x(), event.y())) {
			dragging = true;
			dragged = 0;
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging) {
			panX = Math.max(-220, Math.min(220, panX + dx));
			panY = Math.max(-120, Math.min(120, panY + dy));
			dragged += Math.abs(dx) + Math.abs(dy);
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = false;
		return super.mouseReleased(event);
	}
}
