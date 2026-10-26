package com.huntlike.client.pet;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.huntlike.pet.PetMenu;

/**
 * The big slime pet's menu, laid out like a donkey's: the pet on the left, the chest slot where the saddle would be, and the
 * page button (1 / 2) under it; the chest's storage below once a chest is in.
 */
public class PetScreen extends AbstractContainerScreen<PetMenu> {
	private static final int BACKGROUND = 0xFFC6C6C6;
	private static final int LIGHT = 0xFFFFFFFF;
	private static final int DARK = 0xFF555555;
	private static final int SLOT = 0xFF8B8B8B;
	private static final int SLOT_DARK = 0xFF373737;
	private static final int TEXT = 0xFF404040;

	private Button pageButton;
	private float mouseX;
	private float mouseY;

	public PetScreen(PetMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 222);
		this.inventoryLabelY = PetMenu.INVENTORY_Y - 11;
	}

	@Override
	protected void init() {
		super.init();
		this.pageButton = this.addRenderableWidget(Button.builder(Component.literal("1"), button -> {
			if (this.minecraft.gameMode != null) {
				this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
			}
		}).bounds(this.leftPos + PetMenu.CHEST_X - 1, this.topPos + PetMenu.CHEST_Y + 20, 18, 14).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		this.pageButton.visible = this.menu.hasChest();
		this.pageButton.setMessage(Component.literal(String.valueOf(this.menu.page() + 1)));
		super.extractRenderState(g, mouseX, mouseY, partialTick);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(g, mouseX, mouseY, partialTick);
		int x = this.leftPos;
		int y = this.topPos;
		panel(g, x, y, this.imageWidth, this.imageHeight);
		// The pet, in a black box like the donkey's.
		int bx = x + 30;
		int by = y + 17;
		g.fill(bx, by, bx + 54, by + 54, SLOT_DARK);
		g.fill(bx + 1, by + 1, bx + 53, by + 53, 0xFF000000);
		Entity entity = this.minecraft.level == null ? null : this.minecraft.level.getEntity(this.menu.petId());
		if (entity instanceof LivingEntity pet) {
			InventoryScreen.extractEntityInInventoryFollowsMouse(g, bx + 1, by + 1, bx + 53, by + 53, 13, 0.25F, this.mouseX, this.mouseY,
					pet);
		}
		for (Slot slot : this.menu.slots) {
			if (slot.isActive()) {
				slot(g, x + slot.x - 1, y + slot.y - 1);
			}
		}
		if (!this.menu.hasChest()) {
			g.text(this.font, Component.literal("Put a chest in the chest slot"), x + 8, y + PetMenu.STORAGE_Y + 14, TEXT, false);
			g.text(this.font, Component.literal("to carry a double chest."), x + 8, y + PetMenu.STORAGE_Y + 25, TEXT, false);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		super.extractLabels(g, mouseX, mouseY);
		if (this.menu.hasChest()) {
			g.text(this.font, Component.literal("Page " + (this.menu.page() + 1) + " / 2"), 90, 24, TEXT, false);
		}
	}

	private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		g.fill(x, y, x + w, y + h, DARK);
		g.fill(x, y, x + w - 1, y + h - 1, LIGHT);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, BACKGROUND);
	}

	private static void slot(GuiGraphicsExtractor g, int x, int y) {
		g.fill(x, y, x + 18, y + 18, LIGHT);
		g.fill(x, y, x + 17, y + 17, SLOT_DARK);
		g.fill(x + 1, y + 1, x + 17, y + 17, SLOT);
	}
}
