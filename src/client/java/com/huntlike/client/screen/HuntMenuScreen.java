package com.huntlike.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
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

	private static final int W = 300;
	private static final int MAX_H = 236;
	private static final int TAB_H = 20;

	private static final int FRAME = 0xFF2B2B2B;
	private static final int TAB = 0xFF353535;
	private static final int TAB_SELECTED = 0xFF4A4A4A;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int BLACK = 0xFF000000;
	private static final int PANEL = 0xFF3C3C3C;
	private static final int PANEL_DARK = 0xFF2E2E2E;
	private static final int BUTTON = 0xFF555555;
	private static final int BUTTON_HOVER = 0xFF6A6A6A;
	private static final int BUTTON_OFF = 0xFF3A3A3A;
	private static final int TEXT_GRAY = 0xFFAAAAAA;
	private static final int TEXT_DIM = 0xFF6F6F6F;
	private static final int BAR = 0xFFAA0000;

	private int tab;
	/** False: the Hunt rail only. True: the selected Hunt's panel is open. */
	private boolean panelOpen;
	private int huntIndex;
	private int questTier = 1;
	private int rankView;
	private int abilityScroll;
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
			if (rank >= tier.unlockRank()) {
				best = tier.number();
			}
		}
		return best;
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

		drawTabs(g, mouseX, mouseY);
		g.fill(left, top, left + W, top + H, FRAME);
		g.fill(ix - 2, iy - 2, ix + iw + 2, iy + ih + 2, WHITE);
		g.fill(ix, iy, ix + iw, iy + ih, BLACK);
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
		} else {
			g.text(this.font, Component.literal("Click a symbol or press Enter"), px + 8, iy + ih - 14, TEXT_DIM, false);
		}
	}

	private void drawTabs(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int tabW = W / 3;
		for (int i = 0; i < 3; i++) {
			int x = left + i * tabW;
			int y = top - TAB_H;
			boolean selected = i == tab;
			g.fill(x + 1, y, x + tabW - 1, top, selected ? TAB_SELECTED : TAB);
			g.centeredText(this.font, Component.literal(TABS[i]), x + tabW / 2, y + 6, selected ? WHITE : TEXT_GRAY);
			int index = i;
			hits.add(new Hit(x, y, tabW, TAB_H, () -> switchTab(index)));
		}
	}

	private void drawRail(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		int cx = ix + 46;
		int cy = iy + ih / 2;
		// Slanted rail: leans right toward the selected symbol, then back.
		for (int y = iy; y < iy + ih; y++) {
			double t = Math.abs(y - cy) / (double) (ih / 2);
			int x = (int) Math.round(cx - 12 * t);
			g.fill(x - 1, y, x + 1, y + 1, WHITE);
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
			g.fill(x0 - 1, y0 - 1, x0 + size + 1, y0 + size + 1, hover ? 0xFF8A8A8A : 0xFF6E6E6E);
			g.fill(x0, y0, x0 + size, y0 + size, 0xFF555555);
			g.blit(RenderPipelines.GUI_TEXTURED, ANKH, x0 + 3, y0 + 3, 0.0F, 0.0F, size - 6, size - 6, 128, 128, 128, 128);
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
		g.fill(px, py, px + pw, py + ph, PANEL);
	}

	/** Draws a button, registers its click and returns whether the mouse is over it. */
	private boolean button(GuiGraphicsExtractor g, int x, int y, int w, int h, Component label, boolean enabled,
			int mouseX, int mouseY, Runnable action) {
		boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
		g.fill(x, y, x + w, y + h, !enabled ? BUTTON_OFF : hover ? BUTTON_HOVER : BUTTON);
		g.centeredText(this.font, label, x + w / 2, y + (h - 8) / 2, enabled ? WHITE : TEXT_DIM);
		if (enabled && action != null) {
			hits.add(new Hit(x, y, w, h, action));
		}
		return hover;
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
		boolean unlocked = progress.rank() >= tier.unlockRank();
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
		boolean affordable = progress.tokens >= tier.tokenCost();
		g.text(this.font, colored("Tokens: ", TEXT_GRAY).append(colored(String.valueOf(tier.tokenCost()), WHITE))
				.append(colored(" [" + progress.tokens + "]", affordable ? HuntDefs.LIME : HuntDefs.RED)), px + 6, y, WHITE, false);
		y += 12;

		if (!unlocked) {
			g.text(this.font, colored("Unlocks at " + hunt.rankName(tier.unlockRank()) + " (Rank "
					+ HuntDefs.ROMAN[tier.unlockRank()] + ")", HuntDefs.RED), px + 6, y, WHITE, false);
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
		g.fill(x, y, x + w, y + h, PANEL_DARK);
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
		g.fill(barX, y, barX + barW, y + 12, 0xFF1E1E1E);
		g.fill(barX, y, barX + Math.round(barW * fill), y + 12, BAR);
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
			g.setComponentTooltipForNextFrame(this.font, dropLines(tier), mouseX, mouseY);
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

	private void drawGear(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		panelBox(g);
		Hunt hunt = hunt();
		int cx = px + pw / 2;
		int y = py + 5;
		g.centeredText(this.font, colored(hunt.name() + " Gear", hunt.color()), cx, y, WHITE);
		y += 16;
		g.fill(px + 4, y - 3, px + pw - 4, y + 52, PANEL_DARK);
		g.text(this.font, colored(hunt.name() + " Ritual Tokens", WHITE), px + 8, y, WHITE, false);
		y += 11;
		g.text(this.font, colored("Balance: ", TEXT_GRAY).append(colored(String.valueOf(progress().tokens), HuntDefs.LIME)),
				px + 8, y, WHITE, false);
		y += 12;
		boolean hover = button(g, px + 8, y, pw - 16, 16, Component.literal("Buy 64 Tokens [10 Oak Logs]"), true, mouseX, mouseY,
				null);
		if (hover) {
			g.setComponentTooltipForNextFrame(this.font, List.of(colored("Click: 10 Oak Logs -> 64 Tokens", WHITE),
					colored("Shift-click: 100 Oak Logs -> 640 Tokens", TEXT_GRAY)), mouseX, mouseY);
		}
		y += 30;
		for (FormattedCharSequence line : this.font.split(
				colored("Hunt Weapons, Hunt Armor and Koro arrive in a later version.", TEXT_DIM), pw - 12)) {
			g.text(this.font, line, px + 6, y, WHITE, false);
			y += 9;
		}
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
		g.fill(px, py, px + pw, py + headerH, PANEL);
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
		g.fill(px, listY, px + pw, listY + listH, PANEL);
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
		g.fill(px, barY, px + pw, barY + 12, 0xFF1E1E1E);
		g.fill(px, barY, px + Math.round(pw * Math.max(0, Math.min(1, fill))), barY + 12, 0xFF2E8B2E);
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
		abilityScroll = 0;
		rankView = progress().rank();
		questTier = Math.min(questTier, hunt().tiers().size());
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
			}
			return true;
		}
		if (key == InputConstants.KEY_D || key == InputConstants.KEY_RIGHT) {
			if (panelOpen && tab == 0 && !questPanelShowing()) {
				changeTier(1);
			} else if (panelOpen && tab == 2) {
				changeRank(1);
			}
			return true;
		}
		if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
			if (!panelOpen && !questPanelShowing()) {
				openPanel();
			} else if (tab == 0 && !questPanelShowing()) {
				Tier tier = hunt().tier(questTier);
				HuntData.Progress progress = progress();
				if (progress.rank() >= tier.unlockRank() && progress.tokens >= tier.tokenCost() && !ClientHuntState.peaceful) {
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
			if (tab == 1 && panelOpen && mx >= px + 8 && mx < px + pw - 8 && my >= py + 44 && my < py + 60) {
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
		if (mouseX < px) {
			changeHunt(scrollY > 0 ? -1 : 1);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}
}
