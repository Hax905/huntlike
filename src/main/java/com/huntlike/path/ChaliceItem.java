package com.huntlike.path;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import com.huntlike.hunt.Messages;

/** The Chalice: held in the off-hand it collects blood from fleshy creatures you kill (a dark red bar, 100 to fill). */
public class ChaliceItem extends BlockItem {
	public static final int MAX_BLOOD = 100;

	public ChaliceItem(Block block, Properties properties) {
		super(block, properties);
	}

	/** Used in the air: Canavar's challenge (0.5 s cooldown). Used on a block, it is placed as usual. */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendSystemMessage(Messages.c("Canavar: Prove your strength and fill this chalice!", 0xAA0000));
			serverPlayer.getCooldowns().addCooldown(player.getItemInHand(hand), FullChaliceItem.COOLDOWN);
		}
		return InteractionResult.SUCCESS;
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
