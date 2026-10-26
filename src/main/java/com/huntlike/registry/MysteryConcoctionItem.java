package com.huntlike.registry;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

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

/**
 * One-time Consumable: eaten once, every rare drop of one Hunt goes from 1/X to 1/(X-1) for that player, permanently.
 * Mystery Concoction (Animal) and Warm and Slimy Beef Jerky (Cluster).
 */
public class MysteryConcoctionItem extends Item {
	private final Predicate<HuntData> eaten;
	private final BiConsumer<HuntData, Boolean> setEaten;
	private final String creatures;
	private final String refusal;
	private final String advancement;

	public MysteryConcoctionItem(Properties properties, Predicate<HuntData> eaten, BiConsumer<HuntData, Boolean> setEaten,
			String creatures, String refusal, String advancement) {
		super(properties);
		this.eaten = eaten;
		this.setEaten = setEaten;
		this.creatures = creatures;
		this.refusal = refusal;
		this.advancement = advancement;
	}

	public static MysteryConcoctionItem concoction(Properties properties) {
		return new MysteryConcoctionItem(properties, data -> data.concoction, (data, value) -> data.concoction = value, "Animal",
				"You would never eat this again...", "animal/never_again");
	}

	public static MysteryConcoctionItem jerky(Properties properties) {
		return new MysteryConcoctionItem(properties, data -> data.jerky, (data, value) -> data.jerky = value, "Cluster",
				"You would never let that be near your mouth again...", "cluster/jerky");
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer && eaten.test(QuestManager.data(serverPlayer))) {
			serverPlayer.sendSystemMessage(Messages.c(refusal, 0xAAAAAA));
			return InteractionResult.FAIL;
		}
		return super.use(level, player, hand);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		if (entity instanceof ServerPlayer player) {
			HuntData data = QuestManager.data(player);
			if (eaten.test(data)) {
				player.sendSystemMessage(Messages.c(refusal, 0xAAAAAA));
				return stack;
			}
			setEaten.accept(data, true);
			QuestManager.award(player, advancement);
			player.setAttached(QuestManager.DATA, data);
			player.sendSystemMessage(Messages.c("Deities laugh at your act and favor your luck on " + creatures + " creatures!", 0xFF55FF));
			QuestManager.playSound(player, SoundEvents.ZOMBIE_VILLAGER_CURE, 1.4F);
			QuestManager.sync(player);
		}
		return super.finishUsingItem(stack, level, entity);
	}
}
