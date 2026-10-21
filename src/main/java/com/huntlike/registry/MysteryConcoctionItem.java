package com.huntlike.registry;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.huntlike.hunt.HuntData;
import com.huntlike.hunt.Messages;
import com.huntlike.hunt.QuestManager;

/** Eaten once: every rare drop's odds improve from 1/X to 1/(X-1) for that player, permanently. */
public class MysteryConcoctionItem extends Item {
	public MysteryConcoctionItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer && QuestManager.data(serverPlayer).concoction) {
			serverPlayer.sendSystemMessage(Messages.c("You would never eat this again...", 0xAAAAAA));
			return InteractionResult.FAIL;
		}
		return super.use(level, player, hand);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			HuntData data = QuestManager.data(player);
			if (data.concoction) {
				player.sendSystemMessage(Messages.c("You would never eat this again...", 0xAAAAAA));
				return stack;
			}
			data.concoction = true;
			player.setAttached(QuestManager.DATA, data);
			player.sendSystemMessage(Messages.c("Deities laugh at your act and favor your luck on Animal creatures!", 0xFF55FF));
			QuestManager.playSound(player, SoundEvents.ZOMBIE_VILLAGER_CURE, 1.4F);
			QuestManager.sync(player);
		}
		return super.finishUsingItem(stack, level, entity);
	}
}
