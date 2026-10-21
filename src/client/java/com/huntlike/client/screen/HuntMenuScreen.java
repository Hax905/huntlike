package com.huntlike.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.huntlike.HuntLike;
import com.huntlike.client.ClientHuntState;
import com.huntlike.hunt.HuntData;
import com.huntlike.hunt.HuntDefs;
import com.huntlike.hunt.HuntDefs.Ability;
import com.huntlike.hunt.HuntDefs.Drop;
import com.huntlike.hunt.HuntDefs.Hunt;
import com.huntlike.hunt.HuntDefs.Tier;
import com.huntlike.hunt.HuntDefs.Unlock;
import com.huntlike.network.HuntPayloads;
import com.huntlike.registry.ModItems;

/**
 * The Hunt menu opened by the Alpha Altar: Quests, Gear and Ranks tabs.
 * Every tab starts on the Hunt rail; clicking a Hunt's symbol (or Enter) opens its panel.
 */
public class HuntMenuScreen extends Screen {
	private static final Identifier ANKH = HuntLike.id("textures/gui/ankh.png");
	private static final String[] TABS = {"Quests", "Gear", "Ranks"};

	private static final int W = 360;
	private static final int MAX_H = 256;
	private static final int TAB_H = 20;

	private static final int WHITE = 0xFFFFFFFF;
	private static final int TAB_TEXT = 0xFF404040;
	private static final int RAIL = 0xFFC6C6C6;
	private static final Identifier BUTTON_SPRITE = Identifier.withDefaultNamespace("widget/button");
	private static final Identifier BUTTON_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/button_highlighted");
	private static final Identifier BUTTON_DISABLED = Identifier.withDefaultNamespace("widget/button_disabled");
	private static final int TEXT_GRAY = 0xFFAAAAAA;
	private static final int TEXT_DIM = 0xFF6F6F6F;

	private int tab;
	/** False: the Hunt rail only. True: the selected Hunt's panel is open. */
	private boolean panelOpen;
	private int huntIndex;
	private int questTier = 1;
	private int rankView;
	private int abilityScroll;
	/** Client-only boss entities for the boss card preview. */
	private final java.util.Map<Integer, LivingEntity> previews = new java.util.HashMap<>();
	/** The tier whose Drops panel is hovered this frame, or null. */
	private Tier dropsHover;
	/** Gear tab page: 0 Armor, 1 Misc, 2 Weapons. */
	private int gearPage = 1;
	private int gearScroll;
	/** The token trade button (x, y, w, h) from the last frame, or null. */
	private int[] tokenButton;
	private boolean cancelArmed;

	// Layout, recomputed each frame.
	private int H = MAX_H;
	private int left;
	private int top;
	private int ix;
	private int iy;
	private int iw;
	private int ih;
	private int px;
	private int py;
	private int pw;
	private int ph;

	/** Center of each labelled button drawn last frame, for automated tests. */
	private final java.util.Map<String, int[]> buttonCenters = new java.util.HashMap<>();

	/** Clickable areas registered while drawing, checked on click. */
	private final List<Hit> hits = new ArrayList<>();

	private record Hit(int x, int y, int w, int h, Runnable action) {
		boolean contains(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	public HuntMenuScreen(int tab) {
		super(Component.literal("Hunt Menu"));
		this.tab = Math.max(0, Math.min(2, tab));
		HuntData data = ClientHuntState.data;
		for (int i = 0; i < HuntDefs.HUNTS.size(); i++) {
			if (HuntDefs.HUNTS.get(i).id().equals(data.selectedHunt)) {
				this.huntIndex = i;
			}
		}
		this.rankView = progress().rank();
		this.questTier = highestUnlockedTier();
	}

	@Override
	protected void init() {
		ClientPlayNetworking.send(new HuntPayloads.Action("refresh", hunt().id(), 0));
	}

	/** GUI coordinates of a button's center from the last frame, or null. Used by automated tests. */
	public int[] buttonCenter(String label) {
		return buttonCenters.get(label);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private Hunt hunt() {
		return HuntDefs.HUNTS.get(huntIndex);
	}

	private HuntData.Progress progress() {
		return ClientHuntState.data.progress(hunt().id());
	}

	private int highestUnlockedTier() {
		int rank = progress().rank();
		int best = 1;
		for (Tier tier : hunt().tiers()) {
			if (tierOpen(tier)) {
				best = tier.number();
			}
		}
		return best;
	}

	/** A tier can be started once its Rank is reached and the previous tier's boss has been defeated. */
	private boolean tierOpen(Tier tier) {
		HuntData.Progress progress = progress();
		return progress.rank() >= tier.unlockRank() && previousDefeated(tier);
	}

	private boolean previousDefeated(Tier tier) {
		return tier.number() == 1 || progress().bossKills[tier.number() - 2] > 0;
	}

	private HuntData.Quest activeQuest() {
		return ClientHuntState.data.quest;
	}

	// ---------------------------------------------------------------- drawing

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		hits.clear();
		H = Math.max(180, Math.min(MAX_H, this.height - TAB_H - 8));
		left = (this.width - W) / 2;
		top = (this.height - H - TAB_H) / 2 + TAB_H;
		ix = left + 8;
		iy = top + 8;
		iw = W - 16;
		ih = H - 16;
		px = ix + 92;
		py = iy + 8;
		pw = iw - 100;
		ph = ih - 16;

		drawTabs(g, mouseX, mouseY, false);
		sprite(g, "window", left, top, W, H);
		drawTabs(g, mouseX, mouseY, true);
		sprite(g, "content", ix - 2, iy - 2, iw + 4, ih + 4);
		sprite(g, "portal", ix, iy, iw, ih);
		drawRail(g, mouseX, mouseY);

		boolean questRunning = activeQuest() != null;
		if (tab == 0 && questRunning) {
			drawActiveQuest(g, mouseX, mouseY);
		} else if (panelOpen) {
			switch (tab) {
				case 0 -> drawBossCard(g, mouseX, mouseY);
				case 1 -> drawGear(g, mouseX, mouseY);
				default -> drawRanks(g, mouseX, mouseY);
			}
		} else if (!hintSeen()) {
			g.text(this.font, Component.literal("Click a symbol or press Enter"), px + 8, iy + ih - 14, TEXT_DIM, false);
		}
		if (dropsHover != null) {
			drawDropsPanel(g, dropsHover, mouseX, mouseY);
			dropsHover = null;
		}
	}

	// ---------------------------------------------------------------- the "click a symbol" hint, shown until first used

	private static Boolean hintSeen;

	private static java.nio.file.Path settingsFile() {
		return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("huntlike-client.properties");
	}

	private static boolean hintSeen() {
		if (hintSeen == null) {
			try {
				hintSeen = java.nio.file.Files.readString(settingsFile()).contains("hintSeen=true");
			} catch (java.io.IOException e) {
				hintSeen = false;
			}
		}
		return hintSeen;
	}

	private static void markHintSeen() {
		if (hintSeen()) {
			return;
		}
		hintSeen = true;
		try {
			java.nio.file.Files.writeString(settingsFile(), "hintSeen=true\n");
		} catch (java.io.IOException e) {
			HuntLike.LOGGER.warn("Could not save HuntLike client settings", e);
		}
	}

	/** Unselected tabs are drawn behind the window, the selected tab in front so it joins the window. */
	private void drawTabs(GuiGraphicsExtractor g, int mouseX, int mouseY, boolean selectedPass) {
		int tabW = W / 3;
		for (int i = 0; i < 3; i++) {
			boolean selected = i == tab;
			if (selected != selectedPass) {
				continue;
			}
			int x = left + i * tabW;
			int y = top - TAB_H;
			if (selected) {
				sprite(g, "tab_selected", x, y, tabW, TAB_H + 4);
			} else {
				sprite(g, "tab", x + 1, y + 2, tabW - 2, TAB_H);
			}
			Component label = Component.literal(TABS[i]);
			g.text(this.font, label, x + (tabW - this.font.width(label)) / 2, y + (selected ? 7 : 8), TAB_TEXT, false);
			if (!selectedPass) {
				int index = i;
				hits.add(new Hit(x, y, tabW, TAB_H, () -> switchTab(index)));
			}
		}
	}

	private void drawRail(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int cx = ix + 46;
		int cy = iy + ih / 2;
		// Slanted rail: leans right toward the selected symbol, then back.
		for (int y = iy; y < iy + ih; y++) {
			double t = Math.abs(y - cy) / (double) (ih / 2);
			int x = (int) Math.round(cx - 12 * t);
			g.fill(x - 1, y, x + 1, y + 1, RAIL);
		}
		for (int offset = -1; offset <= 1; offset++) {
			int index = huntIndex + offset;
			if (index < 0 || index >= HuntDefs.HUNTS.size()) {
				continue;
			}
			int sy = cy + offset * 56;
			int size = offset == 0 ? 32 : 22;
			int x0 = cx - size / 2;
			int y0 = sy - size / 2;
			boolean hover = mouseX >= x0 && mouseX < x0 + size && mouseY >= y0 && mouseY < y0 + size;
			sprite(g, hover ? "symbol_hover" : "symbol", x0 - 1, y0 - 1, size + 2, size + 2);
			g.blit(RenderPipelines.GUI_TEXTURED, ANKH, x0 + 3, y0 + 3, 0.0F, 0.0F, size - 6, size - 6, 26, 26, 26, 26);
			if (offset == 0) {
				hits.add(new Hit(x0, y0, size, size, this::openPanel));
				if (hover) {
					g.setTooltipForNextFrame(this.font, Component.literal(HuntDefs.HUNTS.get(index).name() + " Hunt")
							.withColor(HuntDefs.HUNTS.get(index).color()), mouseX, mouseY);
				}
			}
		}
	}

	private void panelBox(GuiGraphicsExtractor g) {
		sprite(g, "panel", px, py, pw, ph);
	}

	/** Draws a button, registers its click and returns whether the mouse is over it. */
	private boolean button(GuiGraphicsExtractor g, int x, int y, int w, int h, Component label, boolean enabled,
			int mouseX, int mouseY, Runnable action) {
		boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		buttonCenters.put(label.getString(), new int[] {x + w / 2, y + h / 2});
		g.blitSprite(RenderPipelines.GUI_TEXTURED, !enabled ? BUTTON_DISABLED : hover ? BUTTON_HIGHLIGHTED : BUTTON_SPRITE, x, y, w, h);
		g.centeredText(this.font, label, x + w / 2, y + (h - 8) / 2, enabled ? WHITE : 0xFFA0A0A0);
		if (enabled && action != null) {
			hits.add(new Hit(x, y, w, h, action));
		}
		return hover;
	}

	/** Draws one of HuntLike's GUI sprites (assets/huntlike/textures/gui/sprites/menu/<name>.png). */
	private static void sprite(GuiGraphicsExtractor g, String name, int x, int y, int w, int h) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, HuntLike.id("menu/" + name), x, y, w, h);
	}

	private static MutableComponent colored(String text, int rgb) {
		return Component.literal(text).withColor(rgb);
	}

	// ---------------------------------------------------------------- Quests tab

	private void drawBossCard(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		panelBox(g);
		Hunt hunt = hunt();
		Tier tier = hunt.tier(questTier);
		HuntData.Progress progress = progress();
		boolean unlocked = tierOpen(tier);
		int cx = px + pw / 2;
		int y = py + 5;

		g.centeredText(this.font, colored(hunt.name() + " Hunt", hunt.color()), cx, y, WHITE);
		y += 11;
		g.centeredText(this.font, colored("<  ", TEXT_GRAY).append(colored("Tier " + tier.roman(), tier.color()))
				.append(colored("  >", TEXT_GRAY)), cx, y, WHITE);
		hits.add(new Hit(cx - 40, y - 2, 20, 12, () -> changeTier(-1)));
		hits.add(new Hit(cx + 20, y - 2, 20, 12, () -> changeTier(1)));
		y += 12;
		g.centeredText(this.font, colored(tier.bossName(), tier.color()), cx, y, WHITE);
		y += 10;
		g.centeredText(this.font, colored(tier.bossTitle(), TEXT_GRAY), cx, y, WHITE);
		y += 12;
		int statsTop = y;
		boolean affordable = progress.tokens >= tier.tokenCost();
		g.text(this.font, colored("Tokens: ", TEXT_GRAY).append(colored(String.valueOf(tier.tokenCost()), WHITE))
				.append(colored(" [" + progress.tokens + "]", affordable ? HuntDefs.LIME : HuntDefs.RED)), px + 6, y, WHITE, false);
		y += 10;
		g.text(this.font, colored("Experience: ", TEXT_GRAY).append(colored(tier.xp() + " XP", HuntDefs.XP_GREEN)),
				px + 6, y, WHITE, false);
		y += 12;

		if (!unlocked) {
			String lock = progress.rank() < tier.unlockRank()
					? "Unlocks at " + hunt.rankName(tier.unlockRank()) + " (Rank " + HuntDefs.ROMAN[tier.unlockRank()] + ")"
					: "Defeat " + hunt.tier(tier.number() - 1).bossName() + " (Tier " + HuntDefs.ROMAN[tier.number() - 1]
							+ ") to unlock";
			for (FormattedCharSequence line : this.font.split(colored(lock, HuntDefs.RED), pw - 90)) {
				g.text(this.font, line, px + 6, y, WHITE, false);
				y += 9;
			}
		} else {
			g.text(this.font, colored("Health: ", TEXT_GRAY).append(colored(String.valueOf(tier.health()), HuntDefs.RED)),
					px + 6, y, WHITE, false);
			y += 10;
			g.text(this.font, colored("Damage: ", TEXT_GRAY).append(colored(String.valueOf(tier.melee()), HuntDefs.GOLD)),
					px + 6, y, WHITE, false);
			y += 10;
			g.text(this.font, colored("Regeneration: ", TEXT_GRAY).append(colored(tier.regen() + "/s", HuntDefs.LIME)),
					px + 6, y, WHITE, false);
			y += 12;
			drawAbilities(g, tier, px + 4, y, pw - 8, py + ph - 26 - y);
		}
		drawBossModel(g, tier, px + pw - 84, statsTop - 4, px + pw - 6, statsTop + 54);

		int by = py + ph - 20;
		boolean defeated = progress.bossKills[tier.number() - 1] > 0;
		int startX = px + 4;
		int startW = pw - 8;
		if (defeated) {
			boolean hover = button(g, px + 4, by, 34, 16, Component.literal("Lore"), true, mouseX, mouseY, null);
			if (hover) {
				List<FormattedCharSequence> lines = new ArrayList<>();
				lines.add(colored(tier.bossName() + ", " + tier.bossTitle(), tier.color()).getVisualOrderText());
				lines.addAll(this.font.split(Component.translatable(tier.loreKey()).withColor(0xFFDDDDDD), 220));
				g.setTooltipForNextFrame(this.font, lines, mouseX, mouseY);
			}
			startX += 38;
			startW -= 38;
		}
		boolean canStart = unlocked && affordable && !ClientHuntState.peaceful;
		if (ClientHuntState.peaceful) {
			g.text(this.font, colored("Cannot start quest because of", HuntDefs.RED), px + 4, by - 20, WHITE, false);
			g.text(this.font, colored("the world's difficulty.", HuntDefs.RED), px + 4, by - 11, WHITE, false);
		}
		button(g, startX, by, startW, 16, Component.literal("Start [" + tier.tokenCost() + " Tokens]"), canStart,
				mouseX, mouseY, this::startQuest);
	}

	private void drawAbilities(GuiGraphicsExtractor g, Tier tier, int x, int y, int w, int h) {
		if (h < 20) {
			return;
		}
		sprite(g, "panel_dark", x, y, w, h);
		g.text(this.font, colored("Abilities", WHITE), x + 4, y + 3, WHITE, false);
		List<FormattedCharSequence> lines = new ArrayList<>();
		List<Integer> colors = new ArrayList<>();
		for (Ability ability : tier.abilities()) {
			lines.add(colored(ability.name(), HuntDefs.GOLD).getVisualOrderText());
			colors.add(WHITE);
			for (FormattedCharSequence line : this.font.split(colored(ability.description(), TEXT_GRAY), w - 10)) {
				lines.add(line);
				colors.add(WHITE);
			}
		}
		int lineH = 9;
		int viewTop = y + 14;
		int viewH = h - 16;
		int maxScroll = Math.max(0, lines.size() * lineH - viewH);
		abilityScroll = Math.max(0, Math.min(abilityScroll, maxScroll));
		g.enableScissor(x, viewTop, x + w, viewTop + viewH);
		for (int i = 0; i < lines.size(); i++) {
			int ly = viewTop + i * lineH - abilityScroll;
			g.text(this.font, lines.get(i), x + 4, ly, colors.get(i), false);
		}
		g.disableScissor();
		if (maxScroll > 0) {
			int barH = Math.max(8, viewH * viewH / (lines.size() * lineH));
			int barY = viewTop + (viewH - barH) * abilityScroll / maxScroll;
			g.fill(x + w - 3, barY, x + w - 1, barY + barH, 0xFF8A8A8A);
		}
	}

	private void drawActiveQuest(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		panelBox(g);
		HuntData.Quest quest = activeQuest();
		Hunt hunt = HuntDefs.hunt(quest.hunt);
		Tier tier = hunt.tier(quest.tier);
		int cx = px + pw / 2;
		int y = py + 5;
		g.centeredText(this.font, colored("Active Quest", WHITE), cx, y, WHITE);
		y += 13;
		g.text(this.font, colored(hunt.name() + " ", hunt.color()).append(colored("[Tier " + tier.roman() + "]", tier.color())),
				px + 6, y, WHITE, false);
		y += 11;
		g.text(this.font, colored("Boss: ", TEXT_GRAY).append(colored(tier.bossName(), tier.color())), px + 6, y, WHITE, false);
		y += 14;

		int barX = px + 6;
		int barW = pw - 12;
		float fill;
		Component label;
		if (quest.phase == HuntData.PHASE_KILLING) {
			fill = quest.quota == 0 ? 0 : quest.kills / (float) quest.quota;
			label = colored("Quota " + quest.kills + "/" + quest.quota, WHITE);
		} else if (quest.phase == HuntData.PHASE_SUMMONING) {
			fill = 1.0F;
			label = colored("The boss is being summoned...", WHITE);
		} else {
			fill = 1.0F;
			int seconds = ClientHuntState.bossTicksLeft / 20;
			label = colored(String.format("Boss alive - %d:%02d left", seconds / 60, seconds % 60), WHITE);
		}
		sprite(g, "bar_background", barX, y, barW, 12);
		int filled = Math.round(barW * fill);
		if (filled > 0) {
			sprite(g, "bar_quota", barX, y, filled, 12);
		}
		g.centeredText(this.font, label, cx, y + 2, WHITE);
		y += 15;
		if (ClientHuntState.peaceful) {
			for (FormattedCharSequence line : this.font.split(
					colored("Cannot progress further because of the world's difficulty.", HuntDefs.RED), pw - 12)) {
				g.text(this.font, line, px + 6, y, WHITE, false);
				y += 9;
			}
			y += 3;
		}
		g.text(this.font, colored("Huntable mobs:", TEXT_GRAY), px + 6, y, WHITE, false);
		y += 10;
		String huntable = ClientHuntState.huntable.isEmpty() ? "[None]" : String.join(", ", ClientHuntState.huntable);
		for (FormattedCharSequence line : this.font.split(colored(huntable, hunt.color()), pw - 12)) {
			g.text(this.font, line, px + 6, y, WHITE, false);
			y += 9;
		}

		int by = py + ph - 20;
		boolean hoverDrops = button(g, px + 4, by, 50, 16, Component.literal("Drops"), true, mouseX, mouseY, null);
		if (hoverDrops) {
			dropsHover = tier;
		}
		Component cancelLabel = cancelArmed ? colored("Confirm?", HuntDefs.RED) : Component.literal("Cancel");
		button(g, px + pw - 64, by, 60, 16, cancelLabel, true, mouseX, mouseY, () -> {
			if (cancelArmed) {
				cancelArmed = false;
				ClientPlayNetworking.send(new HuntPayloads.Action("cancel", hunt().id(), 0));
			} else {
				cancelArmed = true;
			}
		});
	}

	/** The boss, turning slowly, from a client-only copy of its entity. */
	private void drawBossModel(GuiGraphicsExtractor g, Tier tier, int x1, int y1, int x2, int y2) {
		if (this.minecraft.level == null) {
			return;
		}
		LivingEntity entity = previews.computeIfAbsent(tier.number(), n -> {
			LivingEntity created = tier.mob().create(this.minecraft.level, EntitySpawnReason.LOAD);
			if (created != null) {
				// A preview never joins the world; it only needs an id the renderer can read.
				created.setId(-1000 - n);
			}
			return created;
		});
		if (entity == null) {
			return;
		}
		double time = System.currentTimeMillis() / 1000.0;
		float lookX = (x1 + x2) / 2.0F + (float) Math.sin(time * 0.7) * 80.0F;
		float lookY = (y1 + y2) / 2.0F - 10.0F;
		int[] scales = {0, 34, 22, 19, 18, 26};
		InventoryScreen.extractEntityInInventoryFollowsMouse(g, x1, y1, x2, y2, scales[tier.number()], 0.0625F, lookX, lookY, entity);
	}

	/** Drops list with item icons, drawn on top of everything. */
	private void drawDropsPanel(GuiGraphicsExtractor g, Tier tier, int mouseX, int mouseY) {
		HuntData.Progress progress = progress();
		boolean concoction = ClientHuntState.data.concoction;
		List<ItemStack> icons = new ArrayList<>();
		List<Component> names = new ArrayList<>();
		for (Drop drop : HuntDefs.drops(tier)) {
			boolean seen = progress.dropsSeen.contains(drop.id());
			icons.add(seen ? new ItemStack(drop.item()) : ItemStack.EMPTY);
			String chance = drop.guaranteed() ? drop.minCount() + "-" + drop.maxCount() + " (100%)"
					: formatPercent(drop.chanceNum() * 100.0 / HuntDefs.odds(drop.chanceDen(), concoction));
			names.add(Component.empty().append(seen ? new ItemStack(drop.item()).getStyledHoverName() : colored("Unknown", TEXT_DIM))
					.append(colored("  " + chance, TEXT_GRAY)));
		}
		boolean seenStone = progress.dropsSeen.contains("soul_stone");
		icons.add(seenStone ? new ItemStack(ModItems.SOUL_STONE) : ItemStack.EMPTY);
		names.add(Component.empty().append(seenStone ? new ItemStack(ModItems.SOUL_STONE).getStyledHoverName()
				: colored("Unknown", TEXT_DIM)).append(colored("  1% (Spirit Lure animals)", TEXT_GRAY)));
		int width = 0;
		for (Component name : names) {
			width = Math.max(width, this.font.width(name));
		}
		int rowH = 14;
		int w = width + 30;
		int h = 16 + names.size() * rowH + (concoction ? 11 : 0);
		int x = Math.min(mouseX + 10, this.width - w - 4);
		int y = Math.max(4, mouseY - h - 4);
		g.nextStratum();
		sprite(g, "panel", x, y, w, h);
		g.text(this.font, colored("Drops", WHITE), x + 6, y + 5, WHITE, false);
		for (int i = 0; i < names.size(); i++) {
			int ry = y + 16 + i * rowH;
			if (icons.get(i).isEmpty()) {
				sprite(g, "panel_dark", x + 5, ry, 12, 12);
				g.text(this.font, colored("?", TEXT_GRAY), x + 9, ry + 2, WHITE, false);
			} else {
				g.pose().pushMatrix();
				g.pose().translate(x + 5, ry);
				g.pose().scale(0.75F, 0.75F);
				g.item(icons.get(i), 0, 0);
				g.pose().popMatrix();
			}
			g.text(this.font, names.get(i), x + 22, ry + 2, WHITE, false);
		}
		if (concoction) {
			g.text(this.font, colored("Mystery Concoction: odds improved", 0xFF55FF), x + 6, y + h - 11, WHITE, false);
		}
	}

	private List<Component> dropLines(Tier tier) {
		HuntData.Progress progress = progress();
		List<Component> lines = new ArrayList<>();
		lines.add(colored("Drops", WHITE));
		for (Drop drop : HuntDefs.drops(tier)) {
			boolean seen = progress.dropsSeen.contains(drop.id());
			Component name = seen ? new ItemStack(drop.item()).getStyledHoverName() : colored("Unknown", TEXT_DIM);
			String chance;
			if (drop.guaranteed()) {
				chance = drop.minCount() + "-" + drop.maxCount() + " (100%)";
			} else {
				chance = formatPercent(drop.chanceNum() * 100.0 / drop.chanceDen());
			}
			lines.add(Component.empty().append(name).append(colored("  " + chance, TEXT_GRAY)));
		}
		boolean seenStone = progress.dropsSeen.contains("soul_stone");
		Component stone = seenStone ? new ItemStack(ModItems.SOUL_STONE).getStyledHoverName() : colored("Unknown", TEXT_DIM);
		lines.add(Component.empty().append(stone).append(colored("  1% (Spirit Lure animals)", TEXT_GRAY)));
		return lines;
	}

	private static String formatPercent(double percent) {
		if (percent == Math.floor(percent)) {
			return (int) percent + "%";
		}
		return String.format("%.1f%%", percent);
	}

	// ---------------------------------------------------------------- Gear tab

	private static final String[] GEAR_PAGES = {"Armor", "Misc", "Weapons"};

	private void drawGear(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		panelBox(g);
		Hunt hunt = hunt();
		int cx = px + pw / 2;
		int y = py + 5;
		g.centeredText(this.font, colored(hunt.name() + " Gear", hunt.color()), cx, y, WHITE);
		y += 14;
		// Page switcher: "Armor  <  Misc  >  Weapons", wrapping around.
		String prev = GEAR_PAGES[(gearPage + 2) % 3];
		String next = GEAR_PAGES[(gearPage + 1) % 3];
		String current = GEAR_PAGES[gearPage];
		int curW = this.font.width(current);
		g.centeredText(this.font, colored(current, WHITE), cx, y, WHITE);
		g.text(this.font, colored("<", TEXT_GRAY), cx - curW / 2 - 12, y, WHITE, false);
		g.text(this.font, colored(">", TEXT_GRAY), cx + curW / 2 + 7, y, WHITE, false);
		g.text(this.font, colored(prev, TEXT_DIM), cx - curW / 2 - 18 - this.font.width(prev), y, WHITE, false);
		g.text(this.font, colored(next, TEXT_DIM), cx + curW / 2 + 18, y, WHITE, false);
		hits.add(new Hit(px, y - 2, cx - curW / 2 - 4 - px, 12, () -> changeGearPage(-1)));
		hits.add(new Hit(cx + curW / 2 + 4, y - 2, px + pw - (cx + curW / 2 + 4), 12, () -> changeGearPage(1)));
		y += 15;

		tokenButton = null;
		switch (gearPage) {
			case 1 -> drawGearMisc(g, hunt, y, mouseX, mouseY);
			case 0 -> drawGearTrades(g, HuntDefs.ANIMAL_ARMOR, y, mouseX, mouseY);
			default -> drawGearTrades(g, HuntDefs.ANIMAL_WEAPONS, y, mouseX, mouseY);
		}
	}

	private void drawGearMisc(GuiGraphicsExtractor g, Hunt hunt, int y, int mouseX, int mouseY) {
		int cx = px + pw / 2;
		sprite(g, "panel_dark", px + 4, y, pw - 8, 52);
		g.text(this.font, colored(hunt.name() + " Ritual Tokens", WHITE), px + 8, y + 4, WHITE, false);
		g.text(this.font, colored("Balance: ", TEXT_GRAY).append(colored(String.valueOf(progress().tokens), HuntDefs.LIME)),
				px + 8, y + 15, WHITE, false);
		tokenButton = new int[] {px + 8, y + 28, pw - 16, 16};
		boolean hover = button(g, px + 8, y + 28, pw - 16, 16, Component.literal("Buy 64 Tokens [10 Logs]"), true, mouseX,
				mouseY, null);
		if (hover) {
			g.setComponentTooltipForNextFrame(this.font, List.of(colored("Click: 10 Logs -> 64 Tokens", WHITE),
					colored("Any Overworld log works.", TEXT_GRAY),
					colored("Shift-click: 100 Oak Logs -> 640 Tokens", TEXT_GRAY)), mouseX, mouseY);
		}
		y += 58;
		sprite(g, "panel_dark", px + 4, y, pw - 8, 44);
		g.centeredText(this.font, colored("Questline - Spiritual Talk", WHITE), cx, y + 6, WHITE);
		int nextRank = HuntDefs.QUESTLINE_RANKS[0];
		boolean unlocked = progress().rank() >= nextRank;
		Component label = Component.literal("[Click] - Rank " + HuntDefs.ROMAN[nextRank] + " Required");
		boolean hoverTalk = button(g, px + 8, y + 20, pw - 16, 16, label, false, mouseX, mouseY, null);
		if (hoverTalk) {
			g.setComponentTooltipForNextFrame(this.font, List.of(
					colored(unlocked ? "Koro's questline arrives in a later version." : "Unlocks at " + hunt.rankName(nextRank)
							+ " (Rank " + HuntDefs.ROMAN[nextRank] + ")", TEXT_GRAY)), mouseX, mouseY);
		}
	}

	private void drawGearTrades(GuiGraphicsExtractor g, List<HuntDefs.GearTrade> trades, int y, int mouseX, int mouseY) {
		int listX = px + 4;
		int listW = pw - 14;
		int listTop = y;
		int listH = py + ph - 4 - y;
		int rank = progress().rank();
		int total = 0;
		for (HuntDefs.GearTrade trade : trades) {
			total += entryHeight(trade.cost().size()) + 4;
		}
		int maxScroll = Math.max(0, total - 4 - listH);
		gearScroll = Math.max(0, Math.min(gearScroll, maxScroll));
		g.enableScissor(listX, listTop, listX + listW, listTop + listH);
		int ey = listTop - gearScroll;
		for (int t = 0; t < trades.size(); t++) {
			HuntDefs.GearTrade trade = trades.get(t);
			int value = gearPage * 100 + t;
			boolean affordable = true;
			for (HuntDefs.Cost cost : trade.cost()) {
				affordable &= countInInventory(cost) >= cost.count();
			}
			int h = entryHeight(trade.cost().size());
			sprite(g, "panel_dark", listX, ey, listW, h);
			boolean unlocked = rank >= trade.rank();
			g.text(this.font, colored(trade.name(), unlocked ? HuntDefs.GOLD : 0x8A8A8A), listX + 5, ey + 4, WHITE, false);
			int ly = ey + 15;
			for (HuntDefs.Cost cost : trade.cost()) {
				boolean have = countInInventory(cost) >= cost.count();
				String name = new ItemStack(cost.item()).getHoverName().getString();
				g.pose().pushMatrix();
				g.pose().translate(listX + 5, ly);
				g.pose().scale(0.625F, 0.625F);
				g.item(new ItemStack(cost.item()), 0, 0);
				g.pose().popMatrix();
				g.text(this.font, colored(name + " x" + cost.count(), have ? 0xFFFFFF : HuntDefs.RED), listX + 18, ly + 1, WHITE, false);
				ly += 11;
			}
			Component label = unlocked ? Component.literal("[Click] - Buy") : Component.literal("Rank Required: [?]");
			if (ey + h > listTop && ey < listTop + listH) {
				boolean inside = mouseY >= listTop && mouseY < listTop + listH;
				button(g, listX + 4, ly + 2, listW - 8, 14, label, unlocked && affordable, mouseX, mouseY,
						inside ? () -> ClientPlayNetworking.send(new HuntPayloads.Action("buy_gear", hunt().id(), value)) : null);
			}
			ey += h + 4;
		}
		g.disableScissor();

		// Scroll bar: the track shows the whole list, the handle the part on screen.
		int trackX = px + pw - 8;
		g.fill(trackX, listTop, trackX + 4, listTop + listH, 0xFF1A1A1A);
		if (maxScroll > 0) {
			int handleH = Math.max(12, listH * listH / (listH + maxScroll));
			int handleY = listTop + (listH - handleH) * gearScroll / maxScroll;
			g.fill(trackX, handleY, trackX + 4, handleY + handleH, 0xFFC6C6C6);
			g.fill(trackX + 3, handleY, trackX + 4, handleY + handleH, 0xFF8B8B8B);
		} else {
			g.fill(trackX, listTop, trackX + 4, listTop + listH, 0xFF555555);
		}
	}

	/** How many of a cost item the player is carrying. */
	private int countInInventory(HuntDefs.Cost cost) {
		if (this.minecraft.player == null) {
			return 0;
		}
		net.minecraft.world.entity.player.Inventory inventory = this.minecraft.player.getInventory();
		int count = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(cost.item())) {
				count += stack.getCount();
			}
		}
		return count;
	}

	private static int entryHeight(int costLines) {
		return 15 + costLines * 11 + 20;
	}

	private void changeGearPage(int direction) {
		gearPage = (gearPage + direction + 3) % 3;
		gearScroll = 0;
	}

	// ---------------------------------------------------------------- Ranks tab

	private void drawRanks(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		Hunt hunt = hunt();
		HuntData.Progress progress = progress();
		int rank = progress.rank();
		int maxView = Math.min(rank + 1, HuntDefs.MAX_RANK);
		rankView = Math.max(0, Math.min(rankView, maxView));
		boolean snapshot = rankView > rank;
		int cx = px + pw / 2;

		// Header box: "Hunt Type - Rank - RN".
		int headerH = 24;
		sprite(g, "panel", px, py, pw, headerH);
		Component header = colored(hunt.name(), hunt.color())
				.append(colored(" - " + HuntDefs.RANK_TITLES[rankView] + " - " + HuntDefs.ROMAN[rankView],
						snapshot ? TEXT_GRAY : WHITE));
		g.centeredText(this.font, header, cx, py + 8, WHITE);
		if (rankView > 0) {
			g.text(this.font, colored("<", TEXT_GRAY), px + 4, py + 8, WHITE, false);
			hits.add(new Hit(px, py, 14, headerH, () -> changeRank(-1)));
		}
		if (rankView < maxView) {
			g.text(this.font, colored(">", TEXT_GRAY), px + pw - 9, py + 8, WHITE, false);
			hits.add(new Hit(px + pw - 14, py, 14, headerH, () -> changeRank(1)));
		}

		// Unlock list.
		int listY = py + headerH + 4;
		int listH = ph - headerH - 4 - 20;
		sprite(g, "panel", px, listY, pw, listH);
		int y = listY + 5;
		if (snapshot) {
			g.text(this.font, colored("Next Rank: " + HuntDefs.RANK_XP[rankView] + " XP", HuntDefs.GOLD), px + 6, y, WHITE, false);
			y += 12;
		}
		for (Unlock unlock : hunt.unlocks().get(rankView)) {
			int color = snapshot ? 0x8A8A8A : unlock.color();
			for (FormattedCharSequence line : this.font.split(colored("* " + unlock.text(), color), pw - 12)) {
				g.text(this.font, line, px + 6, y, WHITE, false);
				y += 9;
			}
			y += 2;
		}
		if (!snapshot && rankView == rank && progress.bossKills[4] > 0) {
			y += 4;
			g.text(this.font, colored("Boss kills", WHITE), px + 6, y, WHITE, false);
			y += 10;
			StringBuilder kills = new StringBuilder();
			for (int i = 0; i < 5; i++) {
				kills.append(HuntDefs.ROMAN[i + 1]).append(": ").append(progress.bossKills[i]).append(i < 4 ? "  " : "");
			}
			g.text(this.font, colored(kills.toString(), TEXT_GRAY), px + 6, y, WHITE, false);
			y += 11;
			g.text(this.font, colored("Drops received", WHITE), px + 6, y, WHITE, false);
			y += 10;
			for (Drop drop : HuntDefs.drops(hunt.tier(5))) {
				int count = progress.dropCounts.getOrDefault(drop.id(), 0);
				g.text(this.font, Component.empty().append(new ItemStack(drop.item()).getStyledHoverName())
						.append(colored(": " + count, TEXT_GRAY)), px + 6, y, WHITE, false);
				y += 9;
			}
		}

		// XP bar.
		int barY = py + ph - 14;
		int next = rank >= HuntDefs.MAX_RANK ? HuntDefs.RANK_XP[HuntDefs.MAX_RANK] : HuntDefs.RANK_XP[rank + 1];
		int prev = HuntDefs.RANK_XP[rank];
		float fill = rank >= HuntDefs.MAX_RANK ? 1.0F : (progress.xp - prev) / (float) Math.max(1, next - prev);
		sprite(g, "bar_background", px, barY, pw, 12);
		int xpFilled = Math.round(pw * Math.max(0, Math.min(1, fill)));
		if (xpFilled > 0) {
			sprite(g, "bar_xp", px, barY, xpFilled, 12);
		}
		String xpText = rank >= HuntDefs.MAX_RANK ? progress.xp + " XP (Max Rank)" : progress.xp + " / " + next + " XP";
		g.centeredText(this.font, colored(xpText, WHITE), cx, barY + 2, WHITE);
	}

	// ---------------------------------------------------------------- input

	private void switchTab(int newTab) {
		if (newTab != tab) {
			tab = newTab;
			panelOpen = false;
			cancelArmed = false;
		}
	}

	private void openPanel() {
		panelOpen = true;
		markHintSeen();
		abilityScroll = 0;
		rankView = progress().rank();
		questTier = Math.min(questTier, hunt().tiers().size());
		gearPage = 1;
		gearScroll = 0;
	}

	private void changeHunt(int direction) {
		int size = HuntDefs.HUNTS.size();
		int next = Math.max(0, Math.min(size - 1, huntIndex + direction));
		if (next != huntIndex) {
			huntIndex = next;
			rankView = progress().rank();
			questTier = highestUnlockedTier();
			ClientPlayNetworking.send(new HuntPayloads.Action("select", hunt().id(), 0));
		}
	}

	private void changeTier(int direction) {
		questTier = Math.max(1, Math.min(hunt().tiers().size(), questTier + direction));
		abilityScroll = 0;
	}

	private void changeRank(int direction) {
		rankView += direction;
	}

	private void startQuest() {
		ClientPlayNetworking.send(new HuntPayloads.Action("start", hunt().id(), questTier));
	}

	private boolean questPanelShowing() {
		return tab == 0 && activeQuest() != null;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_BACKSPACE) {
			if (panelOpen && !questPanelShowing()) {
				panelOpen = false;
				return true;
			}
			if (key == InputConstants.KEY_ESCAPE) {
				this.onClose();
			}
			return true;
		}
		if (key == InputConstants.KEY_W || key == InputConstants.KEY_UP) {
			changeHunt(-1);
			return true;
		}
		if (key == InputConstants.KEY_S || key == InputConstants.KEY_DOWN) {
			changeHunt(1);
			return true;
		}
		if (key == InputConstants.KEY_A || key == InputConstants.KEY_LEFT) {
			if (panelOpen && tab == 0 && !questPanelShowing()) {
				changeTier(-1);
			} else if (panelOpen && tab == 2) {
				changeRank(-1);
			} else if (panelOpen && tab == 1) {
				changeGearPage(-1);
			}
			return true;
		}
		if (key == InputConstants.KEY_D || key == InputConstants.KEY_RIGHT) {
			if (panelOpen && tab == 0 && !questPanelShowing()) {
				changeTier(1);
			} else if (panelOpen && tab == 2) {
				changeRank(1);
			} else if (panelOpen && tab == 1) {
				changeGearPage(1);
			}
			return true;
		}
		if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
			if (!panelOpen && !questPanelShowing()) {
				openPanel();
			} else if (tab == 0 && !questPanelShowing()) {
				Tier tier = hunt().tier(questTier);
				HuntData.Progress progress = progress();
				if (tierOpen(tier) && progress.tokens >= tier.tokenCost() && !ClientHuntState.peaceful) {
					startQuest();
				}
			}
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			double mx = event.x();
			double my = event.y();
			boolean shift = (event.modifiers() & InputConstants.MOD_SHIFT) != 0;
			// The token trade reads Shift, so it is handled here.
			int[] tb = tokenButton;
			if (tab == 1 && panelOpen && tb != null && mx >= tb[0] && mx < tb[0] + tb[2] && my >= tb[1] && my < tb[1] + tb[3]) {
				ClientPlayNetworking.send(new HuntPayloads.Action("buy_tokens", hunt().id(), shift ? 1 : 0));
				return true;
			}
			for (Hit hit : List.copyOf(hits)) {
				if (hit.contains(mx, my)) {
					boolean wasArmed = cancelArmed;
					hit.action().run();
					if (wasArmed && cancelArmed) {
						cancelArmed = false;
					}
					return true;
				}
			}
			cancelArmed = false;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (panelOpen && tab == 0 && !questPanelShowing() && mouseX >= px && mouseX < px + pw) {
			abilityScroll -= (int) Math.round(scrollY * 9);
			return true;
		}
		if (panelOpen && tab == 1 && mouseX >= px && mouseX < px + pw) {
			gearScroll -= (int) Math.round(scrollY * 12);
			return true;
		}
		if (mouseX < px) {
			changeHunt(scrollY > 0 ? -1 : 1);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}
}
