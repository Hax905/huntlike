package com.huntlike.hunt;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import com.huntlike.hunt.HuntDefs.Cost;
import com.huntlike.hunt.KoroScript.Chapter;
import com.huntlike.hunt.KoroScript.Kind;
import com.huntlike.hunt.KoroScript.Step;

/** Server side of Koro's questline: one click on Koro, one step forward. */
public final class KoroManager {
	private KoroManager() {
	}

	public static void click(ServerPlayer player, HuntData data) {
		if (data.koroDone >= KoroScript.CHAPTERS.size()) {
			return;
		}
		Chapter chapter = KoroScript.CHAPTERS.get(data.koroDone);
		if (data.koroStep == 0) {
			if (KoroScript.canStart(data.koroDone, data.progress("animal"))) {
				reveal(player, data, chapter, 1);
			}
			return;
		}
		Step current = chapter.steps().get(data.koroStep - 1);
		if (current.kind() == Kind.QUEST) {
			if (!hasAll(player, current.items())) {
				QuestManager.playSound(player, SoundEvents.VILLAGER_NO, 1.0F);
				return;
			}
			take(player, current.items());
		}
		reveal(player, data, chapter, data.koroStep + 1);
	}

	private static void reveal(ServerPlayer player, HuntData data, Chapter chapter, int step) {
		data.koroStep = Math.min(step, chapter.steps().size());
		Step shown = chapter.steps().get(data.koroStep - 1);
		switch (shown.kind()) {
			case CLANK -> QuestManager.playSound(player, SoundEvents.ANVIL_USE, 1.0F);
			case QUEST -> QuestManager.playSound(player, SoundEvents.VILLAGER_WORK_TOOLSMITH, 1.0F);
			case COMPLETE -> {
				QuestManager.giveStack(player, new ItemStack(chapter.reward()));
				QuestManager.award(player, chapter.advancement());
				player.sendSystemMessage(Messages.c("[Quest Completed!]", HuntDefs.DARK_GREEN)
						.append(Messages.c(" - " + chapter.title(), HuntDefs.LIME)));
				QuestManager.playSound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F);
				data.koroDone++;
				data.koroStep = 0;
			}
			default -> QuestManager.playSound(player, SoundEvents.VILLAGER_AMBIENT, 1.0F);
		}
		player.setAttached(QuestManager.DATA, data);
		QuestManager.sync(player);
	}

	public static boolean hasAll(ServerPlayer player, List<Cost> items) {
		for (Cost cost : items) {
			if (count(player.getInventory(), cost) < cost.count()) {
				return false;
			}
		}
		return true;
	}

	private static int count(Inventory inventory, Cost cost) {
		int have = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).is(cost.item())) {
				have += inventory.getItem(i).getCount();
			}
		}
		return have;
	}

	private static void take(ServerPlayer player, List<Cost> items) {
		Inventory inventory = player.getInventory();
		for (Cost cost : items) {
			int left = cost.count();
			for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
				ItemStack stack = inventory.getItem(i);
				if (stack.is(cost.item())) {
					int taken = Math.min(left, stack.getCount());
					stack.shrink(taken);
					left -= taken;
				}
			}
		}
		inventory.setChanged();
		player.inventoryMenu.broadcastChanges();
	}
}
