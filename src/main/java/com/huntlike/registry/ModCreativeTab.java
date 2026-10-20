package com.huntlike.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;

import com.huntlike.HuntLike;

/** The HuntLike tab in the creative inventory. */
public final class ModCreativeTab {
	private ModCreativeTab() {
	}

	public static final ResourceKey<CreativeModeTab> KEY = ResourceKey.create(
			BuiltInRegistries.CREATIVE_MODE_TAB.key(), HuntLike.id("huntlike"));

	public static final CreativeModeTab TAB = FabricCreativeModeTab.builder()
			.icon(() -> new ItemStack(ModBlocks.ALPHA_ALTAR))
			.title(Component.translatable("itemGroup.huntlike"))
			.displayItems((params, output) -> {
				output.accept(ModBlocks.ALPHA_ALTAR);
				output.accept(ModItems.LOW_QUALITY_MYSTERY_MEAT);
				output.accept(ModItems.DECENT_QUALITY_MYSTERY_MEAT);
				output.accept(ModItems.HIGH_QUALITY_MYSTERY_MEAT);
				output.accept(ModItems.THICK_HIDE);
				output.accept(ModItems.LARGE_FEMUR);
				output.accept(ModItems.GOLDEN_MOSS);
				output.accept(ModItems.SPIRITUAL_CATALYST);
				output.accept(ModItems.SOUL_STONE);
				output.accept(ModBlocks.SOUL_MONOLITH);
			})
			.build();

	public static void initialize() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, KEY, TAB);
	}
}
