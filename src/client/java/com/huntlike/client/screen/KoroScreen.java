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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.huntlike.HuntLike;
import com.huntlike.client.ClientHuntState;
import com.huntlike.hunt.HuntData;
import com.huntlike.hunt.HuntDefs;
import com.huntlike.hunt.HuntDefs.Cost;
import com.huntlike.hunt.KoroScript;
import com.huntlike.hunt.KoroScript.Chapter;
import com.huntlike.hunt.KoroScript.Kind;
import com.huntlike.hunt.KoroScript.Step;
import com.huntlike.network.HuntPayloads;
import com.huntlike.registry.ModItems;

/**
 * Koro's dialogue window. Left: the soul's portrait (click it to advance) and the Quest Items; right: the chat,
 * which keeps the last conversation and is only replaced when a new chapter starts.
 */
public class KoroScreen extends Screen {
	private static final Identifier PORTRAIT = HuntLike.id("textures/gui/koro.png");
	private static final int W = 360;
	private static final int H = 220;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFFAAAAAA;
	private static final int LIGHT_BLUE = 0x8FD3FF;
	private static final int YELLOW = 0xFFFF55;

	private final Screen parent;
	private int left;
	private int top;
	private int portraitX;
	private int portraitY;
	private int chatScroll;
	private int lastLineCount = -1;
	private boolean stickToBottom = true;
	private long bobUntil;

	public KoroScreen(Screen parent) {
		super(Component.literal("Spiritual Connection"));
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

	// ---------------------------------------------------------------- the conversation

	/** Index of the chapter whose conversation is shown, or -1 before meeting Koro. */
	private static int shownChapter(HuntData data) {
		if (data.koroStep > 0) {
			return data.koroDone;
		}
		return data.koroDone - 1;
	}

	private static Component itemName(Item item, HuntData data) {
		MutableComponent name = Component.literal(new ItemStack(item).getHoverName().getString()).withColor(LIGHT_BLUE);
		if (item == ModItems.SPIRITUAL_CATALYST && !data.progress("animal").dropsSeen.contains("spiritual_catalyst")) {
			name = name.withStyle(style -> style.withObfuscated(true));
		}
		return name;
	}

	/** Speech with _italic_ words and the player's name. */
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

	private List<Component> conversation(HuntData data) {
		List<Component> lines = new ArrayList<>();
		int index = shownChapter(data);
		if (index >= 0) {
			Chapter chapter = KoroScript.CHAPTERS.get(index);
			int shown = data.koroStep > 0 ? data.koroStep : chapter.steps().size();
			for (int i = 0; i < shown; i++) {
				Step step = chapter.steps().get(i);
				switch (step.kind()) {
					case SAY -> {
						boolean soul = step.speaker().equals("[Soul]");
						lines.add(Component.empty().append(c(soul ? "[Soul]" : "Koro", soul ? GRAY : HuntDefs.LIME))
								.append(c(": ", 0xFFFFFF)).append(speech(step.text())));
					}
					case PROMPT -> lines.add(c(step.text(), GRAY));
					case CLANK -> lines.add(c("*Clank*", GRAY).withStyle(style -> style.withItalic(true)));
					case QUEST -> {
						MutableComponent line = Component.empty().append(c("[Quest Started]", YELLOW))
								.append(c(" - " + step.text() + " Requirements: ", 0xFFFFFF));
						for (int r = 0; r < step.items().size(); r++) {
							Cost cost = step.items().get(r);
							line.append(itemName(cost.item(), data)).append(c(" [x" + cost.count() + "]", LIGHT_BLUE))
									.append(c(r < step.items().size() - 1 ? ", " : ".", 0xFFFFFF));
						}
						lines.add(line);
					}
					case COMPLETE -> lines.add(Component.empty().append(c("[Quest Completed!]", HuntDefs.DARK_GREEN))
							.append(c(" - " + chapter.title(), HuntDefs.LIME)));
					default -> {
					}
				}
			}
		}
		if (data.koroStep == 0 && data.koroDone < KoroScript.CHAPTERS.size()) {
			if (KoroScript.canStart(data.koroDone, data.progress("animal"))) {
				lines.add(c(data.koroDone == 0 ? "Click on the soul" : "Click on Koro", GRAY));
			} else if (data.koroDone > 0) {
				lines.add(Component.empty().append(c("Koro", HuntDefs.LIME))
						.append(c(": You are still not ready for my next quest dear adventurer...", 0xFFFFFF)));
			}
		}
		return lines;
	}

	/** The quest waiting for items, or null. */
	private static Step openQuest(HuntData data) {
		if (data.koroStep == 0) {
			return null;
		}
		Step step = KoroScript.CHAPTERS.get(data.koroDone).steps().get(data.koroStep - 1);
		return step.kind() == Kind.QUEST ? step : null;
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
		left = (this.width - W) / 2;
		top = (this.height - H) / 2;
		sprite(g, "window", left, top, W, H);
		g.text(this.font, c("Questline - Spiritual Connection", 0x404040), left + 10, top + 7, 0xFF404040, false);
		sprite(g, "content", left + 6, top + 18, W - 12, H - 24);
		sprite(g, "portal", left + 8, top + 20, W - 16, H - 28);

		// Left column: portrait, name, Quest Items.
		int colX = left + 14;
		int colW = 104;
		portraitX = colX + (colW - 56) / 2;
		portraitY = top + 26;
		boolean hover = mouseX >= portraitX && mouseX < portraitX + 56 && mouseY >= portraitY && mouseY < portraitY + 56;
		sprite(g, hover ? "symbol_hover" : "symbol", portraitX, portraitY, 56, 56);
		long now = System.currentTimeMillis();
		int bob = now < bobUntil ? -1 : 0;
		g.blit(RenderPipelines.GUI_TEXTURED, PORTRAIT, portraitX + 4, portraitY + 4 + bob, 0.0F, 0.0F, 48, 48, 48, 48, 48, 48);
		// A soul: a slow green shimmer over the portrait.
		int alpha = (int) (24 + 20 * Math.sin(now / 600.0));
		g.fill(portraitX + 4, portraitY + 4, portraitX + 52, portraitY + 52, (alpha << 24) | HuntDefs.LIME);
		boolean named = data.koroDone > 0 || data.koroStep > 1;
		g.centeredText(this.font, named ? c("Koro", HuntDefs.LIME) : c("[Soul]", GRAY), colX + colW / 2, portraitY + 60, WHITE);

		int y = portraitY + 74;
		sprite(g, "panel_dark", colX, y, colW, top + H - 14 - y);
		g.text(this.font, c("Quest Items", 0xFFFFFF), colX + 4, y + 4, WHITE, false);
		y += 15;
		Step quest = openQuest(data);
		if (quest == null) {
			g.text(this.font, c("None", GRAY), colX + 4, y, WHITE, false);
			y += 10;
		} else {
			for (Cost cost : quest.items()) {
				boolean have = carried(cost.item()) >= cost.count();
				MutableComponent name = Component.empty().append(itemName(cost.item(), data))
						.append(c(" [x" + cost.count() + "]", LIGHT_BLUE));
				if (!have) {
					name = Component.empty().append(name).append(c(" ✖", HuntDefs.RED));
				}
				for (FormattedCharSequence line : this.font.split(name, colW - 8)) {
					g.text(this.font, line, colX + 4, y, WHITE, false);
					y += 9;
				}
				y += 1;
			}
		}
		if (data.koroStep > 0) {
			Item reward = KoroScript.CHAPTERS.get(data.koroDone).reward();
			y += 4;
			g.text(this.font, c("Crafting:", GRAY), colX + 4, y, WHITE, false);
			y += 10;
			for (FormattedCharSequence line : this.font.split(c(new ItemStack(reward).getHoverName().getString(), HuntDefs.GOLD),
					colW - 8)) {
				g.text(this.font, line, colX + 4, y, WHITE, false);
				y += 9;
			}
		}

		// Right: the chat.
		int chatX = colX + colW + 8;
		int chatY = top + 26;
		int chatW = left + W - 14 - chatX;
		int chatH = top + H - 14 - chatY;
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

		if (hover) {
			g.setTooltipForNextFrame(this.font, c(named ? "Click on Koro" : "Click on the soul", GRAY), mouseX, mouseY);
		}
	}

	private static void sprite(GuiGraphicsExtractor g, String name, int x, int y, int w, int h) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, HuntLike.id("menu/" + name), x, y, w, h);
	}

	// ---------------------------------------------------------------- input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && event.x() >= portraitX && event.x() < portraitX + 56
				&& event.y() >= portraitY && event.y() < portraitY + 56) {
			stickToBottom = true;
			ClientPlayNetworking.send(new HuntPayloads.Action("koro_click", "animal", 0));
			return true;
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
		if (event.key() == InputConstants.KEY_SPACE || event.key() == InputConstants.KEY_RETURN) {
			stickToBottom = true;
			ClientPlayNetworking.send(new HuntPayloads.Action("koro_click", "animal", 0));
			return true;
		}
		if (event.key() == InputConstants.KEY_BACKSPACE) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}
}
