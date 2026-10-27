package com.huntlike.towernaar;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import com.huntlike.gear.ModShields;
import com.huntlike.hunt.HuntData;
import com.huntlike.hunt.HuntDefs;
import com.huntlike.hunt.KoroManager;
import com.huntlike.hunt.Messages;
import com.huntlike.hunt.QuestManager;
import com.huntlike.towernaar.TowernaarScript.Chapter;
import com.huntlike.towernaar.TowernaarScript.Kind;
import com.huntlike.towernaar.TowernaarScript.Step;

/** Server side of Towernaar's questline: one click on him, one step forward. An enderman sound plays on each of his lines. */
public final class TowernaarManager {
	private TowernaarManager() {
	}

	public static void click(ServerPlayer player, HuntData data) {
		if (data.towernaarDone >= TowernaarScript.FINAL) {
			return;
		}
		Chapter chapter = TowernaarScript.CHAPTERS.get(data.towernaarDone);
		if (data.towernaarStep == 0) {
			if (TowernaarScript.canStart(data.towernaarDone, data)) {
				reveal(player, data, chapter, 1);
			}
			return;
		}
		Step current = chapter.steps().get(data.towernaarStep - 1);
		if (current.kind() == Kind.QUEST) {
			if (!KoroManager.hasAll(player, current.items())) {
				QuestManager.playSound(player, SoundEvents.ENDERMAN_HURT, 1.2F);
				return;
			}
			KoroManager.take(player, current.items());
		} else if (current.kind() == Kind.KILLS && killsDone(data) < current.kills()) {
			QuestManager.playSound(player, SoundEvents.ENDERMAN_HURT, 1.2F);
			return;
		}
		reveal(player, data, chapter, data.towernaarStep + 1);
	}

	/** Rogue Hunters slain since the current kill quest started. */
	public static int killsDone(HuntData data) {
		return data.rogueKills - data.towernaarKillBase;
	}

	private static void reveal(ServerPlayer player, HuntData data, Chapter chapter, int step) {
		data.towernaarStep = Math.min(step, chapter.steps().size());
		Step shown = chapter.steps().get(data.towernaarStep - 1);
		switch (shown.kind()) {
			case SAY -> QuestManager.playSound(player, SoundEvents.ENDERMAN_AMBIENT, 1.0F + player.getRandom().nextFloat() * 0.2F);
			case QUEST -> QuestManager.playSound(player, SoundEvents.ENDERMAN_TELEPORT, 1.0F);
			case KILLS -> {
				data.towernaarKillBase = data.rogueKills;
				QuestManager.playSound(player, SoundEvents.ENDERMAN_STARE, 1.4F);
			}
			case COMPLETE -> complete(player, data, chapter);
			default -> {
			}
		}
		player.setAttached(QuestManager.DATA, data);
		QuestManager.sync(player);
	}

	/** Gives the reward and the advancement, and closes the chapter. */
	static void complete(ServerPlayer player, HuntData data, Chapter chapter) {
		if (chapter.reward() != ModShields.MAGNETIC_WAND_PRD) {
			QuestManager.giveStack(player, new ItemStack(chapter.reward()));
		}
		QuestManager.award(player, chapter.advancement());
		player.sendSystemMessage(Messages.c("[Quest Completed!]", HuntDefs.DARK_GREEN)
				.append(Messages.c(" - " + chapter.title(), HuntDefs.LIME)));
		QuestManager.playSound(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F);
		data.towernaarDone++;
		data.towernaarStep = 0;
		ModShields.stampSigil(player, data);
		player.setAttached(QuestManager.DATA, data);
		QuestManager.sync(player);
	}
}
