package com.huntlike.hunt;

import java.util.List;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;

import com.huntlike.registry.ModItems;

/** Static design data for every Hunt: ranks, tiers, bosses and drops. Shared by server and client. */
public final class HuntDefs {
	private HuntDefs() {
	}

	public static final int LIME = 0x55FF55;
	public static final int PREFIX = 0xFF55FF;
	public static final int AQUA = 0x55FFFF;
	public static final int DARK_GREEN = 0x00AA00;
	public static final int DARK_RED = 0xAA0000;
	public static final int RED = 0xFF5555;
	public static final int GOLD = 0xFFAA00;
	public static final int DARK_PURPLE = 0xAA00AA;
	public static final int GRAY = 0xAAAAAA;
	public static final int LIGHT_BLUE = 0x8FD3FF;
	public static final int PINK = 0xFF8AD8;
	public static final int BLUE = 0x5555FF;
	/** Experience orb green. */
	public static final int XP_GREEN = 0x80FF20;

	public static final String[] RANK_TITLES = {"Prey", "Scout", "Tracker", "Stalker", "Trapper", "Pursuer", "Warden",
			"Predator", "Apex Hunter", "Dread Hunter", "Slayer"};
	public static final int[] RANK_XP = {0, 60, 150, 390, 750, 1650, 3000, 6000, 10500, 26500, 50500};
	public static final String[] ROMAN = {"0", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
	public static final int MAX_RANK = 10;

	/** One line in a Rank's unlock list. Color is an RGB value. */
	public record Unlock(String text, int color) {
		static Unlock plain(String text) {
			return new Unlock(text, 0xFFFFFF);
		}
	}

	/**
	 * A boss drop, with its chance as numerator / denominator (1/1 = guaranteed).
	 * dropClass picks the chat message: 0 none (guaranteed), 1 Lucky, 2 Good, 3 GREAT, 4 FANTASTIC.
	 * Several drops of a Hunt may share a class.
	 */
	public record Drop(String id, Item item, int minCount, int maxCount, int chanceNum, int chanceDen, int dropClass) {
		public boolean guaranteed() {
			return chanceNum >= chanceDen;
		}
	}

	/** A boss ability as players see it. */
	public record Ability(String name, String description) {
	}

	/** One tier of a Hunt: its boss and its rules. */
	public record Tier(int number, String bossName, String bossTitle, EntityType<? extends Mob> mob, float scale,
			int health, int melee, int regen, float eggDamage, int quota, int tokenCost, int xp, int unlockRank,
			List<Ability> abilities, String loreKey) {
		public String roman() {
			return ROMAN[number];
		}

		/** Name tag and aura color of this tier. */
		public int color() {
			return HuntDefs.tierColor(number);
		}
	}

	/** A Hunt type. Only Animal exists for now. */
	public record Hunt(String id, String name, int color, List<Tier> tiers, List<List<Unlock>> unlocks) {
		public Tier tier(int number) {
			return tiers.get(number - 1);
		}

		public String rankName(int level) {
			return name + " " + RANK_TITLES[level];
		}
	}

	public static int tierColor(int tier) {
		return tier <= 2 ? RED : tier <= 4 ? DARK_RED : GOLD;
	}

	public static final Ability EGG_BLASTER = new Ability("Egg Blaster", "Eggs are shot, dealing damage constantly.");
	public static final Ability SOUL_STRIKE = new Ability("Soul Strike", "A spiritual lash that targets your soul.");
	public static final Ability ANIMALIA_LUNGE = new Ability("Animalia Lunge",
			"A straight charge with heavy knockback and greater damage.");
	public static final Ability HERD_CALL = new Ability("Herd Call",
			"Spawns supporting species that prevent damage to the boss until they are dealt with.");
	public static final Ability APEX_INSTINCT = new Ability("Apex Instinct",
			"After being wounded many times, survival instincts consume the canine body, increasing speed and damage.");

	private static final String KORO_FIRST = "A soul from the unknown territories is here to talk! [Gear]";
	private static final String KORO_NEXT = "The soul of Koro thinks you are ready for his next quest!";

	public static final Hunt ANIMAL = new Hunt("animal", "Animal", LIME,
			List.of(
					new Tier(1, "Sylviornis", "The Giant Galliforme", EntityTypes.CHICKEN, 1.5F, 50, 1, 1, 0.25F, 4, 2, 5, 0,
							List.of(EGG_BLASTER), "lore.huntlike.animal.1"),
					new Tier(2, "Hylochoerus", "The Aberrant Souleater", EntityTypes.PIG, 1.25F, 80, 1, 2, 0.5F, 6, 4, 20, 2,
							List.of(EGG_BLASTER, SOUL_STRIKE), "lore.huntlike.animal.2"),
					new Tier(3, "Ovis Ammon", "The Horned Creation", EntityTypes.SHEEP, 1.25F, 120, 2, 3, 1.0F, 8, 8, 75, 4,
							List.of(EGG_BLASTER, SOUL_STRIKE, ANIMALIA_LUNGE), "lore.huntlike.animal.3"),
					new Tier(4, "Aurochs", "The Animal Mother", EntityTypes.COW, 1.25F, 170, 2, 4, 2.0F, 10, 16, 250, 6,
							List.of(EGG_BLASTER, SOUL_STRIKE, ANIMALIA_LUNGE, HERD_CALL), "lore.huntlike.animal.4"),
					new Tier(5, "Epicyon Haydeni", "The Bone Crusher", EntityTypes.WOLF, 2.0F, 240, 3, 5, 2.0F, 12, 32, 800, 8,
							List.of(EGG_BLASTER, SOUL_STRIKE, ANIMALIA_LUNGE, HERD_CALL, APEX_INSTINCT),
							"lore.huntlike.animal.5")),
			List.of(
					List.of(Unlock.plain("Tier I Animal Quests")),
					List.of(Unlock.plain("Hunt Weapon: Skin Knife [Genesis]")),
					List.of(Unlock.plain("Tier II Animal Quests"), Unlock.plain("Hunt Armor: Rotten Armor")),
					List.of(Unlock.plain("Hunt Weapon upgrade: Meat Cutter [Act I]"), Unlock.plain("Next Hunt type"),
							new Unlock(KORO_FIRST, DARK_PURPLE)),
					List.of(Unlock.plain("Tier III Animal Quests"), Unlock.plain("Hunt Armor upgrade: Carrion Armor")),
					List.of(Unlock.plain("Hunt Weapon upgrade: Limb Severer [Act II]")),
					List.of(Unlock.plain("Tier IV Animal Quests"), Unlock.plain("Hunt Armor upgrade: Ecocide Armor"),
							new Unlock(KORO_NEXT, DARK_PURPLE)),
					List.of(Unlock.plain("Hunt Weapon upgrade: Bone Breaker [Act III]")),
					List.of(Unlock.plain("Tier V Animal Quests")),
					List.of(Unlock.plain("Hunt Weapon upgrade: Bone Pulverizer [Apotheosis]"), new Unlock(KORO_NEXT, DARK_PURPLE)),
					List.of(new Unlock(KORO_NEXT, DARK_PURPLE))));

	public static final List<Hunt> HUNTS = List.of(ANIMAL);

	/** A Gear-tab trade for Hunt armor or a Hunt weapon. */
	public record GearTrade(String name, int rank, String cost) {
	}

	public static final List<GearTrade> ANIMAL_ARMOR = List.of(
			new GearTrade("Rotten Armor", 2,
					"Leather helmet, chestplate, leggings and boots + 32 Oak Logs + 32 Oak Leaves + 4 Decent Quality Mystery Meat"),
			new GearTrade("Carrion Armor", 4,
					"Rotten Armor + 4 Decent Quality Mystery Meat + 2 Thick Hide + 16 Leather + 16 Bones"),
			new GearTrade("Ecocide Armor", 6,
					"Carrion Armor + 2 High Quality Mystery Meat + 2 Thick Hide + 32 Oak Logs + 4 Diamonds"));

	public static final List<GearTrade> ANIMAL_WEAPONS = List.of(
			new GearTrade("Skin Knife [Genesis]", 1, "Iron Sword + 5 Low Quality Mystery Meat"),
			new GearTrade("Meat Cutter [Act I]", 3, "Skin Knife + 2 Decent Quality Mystery Meat + 5 Iron Ingots"),
			new GearTrade("Limb Severer [Act II]", 5, "Meat Cutter + 1 High Quality Mystery Meat + 2 Diamonds"),
			new GearTrade("Bone Breaker [Act III]", 7, "Limb Severer + 1 Large Femur + 8 Soul Sand"),
			new GearTrade("Bone Pulverizer [Apotheosis]", 9, "Bone Breaker + 1 Large Femur + 12 Bone Meal + 8 Obsidian"));

	/** Ranks that unlock each chapter of the soul's questline (Koro for Animal). */
	public static final int[] QUESTLINE_RANKS = {3, 6, 9, 10};

	public static Hunt hunt(String id) {
		for (Hunt hunt : HUNTS) {
			if (hunt.id().equals(id)) {
				return hunt;
			}
		}
		return ANIMAL;
	}

	public static int rankForXp(int xp) {
		int level = 0;
		for (int i = 0; i <= MAX_RANK; i++) {
			if (xp >= RANK_XP[i]) {
				level = i;
			}
		}
		return level;
	}

	/** Boss drops for a tier of the Animal Hunt, owner odds. */
	public static List<Drop> drops(Tier tier) {
		int t = tier.number();
		boolean five = t == 5;
		java.util.ArrayList<Drop> list = new java.util.ArrayList<>();
		list.add(new Drop("low_quality_mystery_meat", ModItems.LOW_QUALITY_MYSTERY_MEAT, t * 2, t * 3, 1, 1, 0));
		if (t >= 2) {
			list.add(new Drop("thick_hide", ModItems.THICK_HIDE, 1, 1, 1, five ? 5 : 10, 1));
		}
		if (t >= 3) {
			list.add(new Drop("large_femur", ModItems.LARGE_FEMUR, 1, 1, five ? 2 : 1, 25, 2));
		}
		if (t >= 4) {
			list.add(new Drop("golden_moss", ModItems.GOLDEN_MOSS, 1, 1, 1, five ? 25 : 50, 3));
		}
		if (t >= 5) {
			list.add(new Drop("spiritual_catalyst", ModItems.SPIRITUAL_CATALYST, 1, 1, 1, 100, 4));
		}
		return list;
	}

}
