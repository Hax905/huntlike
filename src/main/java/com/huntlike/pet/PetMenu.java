package com.huntlike.pet;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.huntlike.HuntLike;

/**
 * The big slime pet's menu (like a donkey's): a chest slot instead of the saddle, and once a chest is in, a double chest's
 * storage shown one page (27 slots) at a time; button 0 swaps pages. Opens on the page used last.
 */
public class PetMenu extends AbstractContainerMenu {
	public static final MenuType<PetMenu> TYPE = Registry.register(BuiltInRegistries.MENU, HuntLike.id("slime_pet"),
			new MenuType<>(PetMenu::new, FeatureFlags.VANILLA_SET));

	public static final int CHEST_X = 8;
	public static final int CHEST_Y = 18;
	public static final int STORAGE_Y = 74;
	public static final int INVENTORY_Y = 140;

	private final Container chestSlot;
	private final Container page;
	private final ContainerData data;
	private final PetState state;
	private final PetCube pet;

	/** Client side. */
	public PetMenu(int id, Inventory inventory) {
		this(id, inventory, new SimpleContainer(1), new SimpleContainer(PetState.PAGE), new net.minecraft.world.inventory.SimpleContainerData(3),
				null, null);
	}

	/** Server side, backed by the owner's pet state. */
	public PetMenu(int id, Inventory inventory, PetState state, PetCube pet) {
		this(id, inventory, new ChestSlotContainer(state), new PageContainer(state), new StateData(state, pet), state, pet);
	}

	private PetMenu(int id, Inventory inventory, Container chestSlot, Container page, ContainerData data, PetState state, PetCube pet) {
		super(TYPE, id);
		this.chestSlot = chestSlot;
		this.page = page;
		this.data = data;
		this.state = state;
		this.pet = pet;
		this.addSlot(new Slot(chestSlot, 0, CHEST_X, CHEST_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(Items.CHEST);
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}

			@Override
			public boolean mayPickup(Player player) {
				return storageEmpty();
			}
		});
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(page, col + row * 9, 8 + col * 18, STORAGE_Y + row * 18) {
					@Override
					public boolean isActive() {
						return hasChest();
					}

					@Override
					public boolean mayPlace(ItemStack stack) {
						return hasChest();
					}
				});
			}
		}
		this.addStandardInventorySlots(inventory, 8, INVENTORY_Y);
		this.addDataSlots(data);
	}

	public boolean hasChest() {
		return this.getSlot(0).hasItem();
	}

	/** Page shown: 0 or 1. */
	public int page() {
		return data.get(1);
	}

	public int petId() {
		return data.get(0);
	}

	private boolean storageEmpty() {
		return state == null || state.storageEmpty();
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id == 0 && state != null && state.chest) {
			state.page = 1 - state.page;
			this.broadcastChanges();
			return true;
		}
		return false;
	}

	@Override
	public boolean stillValid(Player player) {
		return pet == null || pet.isAlive() && (pet.hasPassenger(player) || pet.distanceToSqr(player) < 8 * 8);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		int playerStart = 1 + PetState.PAGE;
		int playerEnd = playerStart + 36;
		if (index < playerStart) {
			if (!this.moveItemStackTo(stack, playerStart, playerEnd, true)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.is(Items.CHEST) && !this.getSlot(0).hasItem()) {
			if (!this.moveItemStackTo(stack, 0, 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!hasChest() || !this.moveItemStackTo(stack, 1, playerStart, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return copy;
	}

	public static void initialize() {
	}

	/** The chest slot: holds one chest while the pet has its storage. */
	private static class ChestSlotContainer implements Container {
		private final PetState state;

		ChestSlotContainer(PetState state) {
			this.state = state;
		}

		@Override
		public int getContainerSize() {
			return 1;
		}

		@Override
		public boolean isEmpty() {
			return !state.chest;
		}

		@Override
		public ItemStack getItem(int slot) {
			return state.chest ? new ItemStack(Items.CHEST) : ItemStack.EMPTY;
		}

		@Override
		public ItemStack removeItem(int slot, int count) {
			return removeItemNoUpdate(slot);
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			if (!state.chest || !state.storageEmpty()) {
				return ItemStack.EMPTY;
			}
			state.chest = false;
			return new ItemStack(Items.CHEST);
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			state.chest = !stack.isEmpty();
		}

		@Override
		public void setChanged() {
		}

		@Override
		public boolean stillValid(Player player) {
			return true;
		}

		@Override
		public void clearContent() {
		}
	}

	/** One page of the pet's 54-slot storage. */
	private static class PageContainer implements Container {
		private final PetState state;

		PageContainer(PetState state) {
			this.state = state;
		}

		private int offset() {
			return state.page * PetState.PAGE;
		}

		@Override
		public int getContainerSize() {
			return PetState.PAGE;
		}

		@Override
		public boolean isEmpty() {
			for (int i = 0; i < PetState.PAGE; i++) {
				if (!getItem(i).isEmpty()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public ItemStack getItem(int slot) {
			return state.storage.get(offset() + slot);
		}

		@Override
		public ItemStack removeItem(int slot, int count) {
			ItemStack stack = getItem(slot);
			return stack.isEmpty() ? ItemStack.EMPTY : stack.split(count);
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			ItemStack stack = getItem(slot);
			state.storage.set(offset() + slot, ItemStack.EMPTY);
			return stack;
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			state.storage.set(offset() + slot, stack);
		}

		@Override
		public void setChanged() {
		}

		@Override
		public boolean stillValid(Player player) {
			return true;
		}

		@Override
		public void clearContent() {
		}
	}

	/** Synced numbers: the pet's entity id (for the preview), the page and its health. */
	private static class StateData implements ContainerData {
		private final PetState state;
		private final PetCube pet;

		StateData(PetState state, PetCube pet) {
			this.state = state;
			this.pet = pet;
		}

		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> pet.getId();
				case 1 -> state.page;
				default -> state.heat;
			};
		}

		@Override
		public void set(int index, int value) {
		}

		@Override
		public int getCount() {
			return 3;
		}
	}
}
