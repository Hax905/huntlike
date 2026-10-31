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
 * The Paths, opened from the soul in the Soul Nexus: every Path on one line over the starfield (drag to move). A
 * discovered Path opens on click; one not found yet is a crying obsidian square.
 */
public class PathSelectScreen extends Screen {
	private static final int W = 300;
	private static final int H = 180;
	private static final int NODE = 28;
	private static final int GAP = 110;
	private static final int WHITE = 0xFFFFFFFF;

	/** The Paths in order: name, color, frame block, inner block once discovered. */
	private record Entry(String name, int color, String frame, String inner) {
	}

	private static final List<Entry> PATHS = List.of(
			new Entry(WildfirePath.NAME, WildfirePath.COLOR, "lava_still", "obsidian"),
			new Entry("Tundra's Path", 0x9AD8FF, "blue_ice", "packed_ice"));

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

	private int[] node(int i) {
		int[] m = map();
		int cx = (m[0] + m[2]) / 2 - (PATHS.size() - 1) * GAP / 2 + i * GAP + (int) panX;
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
		// The line the Paths sit on.
		int[] first = node(0);
		int[] last = node(PATHS.size() - 1);
		int lineY = first[1] + NODE / 2;
		g.fill(first[0] + NODE + 3, lineY - 1, last[0] - 3, lineY + 1, 0xFF7A7A86);
		for (int i = 0; i < PATHS.size(); i++) {
			Entry entry = PATHS.get(i);
			int[] b = node(i);
			boolean found = discovered(data, i);
			var frame = PathsScreen.block(entry.frame());
			g.blitSprite(RenderPipelines.GUI_TEXTURED, frame, b[0] - 3, b[1] - 3, NODE + 6, 3);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, frame, b[0] - 3, b[1] + NODE, NODE + 6, 3);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, frame, b[0] - 3, b[1], 3, NODE);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, frame, b[0] + NODE, b[1], 3, NODE);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, PathsScreen.block(found ? entry.inner() : "crying_obsidian"), b[0], b[1], NODE, NODE);
			Component name = found || i > 0 ? Component.literal(entry.name()).withColor(entry.color())
					: Component.literal("???").withColor(0x808080);
			g.text(this.font, name, b[0] + NODE / 2 - this.font.width(name) / 2, b[1] + NODE + 7, WHITE, true);
			boolean hover = mouseX >= b[0] - 3 && mouseX < b[0] + NODE + 3 && mouseY >= b[1] - 3 && mouseY < b[1] + NODE + 3;
			if (hover) {
				tooltip = found ? List.of(Component.literal(entry.name()).withColor(entry.color()),
						Component.literal("Click to open").withColor(0xAAAAAA))
						: List.of(Component.literal(i == 0 ? "???" : entry.name()).withColor(i == 0 ? 0x808080 : entry.color()),
								Component.literal("Not discovered yet...").withStyle(style -> style.withItalic(true)).withColor(0x808080));
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
			for (int i = 0; i < PATHS.size(); i++) {
				int[] b = node(i);
				if (event.x() >= b[0] - 3 && event.x() < b[0] + NODE + 3 && event.y() >= b[1] - 3 && event.y() < b[1] + NODE + 3
						&& discovered(ClientHuntState.data, i)) {
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
