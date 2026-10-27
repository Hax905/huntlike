package com.huntlike.towernaar;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import com.huntlike.gear.ModShields;
import com.huntlike.hunt.HuntData;
import com.huntlike.hunt.HuntDefs.Cost;

/**
 * Towernaar the Enderian's questline (Cluster Hunt). One click on him, one line. He is "Enderian" until he gives his name at
 * the end of Chapter II. Text markers: _word_ is italic, %player% is the player's name.
 */
public final class TowernaarScript {
	private TowernaarScript() {
	}

	public enum Kind { SAY, QUEST, KILLS, COMPLETE, BOSS }

	/** One step: a line, an item quest, a Rogue Hunter kill quest (kills), the reward, or the Final Chapter's fight. */
	public record Step(Kind kind, String speaker, String text, List<Cost> items, int kills) {
	}

	public record Chapter(String title, int rank, Item reward, String advancement, List<Step> steps) {
	}

	public static final String ENDERIAN = "Enderian";
	public static final String TOWERNAAR = "Towernaar";

	private static List<Step> lines(String speaker, String... texts) {
		List<Step> steps = new ArrayList<>();
		for (String text : texts) {
			steps.add(new Step(Kind.SAY, speaker, text, List.of(), 0));
		}
		return steps;
	}

	private static Step quest(String text, Cost... items) {
		return new Step(Kind.QUEST, "", text, List.of(items), 0);
	}

	private static Step kills(String text, int kills) {
		return new Step(Kind.KILLS, "", text, List.of(), kills);
	}

	private static Step complete() {
		return new Step(Kind.COMPLETE, "", "", List.of(), 0);
	}

	private static List<Step> join(Object... parts) {
		List<Step> steps = new ArrayList<>();
		for (Object part : parts) {
			if (part instanceof Step step) {
				steps.add(step);
			} else if (part instanceof List<?> list) {
				for (Object o : list) {
					steps.add((Step) o);
				}
			}
		}
		return steps;
	}

	public static final List<Chapter> CHAPTERS = List.of(
			new Chapter("The Mysterious Enderian [Chapter I]", 3, ModShields.OBSIDIAN_SHIELD, "towernaar/chapter1", join(
					lines(ENDERIAN,
							"Hm... I know you %player%",
							"I've been watching you from a distance...",
							"Unfortunately I kind of didn't pay attention to my own situations...",
							"And let's say I was slain...",
							"I got killed by a _Rogue Hunter_",
							"I was hunted by a _Rogue Hunter_...",
							"I know... I know",
							"It seems weird that I mention a rogue hunter",
							"But after a certain level you will be hunted back by others...",
							"And yes!",
							"I am a hunter myself!",
							"Well...",
							"Was a hunter...",
							"But I want to help you %player%",
							"I know what is coming for you, you will need a better shield",
							"So let's agree on something!",
							"I will help you with the shield by now",
							"I just need a shield...",
							"Some iron...",
							"Obsidian...",
							"And maybe you are ready...",
							"Hm...",
							"Maybe not still...",
							"If a _Rogue Hunter_ appears and tries to kill you...",
							"Tell me...",
							"Please!"),
					quest("Bring the Enderian the materials for the obsidian shield.", Cost.of(Items.SHIELD, 1), Cost.of(Items.IRON_INGOT, 16),
							Cost.of(Items.OBSIDIAN, 8)),
					lines(ENDERIAN,
							"YES! The obsidian shield!",
							"Oh such a commodity",
							"More durability",
							"It looks better!",
							"And it is nearly unbreakable!",
							"Stories say it is super compatible with enchantments!",
							"Making it last longer than ever!"),
					complete())),
			new Chapter("The Enderian's Trust [Chapter II]", 6, ModShields.UNBREAKABLE_SHIELD, "towernaar/chapter2", join(
					lines(ENDERIAN,
							"I'm sorry %player%...",
							"I know the shield isn't what you expected...",
							"It's just that I thought I had the materials...",
							"But I think I know what I need now!",
							"I might need netherite scrap...",
							"But I need to know you are also worthy of my creation...",
							"It seems exaggerated",
							"But my family is the one attributed to this creation",
							"Not me",
							"It is not my creation",
							"But a recipe I know to make greatness..",
							"I also need ender pearls...",
							"Some of them might be enough",
							"This time I will even trust you some of my resources...",
							"But first...",
							"I need you to prove your strength!",
							"Defeat various _Rogue Hunters!_",
							"They will try to ambush you during quests",
							"They are a pest indeed..."),
					kills("Prove to the Enderian that you are worthy.", 3),
					lines(ENDERIAN,
							"You are back!",
							"I can even feel your growing presence %player%",
							"It grew stronger",
							"It grew more dangerous",
							"But don't let the bloodlust take control",
							"Or you will cross the path of evil without guidance",
							"Creatures that ended up corrupt fell into the vice",
							"The vice of killing without a reason",
							"Killing for joy",
							"As a hunter you will have to learn about the path of evil",
							"Following it will make you stronger",
							"But you have to take control",
							"Don't let the evil control your soul",
							"Or you will turn into a mindless killing machine",
							"But yeah!",
							"Just be careful %player%",
							"Please!",
							"Now, I need the materials from before"),
					quest("Bring the materials the Enderian told you to bring.", Cost.of(ModShields.OBSIDIAN_SHIELD, 1),
							Cost.of(Items.NETHERITE_SCRAP, 2), Cost.of(Items.ENDER_PEARL, 16)),
					lines(ENDERIAN,
							"I know you are worthy %player%",
							"Even before talking to you I knew that...",
							"But people lack motivation",
							"The values and courage to keep going...",
							"But here you are!",
							"Even if you left forever, you deserve this shield...",
							"Us enderians live for centuries",
							"We looked for motivation and things to do",
							"But the End was hollow for us non-worshipers",
							"Endermen are used to walk, wander and worship",
							"To think and evolve by pure thought, intelligence and magic",
							"That's how the Endermen climb the ranks and become powerful",
							"But us enderians don't have the same capacities",
							"We live under their shadow",
							"We are a mix of enderman and human",
							"A weird mix between a creature made to not do",
							"And a creature made to do",
							"But to do or not to do...",
							"Is a hard choice...",
							"...",
							"Thanks for listening %player%!",
							"Talking to someone has been on my checklist since I perished!",
							"Have the shield %player%",
							"I won't bother for a bit...",
							"I need to think...",
							"By the way...",
							"My name is Towernaar!"),
					lines(TOWERNAAR, "Nice to meet you!"),
					complete())),
			new Chapter("The Sigil Shield [Chapter III]", 9, ModShields.UNBREAKABLE_SHIELD_STAR, "towernaar/chapter3", join(
					lines(TOWERNAAR,
							"I've been thinking %player%...",
							"Thinking and thinking...",
							"I've realized I might have missed an opportunity",
							"I am half enderman after all...",
							"I even understand death now...",
							"Thinking is powerful",
							"As long as you can find explanations and reasons...",
							"You will be able to go on...",
							"I find my soul to be powerful",
							"I find myself in a position of impotence as well...",
							"I might find a way...",
							"Thanks for listening in again %player%!",
							"This time, let's be simple",
							"Your shield can be better",
							"Prove to me that you can carry my family's sigil",
							"Slay various _Rogue Hunters_ and come back",
							"I will grant you the sigil of my family after that...",
							"Good luck!"),
					kills("Slay 10 Rogue Hunters.", 10),
					lines(TOWERNAAR,
							"You are strong %player%",
							"I will grant you my family's sigil...",
							"Give me your shield..."),
					quest("Give Towernaar your Unbreakable Shield.", Cost.of(ModShields.UNBREAKABLE_SHIELD, 1)),
					lines(TOWERNAAR,
							"...",
							"%player%",
							"I don't know if I want to follow my family's path",
							"But it is the only way in which I can progress",
							"My family is one that follows the deranged path",
							"The deranged path of evil",
							"In my lineage...",
							"the only way I can get stronger...",
							"Is through the path of evil...",
							"I'm sorry %player%",
							"But don't worry, I will try to control myself",
							"...",
							"If I take the wrong steps...",
							"Please stop me",
							"I know I can trust your strength!"),
					complete())),
			new Chapter("The Revitalized Enderian [Final Chapter]", 10, ModShields.MAGNETIC_WAND_PRD, "towernaar/final", List.of(
					new Step(Kind.BOSS, "", "", List.of(), 0))));

	public static final int FINAL = 3;

	/** Whether the player may start chapter index (0-3): the Cluster Rank it asks for. */
	public static boolean canStart(int index, HuntData data) {
		return index < CHAPTERS.size() && data.progress("cluster").rank() >= CHAPTERS.get(index).rank();
	}

	/** The name shown for him: Enderian until he names himself at the end of Chapter II. */
	public static String name(HuntData data) {
		return data.towernaarDone >= 2 ? TOWERNAAR : ENDERIAN;
	}
}
