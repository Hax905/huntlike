package com.huntlike.client.screen;

import java.util.List;
import java.util.Optional;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import com.huntlike.HuntLike;
import com.huntlike.client.ClientHuntState;
import com.huntlike.hunt.HuntData;
import com.huntlike.path.WildfirePath;

/**
 * The Paths, opened from the soul in the Soul Nexus: the Paths you have discovered, side by side over the starfield
 * (drag to move); a click opens one. Paths not discovered yet aren't shown at all.
 */
public class PathSelectScreen extends Screen {
	private static final int W = 300;
	private static final int H = 180;
	private static final int NODE = 28;
	private static final int GAP = 110;
	private static final int WHITE = 0xFFFFFFFF;

	/**
	 * The Paths in order: name, color, frame block, the block of a reached step and of the current one. The Path of the
	 * Wildfire: lava around obsidian and crying obsidian; Tundra's Path: spruce wood around blue ice and ice.
	 */
	private record Entry(String name, int color, String frame, String reached, String current) {
	}

	private static final List<Entry> PATHS = List.of(
			new Entry(WildfirePath.NAME, WildfirePath.COLOR, "lava_still", "obsidian", "crying_obsidian"),
			new Entry("Tundra's Path", 0x9AD8FF, "spruce_log", "blue_ice", "ice"));

	private final Screen parent;
	private int left;
	private int top;
	private double panX;
	private double panY;
	private boolean dragging;
	private double dragged;

	public PathSelectScreen(Screen parent) {
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

	private int[] map() {
		return new int[] {left + 10, top + 20, left + W - 10, top + H - 10};
	}

	/** The discovered Paths, as indexes into PATHS. */
	private static List<Integer> shown(HuntData data) {
		List<Integer> list = new java.util.ArrayList<>();
		for (int i = 0; i < PATHS.size(); i++) {
			if (discovered(data, i)) {
				list.add(i);
			}
		}
		return list;
	}

	/** The box of the n-th shown Path (of `count`). */
	private int[] node(int n, int count) {
		int[] m = map();
		int cx = (m[0] + m[2]) / 2 - (count - 1) * GAP / 2 + n * GAP + (int) panX;
		int cy = (m[1] + m[3]) / 2 - 6 + (int) panY;
		return new int[] {cx - NODE / 2, cy - NODE / 2, NODE, NODE};
	}

	private static boolean discovered(HuntData data, int i) {
		return i == 0 && WildfirePath.discovered(data);
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
		int[] m = map();
		PathsScreen.drawStarfield(g, m[0], m[1], m[2], m[3], panX, panY);
		g.enableScissor(m[0], m[1], m[2], m[3]);
		List<Component> tooltip = null;
		List<Integer> shown = shown(data);
		if (shown.isEmpty()) {
			Component none = Component.literal("No Paths discovered yet...").withStyle(style -> style.withItalic(true)).withColor(0x808080);
			g.text(this.font, none, (m[0] + m[2]) / 2 - this.font.width(none) / 2, (m[1] + m[3]) / 2 - 4, WHITE, true);
		}
		for (int n = 0; n < shown.size(); n++) {
			Entry entry = PATHS.get(shown.get(n));
			int[] b = node(n, shown.size());
			var frame = PathsScreen.block(entry.frame());
			g.blitSprite(RenderPipelines.GUI_TEXTURED, frame, b[0] - 3, b[1] - 3, NODE + 6, 3);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, frame, b[0] - 3, b[1] + NODE, NODE + 6, 3);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, frame, b[0] - 3, b[1], 3, NODE);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, frame, b[0] + NODE, b[1], 3, NODE);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, PathsScreen.block(entry.reached()), b[0], b[1], NODE, NODE);
			Component name = Component.literal(entry.name()).withColor(entry.color());
			g.text(this.font, name, b[0] + NODE / 2 - this.font.width(name) / 2, b[1] + NODE + 7, WHITE, true);
			boolean hover = mouseX >= b[0] - 3 && mouseX < b[0] + NODE + 3 && mouseY >= b[1] - 3 && mouseY < b[1] + NODE + 3;
			if (hover) {
				tooltip = List.of(Component.literal(entry.name()).withColor(entry.color()), Component.literal("Click to open").withColor(0xAAAAAA));
			}
		}
		g.disableScissor();
		if (tooltip != null) {
			g.setTooltipForNextFrame(this.font, tooltip, Optional.empty(), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int[] m = map();
		if (event.x() >= m[0] && event.x() < m[2] && event.y() >= m[1] && event.y() < m[3]) {
			dragging = true;
			dragged = 0;
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging) {
			panX = Math.max(-200, Math.min(200, panX + dx));
			panY = Math.max(-60, Math.min(60, panY + dy));
			dragged += Math.abs(dx) + Math.abs(dy);
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	/** A click that didn't drag opens the Path under it (if discovered). */
	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging && dragged < 3) {
			List<Integer> shown = shown(ClientHuntState.data);
			for (int n = 0; n < shown.size(); n++) {
				int[] b = node(n, shown.size());
				// Only the Path of the Wildfire has a screen so far.
				if (event.x() >= b[0] - 3 && event.x() < b[0] + NODE + 3 && event.y() >= b[1] - 3 && event.y() < b[1] + NODE + 3
						&& shown.get(n) == 0) {
					dragging = false;
					this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
							net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
					this.minecraft.gui.setScreen(new PathsScreen(this));
					return true;
				}
			}
		}
		dragging = false;
		return super.mouseReleased(event);
	}
}
