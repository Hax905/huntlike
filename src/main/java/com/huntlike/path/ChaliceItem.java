package com.huntlike.path;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** The Chalice: held in the off-hand it collects blood from fleshy creatures you kill (a dark red bar, 100 to fill). */
public class ChaliceItem extends BlockItem {
	public static final int MAX_BLOOD = 100;

	public ChaliceItem(Block block, Properties properties) {
		super(block, properties);
	}

	public static int blood(ItemStack stack) {
		return stack.getOrDefault(ModPaths.BLOOD, 0);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return blood(stack) > 0;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13.0F * Math.min(MAX_BLOOD, blood(stack)) / MAX_BLOOD);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0x8A0303;
	}
}
