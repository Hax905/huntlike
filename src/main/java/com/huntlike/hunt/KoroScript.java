package com.huntlike.hunt;

import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import com.huntlike.gear.ModKoro;
import com.huntlike.hunt.HuntDefs.Cost;
import com.huntlike.registry.ModItems;

/**
 * Koro's questline, word for word from the design. Each click on Koro reveals the next step; a quest step waits
 * until the player hands in its items; the complete step gives the chapter's reward.
 * Text markers: _word_ is italic, %player% is the player's name.
 */
public final class KoroScript {
	private KoroScript() {
	}

	public enum Kind { SAY, PROMPT, CLANK, QUEST, COMPLETE }

	public record Step(Kind kind, String speaker, String text, List<Cost> items) {
	}

	public record Chapter(String title, int rank, boolean needsTierFour, Item reward, String advancement, List<Step> steps) {
	}

	private static Step say(String speaker, String text) {
		return new Step(Kind.SAY, speaker, text, List.of());
	}

	private static Step prompt(String text) {
		return new Step(Kind.PROMPT, "", text, List.of());
	}

	private static Step clank() {
		return new Step(Kind.CLANK, "", "*Clank*", List.of());
	}

	private static Step quest(String text, List<Cost> items) {
		return new Step(Kind.QUEST, "", text, items);
	}

	private static Step complete() {
		return new Step(Kind.COMPLETE, "", "", List.of());
	}

	public static final List<Chapter> CHAPTERS = List.of(
			new Chapter("Koro the Craftsman [Chapter I]", 3, false, ModKoro.WOODEN_BELL, "koro/chapter1", List.of(
					say("[Soul]", "Hi adventurer! My name is Koro."),
					prompt("Click on Koro"),
					say("Koro", "I got lost one day trying to find mushrooms to make dinner."),
					say("Koro", "But I got lost in the forest and got attacked by hyenas."),
					say("Koro", "I barely made it alive and on the way home I looked back to realize my body was some steps behind..."),
					say("Koro", "I HAD PERISHED!"),
					say("Koro", "I died alone, wounded... and most importantly... HUNGRY!"),
					say("Koro", "I know it is a weird request, but could you bring me some mushroom stew?"),
					quest("Bring Koro Mushroom Stew.", List.of(Cost.of(Items.MUSHROOM_STEW, 1))),
					say("Koro", "TRULY GREAT!"),
					say("Koro", "MUCH BETTER THAN HAVING GRASS AND ROTTEN FLESH FOR DINNER!"),
					say("Koro", "Oh dear adventurer... I owe you..."),
					say("Koro", "I was a craftsman during my \"flesh years\"."),
					say("Koro", "I could help you craft a really useful item!"),
					say("Koro", "If you bring me that Thick Hide from those animals you slay..."),
					say("Koro", "And some Wood of an Oak Tree..."),
					say("Koro", "I would be able to craft a Wooden Bell."),
					say("Koro", "I know it sounds underwhelming!"),
					say("Koro", "But you'll see!"),
					say("Koro", "Plus... you know... If you bring me some of that Meat those animals give..."),
					say("Koro", "I would be VERY thankful..."),
					quest("Bring Koro the food and materials he asked for.", List.of(Cost.of(Items.OAK_LOG, 16), Cost.of(ModItems.THICK_HIDE, 1), Cost.of(ModItems.LOW_QUALITY_MYSTERY_MEAT, 5))),
					say("Koro", "THIS MEAT IS INCREDIBLE!"),
					say("Koro", "THE TASTE!"),
					say("Koro", "THE SMELL!"),
					say("Koro", "EVEN HOW IT FEELS!"),
					say("Koro", "I WILL ENJOY THIS SO MUCH!..."),
					say("Koro", "Oh! I forgot about the bell..."),
					say("Koro", "Sorry for that..."),
					clank(),
					clank(),
					clank(),
					say("Koro", "Here, take this Wooden Bell."),
					say("Koro", "Thanks for the meat!"),
					say("Koro", "I see your strong soul, capable of more than what you have done already..."),
					say("Koro", "Keep going dear adventurer!"),
					say("Koro", "You will achieve great things!"),
					complete())),
			new Chapter("Koro the Craftsman [Chapter II]", 6, false, ModKoro.GOLDEN_BELL, "koro/chapter2", List.of(
					say("Koro", "I acknowledge the strength of your soul dear adventurer!"),
					say("Koro", "You have defeated many!"),
					say("Koro", "I'm impressed!"),
					say("Koro", "For that same reason, I will help you again..."),
					say("Koro", "Remember your bell? I sure hope it was useful."),
					say("Koro", "But let's make it MORE useful now!"),
					say("Koro", "I have been reading books of metalwork and blacksmithing."),
					say("Koro", "Poor villagers..."),
					say("Koro", "They thought their bookshelves were haunted..."),
					say("Koro", "But yeah! I think I know how to make it more... _soothing_"),
					say("Koro", "First! I will need some preparation materials."),
					say("Koro", "Some Sand and Iron could work for now..."),
					quest("Bring Koro smithing materials.", List.of(Cost.of(Items.IRON_INGOT, 5), Cost.of(Items.SAND, 10))),
					say("Koro", "With these I can make myself some metalwork gear!"),
					say("Koro", "In the meantime, I need some materials for the actual Upgrade!"),
					say("Koro", "I would need some beautiful metal..."),
					say("Koro", "And maybe some better hide..."),
					say("Koro", "I have seen how some of those animals you kill still have that beautiful hide..."),
					say("Koro", "It is decided!"),
					say("Koro", "Some gold ingots and hide should do!"),
					say("Koro", "A sound and sight that will delight your being."),
					say("Koro", "OH! AND SOME OF THAT NEW MEAT"),
					say("Koro", "YES YES, THAT ONE THAT YOU CALL DECENT!"),
					say("Koro", "Hm hm... hm..."),
					say("Koro", "Sorry about that, but yeah, I would like some of that new meat you have gotten."),
					quest("Bring Koro the bell materials.", List.of(Cost.of(Items.GOLD_INGOT, 10), Cost.of(ModItems.THICK_HIDE, 2), Cost.of(ModKoro.WOODEN_BELL, 1), Cost.of(ModItems.DECENT_QUALITY_MYSTERY_MEAT, 3))),
					say("Koro", "OH THE SMELL IS SO DELIGHTFUL!"),
					say("Koro", "I CAN'T WAIT TO HAVE THIS ON MY PLATE!"),
					say("Koro", "Hm! Sorry again... I got carried away..."),
					say("Koro", "I'm working on it..."),
					say("Koro", "Let me work on this bell right here"),
					say("Koro", "I already made a cast for it!"),
					say("Koro", "The upgrade will elevate the beautiful sound it makes!"),
					say("Koro", "Here it is! Done..."),
					say("Koro", "It looks so soothing..."),
					say("Koro", "Have it, it will hopefully be useful"),
					say("Koro", "I wish you the best of the best dear adventurer!"),
					complete())),
			new Chapter("Koro the Craftsman [Chapter III]", 9, true, ModKoro.MOSSY_BELL, "koro/chapter3", List.of(
					say("Koro", "You seem ready for a new adventure..."),
					say("Koro", "Dear adventurer..."),
					say("Koro", "I feel strong danger from this quest..."),
					say("Koro", "You will have to prepare this time..."),
					say("Koro", "I've been reading of a golden moss..."),
					say("Koro", "One that has magical healing properties..."),
					say("Koro", "And I have noticed while watching you, that the latest beasts you slayed have the same moss on their back!"),
					say("Koro", "I will need one of those if you yearn for more nourishment from the bell..."),
					say("Koro", "And I would also like some of that better food I've seen you can make, PLEASE!"),
					quest("Bring Koro the rare moss and food.", List.of(Cost.of(ModItems.GOLDEN_MOSS, 1), Cost.of(ModItems.HIGH_QUALITY_MYSTERY_MEAT, 1), Cost.of(ModKoro.GOLDEN_BELL, 1))),
					say("Koro", "That smell..."),
					say("Koro", "It is surprising that it comes from the moss and not the meat..."),
					say("Koro", "But I want to cook the meat SO BAD!"),
					say("Koro", "IT WILL TASTE MUCH BETTER!"),
					say("Koro", "Hm... Sorry again..."),
					say("Koro", "I know it will seem stupid..."),
					say("Koro", "But by carefully putting the Golden Moss over the bell..."),
					say("Koro", "The resonance and materials will produce a sound that targets the soul with magical vibrations..."),
					say("Koro", "Warming your soul and healing it from wounds."),
					say("Koro", "I praise you dear adventurer, you have gone far... You are just like my son..."),
					say("Koro", "Here! Have it!"),
					complete())),
			new Chapter("Koro the Master Craftsman [Final Chapter]", 10, false, ModKoro.KORO_WHAKAMATE, "koro/final", List.of(
					say("Koro", "Dear adventurer, I'm grateful to see you again!"),
					say("Koro", "You see, I have been reading..."),
					say("Koro", "Reading and reading..."),
					say("Koro", "And reading..."),
					say("Koro", "And I realized out of the blue..."),
					say("Koro", "The animals you fight..."),
					say("Koro", "Are spiritual animals!"),
					say("Koro", "I have been thinking with my newly acquired knowledge, and I think at least the bigger one, the wolf..."),
					say("Koro", "Has to have a core or nucleus of some sorts that channels its spirit into our reality."),
					say("Koro", "I don't know where they come from, but for them to be that powerful..."),
					say("Koro", "They have to be connected to a strong magical \"something\"..."),
					say("Koro", "Maybe if you slay enough of them... Just maybe... You will find one that has a healthy core, one that is not destroyed by the fight."),
					say("Koro", "Just believe in me..."),
					say("Koro", "It is a hunch... But it is one I am sure about..."),
					say("Koro", "I feel like I am not even the one that came up with it..."),
					quest("Bring Koro what he asked.", List.of(Cost.of(ModItems.SPIRITUAL_CATALYST, 1))),
					say("Koro", "You have done it..."),
					say("Koro", "I was right..."),
					say("Koro", "I could have never achieved something like this without you..."),
					say("Koro", "I thank you dear adventurer... I really do..."),
					say("Koro", "I have some materials I have gathered myself for this moment, I knew something was drawing me to this moment..."),
					say("Koro", "This is my gift to you dear adventurer!"),
					say("Koro", "I thank you for the gifts you have given me..."),
					say("Koro", "Your trust is enough for me to feel better..."),
					say("Koro", "It makes me feel like someone, not just an old spirit that died in a horrible way..."),
					say("Koro", "Being able to come up with such an otherworldly idea and create it makes me feel _whole_..."),
					say("Koro", "Dear adventurer, please remember me..."),
					say("Koro", "Thank you %player%"),
					complete())));

	/** Whether the player may start chapter index (0-3). */
	public static boolean canStart(int index, HuntData.Progress progress) {
		if (index >= CHAPTERS.size()) {
			return false;
		}
		Chapter chapter = CHAPTERS.get(index);
		return progress.rank() >= chapter.rank() && (!chapter.needsTierFour() || progress.bossKills[3] > 0);
	}
}
