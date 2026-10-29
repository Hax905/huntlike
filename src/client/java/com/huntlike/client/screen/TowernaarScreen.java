package com.huntlike.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.huntlike.HuntLike;
import com.huntlike.client.ClientHuntState;
import com.huntlike.entity.ModEntities;
import com.huntlike.hunt.HuntData;
import com.huntlike.hunt.HuntDefs;
import com.huntlike.hunt.HuntDefs.Cost;
import com.huntlike.network.HuntPayloads;
import com.huntlike.towernaar.TowernaarScript;
import com.huntlike.towernaar.TowernaarScript.Chapter;
import com.huntlike.towernaar.TowernaarScript.Kind;
import com.huntlike.towernaar.TowernaarScript.Step;
import com.huntlike.towernaar.TowernaarUndead;

/**
 * Towernaar's dialogue window (Cluster questline), laid out like Koro's. In the Final Chapter the chat becomes a boss
 * panel: Towernaar the Undead's model, his stats, and a free Start button.
 */
public class TowernaarScreen extends Screen {
	private static final Identifier PORTRAIT = HuntLike.id("textures/gui/towernaar.png");
	private static final Identifier PORTRAIT_FINAL = HuntLike.id("textures/gui/towernaar_final.png");
	private static final int W = 360;
	private static final int H = 220;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFFAAAAAA;
	private static final int LIGHT_BLUE = 0x8FD3FF;
	private static final int YELLOW = 0xFFFF55;
	/** Towernaar's name color. */
	public static final int PURPLE = 0xAA00AA;

	private final Screen parent;
	private int left;
	private int top;
	private int portraitX;
	private int portraitY;
	private int chatScroll;
	private int lastLineCount = -1;
	private boolean stickToBottom = true;
	private long bobUntil;
	private int[] startButton;
	/** The PRD trade's button (after the Final Chapter, until the PRD is used), or null. */
	private int[] prdButton;
	private LivingEntity preview;

	public TowernaarScreen(Screen parent) {
		super(Component.literal("Towernaar"));
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

	private static MutableComponent c(String text, int rgb) {
		return Component.literal(text).withColor(rgb);
	}

	/** In the Final Chapter: chapters I-III done and Rank X reached (or the fight is won). */
	private static boolean finalChapter(HuntData data) {
		return data.towernaarDone > TowernaarScript.FINAL
				|| data.towernaarDone == TowernaarScript.FINAL && TowernaarScript.canStart(TowernaarScript.FINAL, data);
	}

	// ---------------------------------------------------------------- the conversation

	private static int shownChapter(HuntData data) {
		if (data.towernaarStep > 0) {
			return data.towernaarDone;
		}
		return Math.min(data.towernaarDone, TowernaarScript.FINAL) - 1;
	}

	private MutableComponent speech(String text) {
		String name = this.minecraft.player == null ? "adventurer" : this.minecraft.player.getName().getString();
		text = text.replace("%player%", name);
		MutableComponent out = Component.empty();
		String[] parts = text.split("_", -1);
		for (int i = 0; i < parts.length; i++) {
			MutableComponent part = c(parts[i], 0xFFFFFF);
			if (i % 2 == 1) {
				part = part.withStyle(style -> style.withItalic(true));
			}
			out.append(part);
		}
		return out;
	}

	private static Component itemName(Item item) {
		return c(new ItemStack(item).getHoverName().getString(), LIGHT_BLUE);
	}

	private List<Component> conversation(HuntData data) {
		List<Component> lines = new ArrayList<>();
		int index = shownChapter(data);
		if (index >= 0) {
			Chapter chapter = TowernaarScript.CHAPTERS.get(index);
			int shown = data.towernaarStep > 0 && index == data.towernaarDone ? data.towernaarStep : chapter.steps().size();
			for (int i = 0; i < shown; i++) {
				Step step = chapter.steps().get(i);
				switch (step.kind()) {
					case SAY -> lines.add(Component.empty().append(c(step.speaker(), PURPLE)).append(c(": ", 0xFFFFFF))
							.append(speech(step.text())));
					case QUEST -> {
						MutableComponent line = Component.empty().append(c("[Quest Started]", YELLOW))
								.append(c(" - " + step.text() + " Requirements: ", 0xFFFFFF));
						for (int r = 0; r < step.items().size(); r++) {
							Cost cost = step.items().get(r);
							line.append(itemName(cost.item())).append(c(" [x" + cost.count() + "]", LIGHT_BLUE))
									.append(c(r < step.items().size() - 1 ? ", " : ".", 0xFFFFFF));
						}
						lines.add(line);
					}
					case KILLS -> lines.add(Component.empty().append(c("[Quest Started]", YELLOW))
							.append(c(" - " + step.text() + " Requirements: ", 0xFFFFFF)).append(c("Rogue Hunters", HuntDefs.RED))
							.append(c(" [x" + step.kills() + "].", LIGHT_BLUE)));
					case COMPLETE -> lines.add(Component.empty().append(c("[Quest Completed!]", HuntDefs.DARK_GREEN))
							.append(c(" - " + chapter.title(), HuntDefs.LIME)));
					default -> {
					}
				}
			}
		}
		if (data.towernaarStep == 0 && data.towernaarDone < TowernaarScript.FINAL) {
			if (TowernaarScript.canStart(data.towernaarDone, data)) {
				lines.add(c(data.towernaarDone == 0 ? "Click on the Enderian" : "Click on " + TowernaarScript.name(data), GRAY));
			} else {
				int rank = TowernaarScript.CHAPTERS.get(data.towernaarDone).rank();
				lines.add(c("Reach Cluster Rank " + HuntDefs.ROMAN[rank] + " for the next chapter.", GRAY));
			}
		}
		if (data.towernaarDone == TowernaarScript.FINAL && !TowernaarScript.canStart(TowernaarScript.FINAL, data)) {
			lines.add(c("Reach Cluster Rank " + HuntDefs.ROMAN[TowernaarScript.CHAPTERS.get(TowernaarScript.FINAL).rank()]
					+ " for the Final Chapter.", GRAY));
		}
		return lines;
	}

	private static Step openStep(HuntData data) {
		if (data.towernaarStep == 0 || data.towernaarDone >= TowernaarScript.FINAL) {
			return null;
		}
		Step step = TowernaarScript.CHAPTERS.get(data.towernaarDone).steps().get(data.towernaarStep - 1);
		return step.kind() == Kind.QUEST || step.kind() == Kind.KILLS ? step : null;
	}

	private int carried(Item item) {
		if (this.minecraft.player == null) {
			return 0;
		}
		var inventory = this.minecraft.player.getInventory();
		int count = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).is(item)) {
				count += inventory.getItem(i).getCount();
			}
		}
		return count;
	}

	// ---------------------------------------------------------------- drawing

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		HuntData data = ClientHuntState.data;
		boolean fin = finalChapter(data);
		left = (this.width - W) / 2;
		top = (this.height - H) / 2;
		startButton = null;
		prdButton = null;
		sprite(g, "window", left, top, W, H);
		g.text(this.font, c("Questline - " + TowernaarScript.name(data), 0x404040), left + 10, top + 7, 0xFF404040, false);
		sprite(g, "content", left + 6, top + 18, W - 12, H - 24);
		sprite(g, "portal", left + 8, top + 20, W - 16, H - 28);

		// Left column: portrait, name, Quest Items.
		int colX = left + 14;
		int colW = 104;
		portraitX = colX + (colW - 56) / 2;
		portraitY = top + 26;
		boolean hover = !fin && mouseX >= portraitX && mouseX < portraitX + 56 && mouseY >= portraitY && mouseY < portraitY + 56;
		sprite(g, hover ? "symbol_hover" : "symbol", portraitX, portraitY, 56, 56);
		long now = System.currentTimeMillis();
		int bob = now < bobUntil ? -1 : 0;
		boolean red = data.towernaarDone == TowernaarScript.FINAL && fin;
		g.blit(RenderPipelines.GUI_TEXTURED, red ? PORTRAIT_FINAL : PORTRAIT, portraitX + 4, portraitY + 4 + bob, 0.0F, 0.0F, 48, 48, 48,
				48, 48, 48);
		g.centeredText(this.font, c(TowernaarScript.name(data), PURPLE), colX + colW / 2, portraitY + 60, WHITE);

		int y = portraitY + 74;
		int boxBottom = top + H - 14;
		sprite(g, "panel_dark", colX, y, colW, boxBottom - y);
		boolean prdTrade = com.huntlike.towernaar.TowernaarManager.prdTradeOpen(data);
		g.text(this.font, prdTrade ? c("Magnetic Wand PRD", HuntDefs.GOLD) : c("Quest Items", 0xFFFFFF), colX + 4, y + 4, WHITE, false);
		y += 15;
		Step open = openStep(data);
		if (prdTrade) {
			// Lost the PRD? Towernaar draws another one: 1 Paper, 1 Ink Sac and 1 Ender Pearl.
			boolean all = true;
			for (Cost cost : com.huntlike.towernaar.TowernaarManager.PRD_COST) {
				boolean have = carried(cost.item()) >= cost.count();
				all &= have;
				MutableComponent name = Component.empty().append(itemName(cost.item())).append(c(" [x" + cost.count() + "]", LIGHT_BLUE));
				if (!have) {
					name = Component.empty().append(name).append(c(" ✖", HuntDefs.RED));
				}
				for (FormattedCharSequence l : this.font.split(name, colW - 8)) {
					g.text(this.font, l, colX + 4, y, WHITE, false);
					y += 9;
				}
				y += 1;
			}
			int by = boxBottom - 20;
			boolean tradeHover = mouseX >= colX + 4 && mouseX < colX + colW - 4 && mouseY >= by && mouseY < by + 16;
			g.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace(!all ? "widget/button_disabled"
					: tradeHover ? "widget/button_highlighted" : "widget/button"), colX + 4, by, colW - 8, 16);
			g.centeredText(this.font, c("Trade", all ? 0xFFFFFF : 0xA0A0A0), colX + colW / 2, by + 4, WHITE);
			if (all) {
				prdButton = new int[] {colX + 4, by, colW - 8, 16};
			}
		} else if (open == null) {
			g.text(this.font, c("None", GRAY), colX + 4, y, WHITE, false);
			y += 10;
		} else if (open.kind() == Kind.KILLS) {
			int done = Math.min(open.kills(), data.rogueKills - data.towernaarKillBase);
			MutableComponent line = Component.empty().append(c("Rogue Hunters", HuntDefs.RED))
					.append(c(" [" + done + "/" + open.kills() + "]", LIGHT_BLUE));
			if (done < open.kills()) {
				line.append(c(" ✖", HuntDefs.RED));
			}
			for (FormattedCharSequence l : this.font.split(line, colW - 8)) {
				g.text(this.font, l, colX + 4, y, WHITE, false);
				y += 9;
			}
		} else {
			for (Cost cost : open.items()) {
				boolean have = carried(cost.item()) >= cost.count();
				MutableComponent name = Component.empty().append(itemName(cost.item())).append(c(" [x" + cost.count() + "]", LIGHT_BLUE));
				if (!have) {
					name = Component.empty().append(name).append(c(" ✖", HuntDefs.RED));
				}
				for (FormattedCharSequence l : this.font.split(name, colW - 8)) {
					g.text(this.font, l, colX + 4, y, WHITE, false);
					y += 9;
				}
				y += 1;
			}
		}
		if (data.towernaarStep > 0 && data.towernaarDone < TowernaarScript.FINAL) {
			Item reward = TowernaarScript.CHAPTERS.get(data.towernaarDone).reward();
			y += 4;
			g.text(this.font, c("Crafting:", GRAY), colX + 4, y, WHITE, false);
			y += 10;
			for (FormattedCharSequence l : this.font.split(c(new ItemStack(reward).getHoverName().getString(), HuntDefs.GOLD), colW - 8)) {
				g.text(this.font, l, colX + 4, y, WHITE, false);
				y += 9;
			}
		}

		int chatX = colX + colW + 8;
		int chatY = top + 26;
		int chatW = left + W - 14 - chatX;
		int chatH = top + H - 14 - chatY;
		if (fin) {
			drawBossPanel(g, data, chatX, chatY, chatW, chatH, mouseX, mouseY);
		} else {
			drawChat(g, data, chatX, chatY, chatW, chatH, now);
		}
		if (hover) {
			g.setTooltipForNextFrame(this.font, c("Click on " + TowernaarScript.name(data), GRAY), mouseX, mouseY);
		}
	}

	private void drawChat(GuiGraphicsExtractor g, HuntData data, int chatX, int chatY, int chatW, int chatH, long now) {
		sprite(g, "panel", chatX, chatY, chatW, chatH);
		List<FormattedCharSequence> wrapped = new ArrayList<>();
		for (Component line : conversation(data)) {
			wrapped.addAll(this.font.split(line, chatW - 14));
			wrapped.add(FormattedCharSequence.EMPTY);
		}
		int lineH = 9;
		int viewH = chatH - 8;
		int maxScroll = Math.max(0, wrapped.size() * lineH - viewH);
		if (wrapped.size() != lastLineCount) {
			if (lastLineCount >= 0) {
				bobUntil = now + 180;
			}
			lastLineCount = wrapped.size();
			if (stickToBottom) {
				chatScroll = maxScroll;
			}
		}
		chatScroll = Math.max(0, Math.min(chatScroll, maxScroll));
		g.enableScissor(chatX + 2, chatY + 4, chatX + chatW - 6, chatY + 4 + viewH);
		for (int i = 0; i < wrapped.size(); i++) {
			g.text(this.font, wrapped.get(i), chatX + 5, chatY + 4 + i * lineH - chatScroll, WHITE, false);
		}
		g.disableScissor();
		int trackX = chatX + chatW - 6;
		g.fill(trackX, chatY + 4, trackX + 4, chatY + 4 + viewH, 0xFF1A1A1A);
		if (maxScroll > 0) {
			int handleH = Math.max(12, viewH * viewH / (viewH + maxScroll));
			int handleY = chatY + 4 + (viewH - handleH) * chatScroll / maxScroll;
			g.fill(trackX, handleY, trackX + 4, handleY + handleH, 0xFFC6C6C6);
		}
	}

	/** The Final Chapter: the boss, like a Hunt boss card. */
	private void drawBossPanel(GuiGraphicsExtractor g, HuntData data, int x, int y, int w, int h, int mouseX, int mouseY) {
		sprite(g, "panel", x, y, w, h);
		int cx = x + w / 2;
		Chapter chapter = TowernaarScript.CHAPTERS.get(TowernaarScript.FINAL);
		g.centeredText(this.font, c(chapter.title(), HuntDefs.GOLD), cx, y + 6, WHITE);
		sprite(g, "panel_dark", x + 6, y + 18, w - 12, 98);
		if (preview == null && this.minecraft.level != null) {
			preview = ModEntities.TOWERNAAR_UNDEAD.create(this.minecraft.level, EntitySpawnReason.LOAD);
			if (preview != null) {
				preview.setId(-2000);
			}
		}
		if (preview != null) {
			InventoryScreen.extractEntityInInventoryFollowsMouse(g, x + 8, y + 20, x + w - 8, y + 114, 30, 0.0625F, mouseX, mouseY, preview);
		}
		int ty = y + 120;
		g.centeredText(this.font, c("Towernaar the Undead", PURPLE), cx, ty, WHITE);
		g.centeredText(this.font, c("The Dark Practicant", GRAY), cx, ty + 10, WHITE);
		g.centeredText(this.font, Component.empty().append(c("❤ " + (int) TowernaarUndead.HEALTH, HuntDefs.RED))
				.append(c("   ⚔ " + (int) TowernaarUndead.DAMAGE, 0xFFFFFF)), cx, ty + 22, WHITE);
		int by = y + h - 22;
		boolean won = data.towernaarDone > TowernaarScript.FINAL;
		int phase = ClientHuntState.towernaarPhase;
		boolean busy = data.quest != null;
		String label = won ? "Defeated" : phase == 1 ? "Slay an enderman..." : phase == 2 ? "In Progress" : busy ? "Finish your Hunting Quest"
				: "[Click] - Start";
		boolean enabled = !won && phase == 0 && !busy;
		boolean hover = mouseX >= x + 8 && mouseX < x + w - 8 && mouseY >= by && mouseY < by + 16;
		g.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace(!enabled ? "widget/button_disabled"
				: hover ? "widget/button_highlighted" : "widget/button"), x + 8, by, w - 16, 16);
		g.centeredText(this.font, c(label, enabled ? 0xFFFFFF : 0xA0A0A0), cx, by + 4, WHITE);
		if (enabled) {
			startButton = new int[] {x + 8, by, w - 16, 16};
		}
		if (enabled && hover) {
			g.setTooltipForNextFrame(this.font, c("Free. Slay an enderman to call him. 10 minutes.", GRAY), mouseX, mouseY);
		}
	}

	private static void sprite(GuiGraphicsExtractor g, String name, int x, int y, int w, int h) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, HuntLike.id("menu/" + name), x, y, w, h);
	}

	// ---------------------------------------------------------------- input

	private void click() {
		stickToBottom = true;
		ClientPlayNetworking.send(new HuntPayloads.Action("towernaar_click", "cluster", 0));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			if (startButton != null && event.x() >= startButton[0] && event.x() < startButton[0] + startButton[2]
					&& event.y() >= startButton[1] && event.y() < startButton[1] + startButton[3]) {
				ClientPlayNetworking.send(new HuntPayloads.Action("towernaar_start", "cluster", 0));
				this.minecraft.gui.setScreen(null);
				return true;
			}
			if (prdButton != null && event.x() >= prdButton[0] && event.x() < prdButton[0] + prdButton[2]
					&& event.y() >= prdButton[1] && event.y() < prdButton[1] + prdButton[3]) {
				ClientPlayNetworking.send(new HuntPayloads.Action("towernaar_prd", "cluster", 0));
				return true;
			}
			if (!finalChapter(ClientHuntState.data) && event.x() >= portraitX && event.x() < portraitX + 56 && event.y() >= portraitY
					&& event.y() < portraitY + 56) {
				click();
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		chatScroll -= (int) Math.round(scrollY * 18);
		stickToBottom = false;
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if ((event.key() == InputConstants.KEY_SPACE || event.key() == InputConstants.KEY_RETURN) && !finalChapter(ClientHuntState.data)) {
			click();
			return true;
		}
		if (event.key() == InputConstants.KEY_BACKSPACE) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}
}
