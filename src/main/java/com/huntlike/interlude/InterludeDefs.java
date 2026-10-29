package com.huntlike.interlude;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.Items;

import com.huntlike.hunt.HuntData;
import com.huntlike.hunt.HuntDefs;
import com.huntlike.hunt.HuntDefs.Ability;
import com.huntlike.hunt.HuntDefs.Drop;

/**
 * Interludes: optional boss fights between two Hunts, on the menu rail between their symbols. They give no Hunt XP and
 * Daily Quest charges don't apply; they're paid with Interlude Tokens (1 gold ingot = 16). The first one is the Slime
 * Rider: Bartholomew (a slime prince) riding Pom (a horse with undead magic).
 */
public final class InterludeDefs {
	private InterludeDefs() {
	}

	public static final String SLIME_RIDER = "slime_rider";
	/** The Interlude's name color on the menu. */
	public static final int COLOR = 0x8FCF5A;
	public static final int TOKENS_PER_GOLD = 16;
	public static final int BULK_GOLD = 10;
	public static final String NAME = "Bartholomew and Pom";
	public static final String TITLE = "The Slime Rider and Mount";

	/** One stage (the Interlude's "tier"): health and melee of Phase 1, cost, the Ranks it needs, and its lore. */
	public record Stage(int number, int health, int melee, int tokenCost, int animalRank, int clusterRank, String lore) {
		public String roman() {
			return HuntDefs.ROMAN[number];
		}

		public int color() {
			return HuntDefs.tierColor(number * 2 - 1);
		}
	}

	public static final List<Stage> STAGES = List.of(
			new Stage(1, 50, 2, 2, 3, 0, "Bonds between creatures of different species are scarce. The bond between some of them can "
					+ "notably improve the quality of life of both species."),
			new Stage(2, 80, 3, 2, 6, 3, "One of the rarest types of bonds between species are agreements. Bonds that aren't made "
					+ "for survival purposes or biological benefits but for motivational purposes, making themselves more like "
					+ "companions in a journey in which they don't even speak the same language."),
			new Stage(3, 140, 4, 2, 6, 6, "In the case of Bartholomew and Pom, they are mobs that share the will to fight! They look "
					+ "for worthy opponents and attempt to win combat no matter the level of unfairness that is presented. They "
					+ "always carry resources, as Bartholomew is a slime prince and Pom is a horse with undead magic capacities, "
					+ "so they won't think twice on giving you great items to escape and have another attempt at adventuring!"));

	/** Kill quota per stage: lured animals count 1, slime clusters 1, magma cube clusters 2. */
	public static final int[] QUOTAS = {0, 2, 4, 6};

	/**
	 * The Interlude as the Hunt its quests run under (quest.hunt = "slime_rider"), so the quest flow (Spirit Lure, quota,
	 * summoning, failing) is the same as a Hunt's. Never listed among the Hunts.
	 */
	private static HuntDefs.Hunt hunt;

	public static HuntDefs.Hunt hunt() {
		if (hunt == null) {
			List<HuntDefs.Tier> tiers = new ArrayList<>();
			for (Stage stage : STAGES) {
				tiers.add(new HuntDefs.Tier(stage.number(), NAME, TITLE, ModInterlude.POM, 1.0F, stage.health(), stage.melee(), 0, 0.0F,
						QUOTAS[stage.number()], stage.tokenCost(), 0, stage.animalRank(), abilities(stage.number()), ""));
			}
			hunt = new HuntDefs.Hunt(SLIME_RIDER, "Interlude", COLOR, net.minecraft.world.BossEvent.BossBarColor.GREEN, tiers, List.of());
		}
		return hunt;
	}

	public static boolean isInterlude(HuntDefs.Hunt h) {
		return h != null && SLIME_RIDER.equals(h.id());
	}

	public static boolean isInterlude(String id) {
		return SLIME_RIDER.equals(id);
	}

	public static Stage stage(int number) {
		return STAGES.get(Math.max(1, Math.min(STAGES.size(), number)) - 1);
	}

	/** The Interlude opens with Stage I (Animal Rank III). */
	public static boolean open(HuntData data) {
		return ranksMet(data, STAGES.get(0));
	}

	public static boolean ranksMet(HuntData data, Stage stage) {
		return data.progress("animal").rank() >= stage.animalRank() && data.progress("cluster").rank() >= stage.clusterRank();
	}

	/** A stage can be started once its Ranks are reached and the stage before it was won. */
	public static boolean stageOpen(HuntData data, Stage stage) {
		return ranksMet(data, stage) && (stage.number() == 1 || data.progress(SLIME_RIDER).bossKills[stage.number() - 2] > 0);
	}

	/** "Animal Rank VI + Cluster Rank III". */
	public static String rankText(Stage stage) {
		String text = "Animal Rank " + HuntDefs.ROMAN[stage.animalRank()];
		return stage.clusterRank() > 0 ? text + " + Cluster Rank " + HuntDefs.ROMAN[stage.clusterRank()] : text;
	}

	public static final Ability TWIN_TACKLE = new Ability("Twin Tackle", "Deals damage on impact.");
	public static final Ability SUPPORTING_TURRETS = new Ability("Supporting Turrets", "Slimeballs and eggs are thrown at you.");
	public static final Ability FRONT_CHARGE = new Ability("Front Charge", "Deals damage on impact.");
	public static final Ability PAINFUL_KICK = new Ability("Painful Kick", "Kicks the player and stuns for a second.");
	public static final Ability ULTIMATE_TECHNIQUE = new Ability("Ultimate Technique", "Such a Bizarre technique...");
	public static final Ability LUNGE = new Ability("Lunge!", "Deals damage on impact.");
	public static final Ability EWW = new Ability("Eww...", "Look away! Or you'll have to close your eyes.");
	public static final Ability FOR_REAL = new Ability("Are we for real?", "Does a very slimy thing...");

	/**
	 * The abilities a stage uses, as the boss card lists them: "Ability (Phase I)" for the pair, "Ability - Who (Phase II)"
	 * for the one who stays. The phases themselves aren't listed as abilities.
	 */
	public static List<Ability> abilities(int stage) {
		List<Ability> list = new ArrayList<>();
		list.add(label(TWIN_TACKLE, null, 1));
		if (stage >= 2) {
			list.add(label(SUPPORTING_TURRETS, null, 1));
		}
		list.add(label(FRONT_CHARGE, "Pom", 2));
		if (stage >= 2) {
			list.add(label(PAINFUL_KICK, "Pom", 2));
		}
		if (stage >= 3) {
			list.add(label(ULTIMATE_TECHNIQUE, "Pom", 2));
		}
		list.add(label(LUNGE, "Bartholomew", 2));
		if (stage >= 2) {
			list.add(label(EWW, "Bartholomew", 2));
		}
		if (stage >= 3) {
			list.add(label(FOR_REAL, "Bartholomew", 2));
		}
		return list;
	}

	private static Ability label(Ability ability, String who, int phase) {
		return new Ability(ability.name() + (who == null ? "" : " - " + who) + " (Phase " + HuntDefs.ROMAN[phase] + ")",
				ability.description());
	}

	/** Drops when Pom (the Equus) is the one left: x stage each; 1/5 Lucky, rarer Good, 1/3 no message. */
	public static List<Drop> pomDrops(int stage) {
		List<Drop> drops = new ArrayList<>();
		drops.add(new Drop("bone", Items.BONE, stage, stage, 1, 3, 0));
		drops.add(new Drop("apple", Items.APPLE, stage, stage, 1, 3, 0));
		drops.add(new Drop("leather", Items.LEATHER, stage, stage, 1, 3, 0));
		if (stage >= 2) {
			drops.add(new Drop("saddle", Items.SADDLE, stage, stage, 1, 5, 1));
		}
		// Pom's Skeletal Remains: 1/35 at Stage II, 1/12.5 at Stage III.
		if (stage == 2) {
			drops.add(new Drop("poms_skeletal_remains", ModInterlude.POMS_SKELETAL_REMAINS, 1, 1, 1, 35, 2));
		} else if (stage >= 3) {
			drops.add(new Drop("poms_skeletal_remains", ModInterlude.POMS_SKELETAL_REMAINS, 1, 1, 2, 25, 2));
		}
		return drops;
	}

	/** Drops when Bartholomew is the one left. The Potion of Leaping is a 3-minute one. */
	public static List<Drop> bartholomewDrops(int stage) {
		List<Drop> drops = new ArrayList<>();
		drops.add(new Drop("slime_ball", Items.SLIME_BALL, stage, stage, 1, 3, 0));
		drops.add(new Drop("ice", Items.ICE, stage, stage, 1, 3, 0));
		drops.add(new Drop("iron_ingot", Items.IRON_INGOT, stage, stage, 1, 3, 0));
		if (stage >= 2) {
			drops.add(new Drop("leaping_potion", Items.POTION, stage, stage, 1, 5, 1));
		}
		// Horseshoe: 1/20 at Stage II, 1/7 at Stage III. Bouncy Slime Ball: 1/20 at Stage II, 1/8 at Stage III.
		if (stage == 2) {
			drops.add(new Drop("horseshoe", ModInterlude.HORSESHOE, 1, 1, 1, 20, 1));
			drops.add(new Drop("bouncy_slime_ball", ModInterlude.BOUNCY_SLIME_BALL, 1, 1, 1, 20, 1));
		} else if (stage >= 3) {
			drops.add(new Drop("horseshoe", ModInterlude.HORSESHOE, 1, 1, 1, 7, 1));
			drops.add(new Drop("bouncy_slime_ball", ModInterlude.BOUNCY_SLIME_BALL, 1, 1, 1, 8, 1));
		}
		// Growing Slime Ball: 1/5 at Stage I, 1/4 at Stage II, 1/2.5 at Stage III.
		drops.add(switch (stage) {
			case 1 -> new Drop("growing_slime_ball", ModInterlude.GROWING_SLIME_BALL, 1, 1, 1, 5, 1);
			case 2 -> new Drop("growing_slime_ball", ModInterlude.GROWING_SLIME_BALL, 1, 1, 1, 4, 1);
			default -> new Drop("growing_slime_ball", ModInterlude.GROWING_SLIME_BALL, 1, 1, 2, 5, 1);
		});
		return drops;
	}
}
