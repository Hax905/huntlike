package com.huntlike.path;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import com.huntlike.hunt.Messages;

/** The Chalice filled with blood. Using it speaks (a dialogue to come; for now one line), with a 0.5 s cooldown. */
public class FullChaliceItem extends Item {
	public static final int COOLDOWN = 10;

	public FullChaliceItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendSystemMessage(Messages.c("Canavar: Hello mighty adventurer!", 0xAA0000));
			serverPlayer.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN);
		}
		return InteractionResult.SUCCESS;
	}
}
