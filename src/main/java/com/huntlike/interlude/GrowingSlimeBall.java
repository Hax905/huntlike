package com.huntlike.interlude;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * The Growing Slime Ball on an anvil: on an enchanted book or an armor piece, every enchantment below its maximum goes
 * up one level. Costs 5 levels per enchantment raised.
 */
public final class GrowingSlimeBall {
	public static final int LEVELS_PER_ENCHANTMENT = 5;

	private GrowingSlimeBall() {
	}

	/** The enchantments component this item keeps its enchantments in, or null if the Slimeball doesn't work on it. */
	private static DataComponentType<ItemEnchantments> component(ItemStack stack) {
		if (stack.is(Items.ENCHANTED_BOOK)) {
			return DataComponents.STORED_ENCHANTMENTS;
		}
		var equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
			return DataComponents.ENCHANTMENTS;
		}
		return null;
	}

	/** How many enchantments on this item can still go up a level. */
	public static int upgradeable(ItemStack stack) {
		DataComponentType<ItemEnchantments> type = component(stack);
		ItemEnchantments enchantments = type == null ? null : stack.get(type);
		if (enchantments == null) {
			return 0;
		}
		int count = 0;
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			if (enchantments.getLevel(enchantment) < enchantment.value().getMaxLevel()) {
				count++;
			}
		}
		return count;
	}

	public static int cost(ItemStack stack) {
		return upgradeable(stack) * LEVELS_PER_ENCHANTMENT;
	}

	/** The item with every enchantment below its maximum one level higher, or empty if nothing can grow. */
	public static ItemStack grow(ItemStack stack) {
		if (upgradeable(stack) == 0) {
			return ItemStack.EMPTY;
		}
		DataComponentType<ItemEnchantments> type = component(stack);
		ItemEnchantments enchantments = stack.get(type);
		ItemEnchantments.Mutable grown = new ItemEnchantments.Mutable(enchantments);
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			int level = enchantments.getLevel(enchantment);
			if (level < enchantment.value().getMaxLevel()) {
				grown.set(enchantment, level + 1);
			}
		}
		ItemStack result = stack.copyWithCount(1);
		result.set(type, grown.toImmutable());
		return result;
	}
}
