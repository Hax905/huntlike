package com.huntlike.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import com.huntlike.interlude.ModInterlude;

/**
 * The Growing Slime Ball on an anvil: an enchanted book with exactly one enchantment on the left, the Slimeball on the
 * right, and the book comes out one level higher (up to the vanilla maximum) for 10 levels. A Horseshoe on Pom's
 * Skeletal Remains adds a star (up to 5) for 5 levels.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
	@Shadow
	@Final
	private DataSlot cost;
	@Shadow
	private int repairItemCountCost;

	private AnvilMenuMixin(MenuType<?> type, int id, Inventory inventory, ContainerLevelAccess access,
			net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition slots) {
		super(type, id, inventory, access, slots);
	}

	@Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
	private void huntlike$growingSlimeball(CallbackInfo ci) {
		ItemStack book = this.inputSlots.getItem(0);
		ItemStack ball = this.inputSlots.getItem(1);
		if (ball.is(ModInterlude.HORSESHOE)) {
			huntlike$horseshoe(book, ci);
			return;
		}
		if (!ball.is(ModInterlude.GROWING_SLIME_BALL)) {
			return;
		}
		ci.cancel();
		ItemEnchantments stored = book.get(DataComponents.STORED_ENCHANTMENTS);
		if (!book.is(Items.ENCHANTED_BOOK) || stored == null || stored.size() != 1) {
			this.resultSlots.setItem(0, ItemStack.EMPTY);
			this.cost.set(0);
			return;
		}
		Holder<Enchantment> enchantment = stored.keySet().iterator().next();
		int level = stored.getLevel(enchantment);
		if (level >= enchantment.value().getMaxLevel()) {
			this.resultSlots.setItem(0, ItemStack.EMPTY);
			this.cost.set(0);
			return;
		}
		ItemStack result = book.copyWithCount(1);
		ItemEnchantments.Mutable grown = new ItemEnchantments.Mutable(stored);
		grown.set(enchantment, level + 1);
		result.set(DataComponents.STORED_ENCHANTMENTS, grown.toImmutable());
		this.resultSlots.setItem(0, result);
		this.repairItemCountCost = 1;
		this.cost.set(10);
		this.broadcastChanges();
	}

	/** A Horseshoe on Pom's Skeletal Remains: one more star (up to 5) for 5 levels. */
	private void huntlike$horseshoe(ItemStack remains, CallbackInfo ci) {
		ci.cancel();
		int stars = com.huntlike.interlude.RemainsItem.stars(remains);
		if (!remains.is(ModInterlude.POMS_SKELETAL_REMAINS) || stars >= com.huntlike.interlude.RemainsItem.MAX_STARS) {
			this.resultSlots.setItem(0, ItemStack.EMPTY);
			this.cost.set(0);
			return;
		}
		ItemStack result = remains.copyWithCount(1);
		result.set(ModInterlude.HORSESHOES, stars + 1);
		this.resultSlots.setItem(0, result);
		this.repairItemCountCost = 1;
		this.cost.set(5);
		this.broadcastChanges();
	}
}
