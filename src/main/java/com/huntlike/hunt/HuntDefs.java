package com.huntlike.hunt;

import java.util.List;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import com.huntlike.entity.ModEntities;
import com.huntlike.gear.ModGear;
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
	public record Hunt(String id, String name, int color, net.minecraft.world.BossEvent.BossBarColor barColor, List<Tier> tiers,
			List<List<Unlock>> unlocks) {
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

	public static final Hunt ANIMAL = new Hunt("animal", "Animal", LIME, net.minecraft.world.BossEvent.BossBarColor.GREEN,
			List.of(
					new Tier(1, "Sylviornis", "The Giant Galliforme", ModEntities.SYLVIORNIS, 1.5F, 50, 1, 1, 0.25F, 2, 2, 5, 0,
							List.of(EGG_BLASTER), "lore.huntlike.animal.1"),
					new Tier(2, "Hylochoerus", "The Aberrant Souleater", ModEntities.HYLOCHOERUS, 1.25F, 80, 1, 2, 0.5F, 4, 4, 20, 2,
							List.of(EGG_BLASTER, SOUL_STRIKE), "lore.huntlike.animal.2"),
					new Tier(3, "Ovis Ammon", "The Horned Creation", ModEntities.OVIS_AMMON, 1.25F, 120, 2, 3, 1.0F, 6, 8, 75, 4,
							List.of(EGG_BLASTER, SOUL_STRIKE, ANIMALIA_LUNGE), "lore.huntlike.animal.3"),
					new Tier(4, "Aurochs", "The Animal Mother", ModEntities.AUROCHS, 1.25F, 170, 2, 4, 2.0F, 8, 16, 250, 6,
							List.of(EGG_BLASTER, SOUL_STRIKE, ANIMALIA_LUNGE, HERD_CALL), "lore.huntlike.animal.4"),
					new Tier(5, "Epicyon Haydeni", "The Bone Crusher", ModEntities.EPICYON_HAYDENI, 2.0F, 240, 3, 5, 2.0F, 10, 32, 800, 8,
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

	/** One cost line: an item and how many. */
	public record Cost(Item item, int count) {
		static Cost of(Item item, int count) {
			return new Cost(item, count);
		}
	}

	/**
	 * A Gear-tab trade. The first result.size() costs are the gear being upgraded (one item each, in the same order as
	 * result), so each new item keeps that item's enchantments.
	 */
	public record GearTrade(String name, int rank, List<Cost> cost, List<Item> result) {
	}

	private static List<Cost> set(ModGear.ArmorSet set) {
		return List.of(Cost.of(set.helmet(), 1), Cost.of(set.chestplate(), 1), Cost.of(set.leggings(), 1), Cost.of(set.boots(), 1));
	}

	private static List<Cost> join(List<Cost> first, Cost... rest) {
		java.util.ArrayList<Cost> list = new java.util.ArrayList<>(first);
		list.addAll(List.of(rest));
		return list;
	}

	public static final List<GearTrade> ANIMAL_ARMOR = List.of(
			new GearTrade("Rotten Armor", 2, join(List.of(Cost.of(Items.LEATHER_HELMET, 1), Cost.of(Items.LEATHER_CHESTPLATE, 1),
					Cost.of(Items.LEATHER_LEGGINGS, 1), Cost.of(Items.LEATHER_BOOTS, 1)), Cost.of(Items.OAK_LOG, 32),
					Cost.of(Items.OAK_LEAVES, 32), Cost.of(ModItems.DECENT_QUALITY_MYSTERY_MEAT, 4)), ModGear.ROTTEN.pieces()),
			new GearTrade("Carrion Armor", 4, join(set(ModGear.ROTTEN), Cost.of(ModItems.DECENT_QUALITY_MYSTERY_MEAT, 4),
					Cost.of(ModItems.THICK_HIDE, 2), Cost.of(Items.LEATHER, 16), Cost.of(Items.BONE, 16)), ModGear.CARRION.pieces()),
			new GearTrade("Ecocide Armor", 6, join(set(ModGear.CARRION), Cost.of(ModItems.HIGH_QUALITY_MYSTERY_MEAT, 2),
					Cost.of(ModItems.THICK_HIDE, 2), Cost.of(Items.OAK_LOG, 32), Cost.of(Items.DIAMOND, 4)), ModGear.ECOCIDE.pieces()));

	public static final List<GearTrade> ANIMAL_WEAPONS = List.of(
			new GearTrade("Skin Knife [Genesis]", 1, List.of(Cost.of(Items.IRON_SWORD, 1),
					Cost.of(ModItems.LOW_QUALITY_MYSTERY_MEAT, 5)), List.of(ModGear.SKIN_KNIFE)),
			new GearTrade("Meat Cutter [Act I]", 3, List.of(Cost.of(ModGear.SKIN_KNIFE, 1),
					Cost.of(ModItems.DECENT_QUALITY_MYSTERY_MEAT, 2), Cost.of(Items.IRON_INGOT, 5)), List.of(ModGear.MEAT_CUTTER)),
			new GearTrade("Limb Severer [Act II]", 5, List.of(Cost.of(ModGear.MEAT_CUTTER, 1),
					Cost.of(ModItems.HIGH_QUALITY_MYSTERY_MEAT, 1), Cost.of(Items.DIAMOND, 2)), List.of(ModGear.LIMB_SEVERER)),
			new GearTrade("Bone Breaker [Act III]", 7, List.of(Cost.of(ModGear.LIMB_SEVERER, 1), Cost.of(ModItems.LARGE_FEMUR, 1),
					Cost.of(Items.SOUL_SAND, 8)), List.of(ModGear.BONE_BREAKER)),
			new GearTrade("Bone Pulverizer [Apotheosis]", 9, List.of(Cost.of(ModGear.BONE_BREAKER, 1), Cost.of(ModItems.LARGE_FEMUR, 1),
					Cost.of(Items.BONE_MEAL, 12), Cost.of(Items.OBSIDIAN, 8)), List.of(ModGear.BONE_PULVERIZER)));

	/** Key stored in a player's seen list once they've obtained a piece of Hunt gear. */
	public static String gearSeenId(Item item) {
		return "gear:" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getPath();
	}

	/** The Rank that unlocks the trade giving this gear, or -1 if it isn't Hunt gear. */
	public static int gearRank(Item item) {
		for (List<GearTrade> list : List.of(ANIMAL_ARMOR, ANIMAL_WEAPONS)) {
			for (GearTrade trade : list) {
				if (trade.result().contains(item)) {
					return trade.rank();
				}
			}
		}
		return -1;
	}

	/** Trades by page: 0 armor, 2 weapons (page 1 is Misc). */
	public static List<GearTrade> trades(int page) {
		return page == 0 ? ANIMAL_ARMOR : ANIMAL_WEAPONS;
	}

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

	/**
	 * A rare drop's odds denominator for num/den odds. The Mystery Concoction (Animal Hunt only) turns "1 in X" into
	 * "1 in X-1": 1/10 becomes 1/9, and 2/25 (1 in 12.5) becomes 2/23 (1 in 11.5).
	 */
	public static int odds(String hunt, int num, int den, boolean concoction) {
		return concoction && "animal".equals(hunt) ? Math.max(num, den - num) : den;
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
