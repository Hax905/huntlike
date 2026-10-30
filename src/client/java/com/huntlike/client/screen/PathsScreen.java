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

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.huntlike.HuntLike;
import com.huntlike.client.ClientHuntState;
import com.huntlike.hunt.HuntData;
import com.huntlike.network.HuntPayloads;
import com.huntlike.path.WildfirePath;
import com.huntlike.path.WildfirePath.Buff;

/**
 * The Paths, opened from the soul in the Soul Nexus: a dark map you drag around. Each Path is a chain of goals (reached
 * ones lit, the next one pulsing, later ones hidden) with its buffs below. Right-click a buff to take it, left-click to
 * give it back.
 */
public class PathsScreen extends Screen {
	private static final int W = 360;
	private static final int H = 226;
	private static final int NODE = 20;
	private static final int GAP = 64;
	private static final int BUFF = 20;
	private static final int WHITE = 0xFFFFFFFF;

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
		// The Path's goals sit 40 px above the map's middle, so the whole Path (title to buff tree) is centered.
		return (int) ((mapY0() + mapY1()) / 2 - 40 + panY);
	}

	private int[] node(int i) {
		return new int[] {originX() + i * GAP - NODE / 2, originY() - NODE / 2, NODE, NODE};
	}

	/** The buff tree: Fire Resistance on the left, Burning Blows and Warm Heart next, Nether Strength and Hot Heart last. */
	private int[] buff(int i) {
		Buff buff = Buff.values()[i];
		int column = buff.tier - 1;
		int row = switch (buff) {
			case FIRE_RESISTANCE -> 0;
			case BURNING_BLOWS, NETHER_STRENGTH -> -1;
			default -> 1;
		};
		int x = originX() + GAP * 2 - (BUFF + 40) + column * (BUFF + 40) - BUFF / 2;
		int y = originY() + 80 + row * (BUFF + 16) - BUFF / 2;
		return new int[] {x, y, BUFF, BUFF};
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
		drawPortal(g);
		if (WildfirePath.discovered(data)) {
			// The Path of the Wildfire's own ground: nether bricks in a lava frame.
			int[] r = wildfireArea();
			g.enableScissor(r[0], r[1], r[2], r[3]);
			drawBricks(g);
			g.disableScissor();
			var lava = block("lava_still");
			g.blitSprite(RenderPipelines.GUI_TEXTURED, lava, r[0] - 2, r[1] - 2, r[2] - r[0] + 4, 2);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, lava, r[0] - 2, r[3], r[2] - r[0] + 4, 2);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, lava, r[0] - 2, r[1], 2, r[3] - r[1]);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, lava, r[2], r[1], 2, r[3] - r[1]);
		}
		List<Component> tooltip = null;
		if (!WildfirePath.discovered(data)) {
			Component none = Component.literal("No Paths discovered yet...").withStyle(style -> style.withItalic(true)).withColor(0x808080);
			g.text(this.font, none, left + W / 2 - this.font.width(none) / 2, (mapY0() + mapY1()) / 2 - 4, WHITE, false);
		} else {
			tooltip = drawWildfire(g, data, mouseX, mouseY);
		}
		g.disableScissor();
		if (tooltip != null && inMap(mouseX, mouseY)) {
			g.setTooltipForNextFrame(this.font, tooltip, Optional.empty(), mouseX, mouseY);
		}
	}

	// ---------------------------------------------------------------- textures

	private static net.minecraft.client.renderer.texture.TextureAtlasSprite block(String name) {
		return net.minecraft.client.Minecraft.getInstance().getAtlasManager().get(new net.minecraft.client.resources.model.sprite.SpriteId(
				net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS, net.minecraft.resources.Identifier.withDefaultNamespace("block/" + name)));
	}

	private static final String[] BRICKS = {"nether_bricks", "nether_bricks", "nether_bricks", "red_nether_bricks", "cracked_nether_bricks",
			"chiseled_nether_bricks"};

	private static final net.minecraft.resources.Identifier END_PORTAL =
			net.minecraft.client.renderer.blockentity.AbstractEndPortalRenderer.END_PORTAL_LOCATION;

	/** The Paths map's floor: the end portal's starfield, darkened, drifting slowly and moving with the map. */
	private void drawPortal(GuiGraphicsExtractor g) {
		int size = 256;
		double drift = System.currentTimeMillis() / 200.0 % size;
		int ox = (int) Math.floor(panX * 0.5 + drift);
		int oy = (int) Math.floor(panY * 0.5);
		for (int y = mapY0() + Math.floorMod(oy, size) - size; y < mapY1(); y += size) {
			for (int x = mapX0() + Math.floorMod(ox, size) - size; x < mapX1(); x += size) {
				g.blit(RenderPipelines.GUI_TEXTURED, END_PORTAL, x, y, 0.0F, 0.0F, size, size, size, size);
			}
		}
		g.fill(mapX0(), mapY0(), mapX1(), mapY1(), 0x99000000);
	}

	/** The area the Path of the Wildfire covers on the map: left, top, right, bottom. */
	private int[] wildfireArea() {
		int left = node(0)[0] - 14;
		int right = node(WildfirePath.GOALS.size() - 1)[0] + NODE + 14;
		int top = originY() - 56;
		int bottom = buff(Buff.WARM_HEART.ordinal())[1] + BUFF + 10;
		return new int[] {left, top, right, bottom};
	}

	/** The Path of the Wildfire's floor: a random mix of nether bricks that moves with the map. */
	private void drawBricks(GuiGraphicsExtractor g) {
		int ox = (int) Math.floor(panX);
		int oy = (int) Math.floor(panY);
		int startX = mapX0() + Math.floorMod(ox, 16) - 16;
		int startY = mapY0() + Math.floorMod(oy, 16) - 16;
		for (int y = startY; y < mapY1(); y += 16) {
			for (int x = startX; x < mapX1(); x += 16) {
				int tx = Math.floorDiv(x - mapX0() - ox, 16);
				int ty = Math.floorDiv(y - mapY0() - oy, 16);
				long h = (tx * 73856093L) ^ (ty * 19349663L);
				h ^= h >>> 13;
				String name = BRICKS[(int) Math.floorMod(h * 0x9E3779B97F4A7C15L >>> 40, (long) BRICKS.length)];
				g.blitSprite(RenderPipelines.GUI_TEXTURED, block(name), x, y, 16, 16);
			}
		}
		g.fill(mapX0(), mapY0(), mapX1(), mapY1(), 0x66000000);
	}

	/** A square: a lava frame (animated) around obsidian, or crying obsidian while locked. */
	private static void square(GuiGraphicsExtractor g, int x, int y, int size, boolean unlocked) {
		var lava = block("lava_still");
		g.blitSprite(RenderPipelines.GUI_TEXTURED, lava, x - 2, y - 2, size + 4, 2);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, lava, x - 2, y + size, size + 4, 2);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, lava, x - 2, y, 2, size);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, lava, x + size, y, 2, size);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, block(unlocked ? "obsidian" : "crying_obsidian"), x, y, size, size);
	}

	/** A lava line between two points (straight, or bent at the middle). */
	private static void lavaLine(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
		var lava = block("lava_still");
		int mid = (x0 + x1) / 2;
		g.blitSprite(RenderPipelines.GUI_TEXTURED, lava, Math.min(x0, mid), y0 - 1, Math.abs(mid - x0) + 2, 2);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, lava, mid - 1, Math.min(y0, y1) - 1, 2, Math.abs(y1 - y0) + 2);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, lava, Math.min(mid, x1), y1 - 1, Math.abs(x1 - mid) + 2, 2);
	}

	private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

	private List<Component> drawWildfire(GuiGraphicsExtractor g, HuntData data, int mouseX, int mouseY) {
		List<Component> tooltip = null;
		Component name = Component.literal(WildfirePath.NAME).withColor(WildfirePath.COLOR);
		int cx = originX() + GAP * 2;
		g.text(this.font, name, cx - this.font.width(name) / 2, originY() - 47, WHITE, true);
		// The points: [free / most].
		String count = WildfirePath.free(data) + "/" + WildfirePath.maxPoints();
		int pw = this.font.width(count) + 10;
		int px = cx - pw / 2;
		int py = originY() - 32;
		var lava = block("lava_still");
		g.blitSprite(RenderPipelines.GUI_TEXTURED, lava, px - 2, py - 2, pw + 4, 15);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, block("obsidian"), px, py, pw, 11);
		g.text(this.font, Component.literal(count).withColor(0xFFA040), px + 5, py + 2, WHITE, true);
		if (mouseX >= px - 2 && mouseX < px + pw + 2 && mouseY >= py - 2 && mouseY < py + 13) {
			tooltip = List.of(Component.literal("Wildfire points: " + WildfirePath.free(data) + " to spend, "
					+ WildfirePath.points(data) + " of " + WildfirePath.maxPoints() + " earned").withColor(0xFFFFFF),
					Component.literal("Gain a ").withColor(0xFFFFFF).append(Component.literal("Wildfire point").withColor(WildfirePath.COLOR))
							.append(Component.literal(" for every goal on this Path!").withColor(0xFFFFFF)));
		}
		int goals = WildfirePath.GOALS.size();
		int reached = data.wildfireGoals;
		for (int i = 0; i < goals - 1; i++) {
			int[] a = node(i);
			lavaLine(g, a[0] + NODE + 2, originY(), a[0] + GAP - 2, originY());
		}
		for (int i = 0; i < goals; i++) {
			int[] b = node(i);
			square(g, b[0], b[1], NODE, i < reached);
			// The numeral shows once the goal before it is reached.
			if (i <= reached) {
				Component mark = Component.literal(ROMAN[i]).withStyle(style -> style.withBold(true)).withColor(0xFFFFFF);
				g.text(this.font, mark, b[0] + (NODE - this.font.width(mark)) / 2, b[1] + (NODE - 7) / 2, WHITE, true);
			}
			if (inside(b, mouseX, mouseY)) {
				tooltip = goalTooltip(data, i);
			}
		}
		// The buff tree: lines first, then the squares.
		for (Buff buff : Buff.values()) {
			if (buff.parent != null) {
				int[] a = buff(buff.parent.ordinal());
				int[] b = buff(buff.ordinal());
				lavaLine(g, a[0] + BUFF + 2, a[1] + BUFF / 2, b[0] - 2, b[1] + BUFF / 2);
			}
		}
		for (Buff buff : Buff.values()) {
			int[] b = buff(buff.ordinal());
			boolean owned = WildfirePath.has(data, buff);
			square(g, b[0], b[1], BUFF, owned);
			var pose = g.pose();
			pose.pushMatrix();
			pose.translate(b[0] + BUFF / 2.0F, b[1] + BUFF / 2.0F - 2.5F);
			pose.scale(0.75F, 0.75F);
			Component numeral = Component.literal(ROMAN[buff.tier - 1]).withColor(0xFFFFFF);
			g.text(this.font, numeral, -this.font.width(numeral) / 2, 0, WHITE, true);
			pose.popMatrix();
			if (inside(b, mouseX, mouseY)) {
				List<Component> lines = new ArrayList<>();
				lines.add(Component.literal(buff.title).withColor(WildfirePath.COLOR));
				lines.add(Component.literal(buff.description).withColor(0xAAAAAA));
				if (owned) {
					lines.add(Component.literal("Taken").withColor(0x55FF55));
					if (WildfirePath.hasChild(data, buff)) {
						lines.add(Component.literal("Give back the buffs after it first").withColor(0xAAAAAA));
					}
				} else if (buff.parent != null && !WildfirePath.has(data, buff.parent)) {
					lines.add(Component.literal("Needs " + buff.parent.title).withColor(0xFF5555));
				} else {
					lines.add(Component.literal("Cost: 1 Wildfire point").withColor(WildfirePath.free(data) > 0 ? 0xFFCC66 : 0xFF5555));
				}
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
