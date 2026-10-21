package com.huntlike.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Corner popup after buying Hunt gear, styled like the advancement popup. */
public class GearToast implements Toast {
	private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("toast/advancement");
	private static final long DURATION = 5000L;

	private final ItemStack icon;
	private final Component name;
	private Visibility visibility = Visibility.SHOW;

	public GearToast(ItemStack icon, String name) {
		this.icon = icon;
		this.name = Component.literal(name);
	}

	@Override
	public Visibility getWantedVisibility() {
		return visibility;
	}

	@Override
	public void update(ToastManager manager, long time) {
		if (time >= DURATION) {
			visibility = Visibility.HIDE;
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, Font font, long time) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, width(), height());
		g.item(icon, 8, 8);
		g.text(font, Component.literal("Gear Obtained!"), 30, 7, 0xFFFFAA00, false);
		g.text(font, name, 30, 18, 0xFFFFFFFF, false);
	}
}
