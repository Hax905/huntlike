package com.huntlike.client.jei;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import com.huntlike.HuntLike;
import com.huntlike.gear.ModKoro;
import com.huntlike.gear.ModShields;
import com.huntlike.hunt.HuntDefs;
import com.huntlike.hunt.HuntDefs.Cost;
import com.huntlike.hunt.HuntDefs.Drop;
import com.huntlike.hunt.HuntDefs.GearTrade;
import com.huntlike.hunt.KoroScript;
import com.huntlike.interlude.InterludeDefs;
import com.huntlike.interlude.ModInterlude;
import com.huntlike.interlude.RemainsItem;
import com.huntlike.registry.ModBlocks;
import com.huntlike.registry.ModItems;
import com.huntlike.towernaar.TowernaarManager;
import com.huntlike.towernaar.TowernaarScript;

/**
 * JEI (and EMI, which loads JEI plugins) support: the two anvil uses, every boss and Interlude drop with its odds, every
 * Gear tab, Koro, Towernaar and PRD trade, and info pages for items whose use isn't a recipe. Boss names stay hidden
 * (tiers only), like in the Hunt menu before a boss is slain.
 */
public final class HuntLikeJeiPlugin implements IModPlugin {
	private static final String[] CLASS_NAMES = {"Guaranteed", "Lucky", "Good", "GREAT", "FANTASTIC"};
	private static final int[] CLASS_COLORS = {0xAAAAAA, 0x55FF55, 0x55FFFF, 0xFFAA00, 0xFF55FF};

	@Override
	public Identifier getPluginUid() {
		return Identifier.fromNamespaceAndPath(HuntLike.MOD_ID, "jei_plugin");
	}

	/** JEI's runtime once it's up (null without JEI), so the client test can open the pages. */
	private static mezz.jei.api.runtime.IJeiRuntime runtime;

	@Override
	public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime jeiRuntime) {
		runtime = jeiRuntime;
	}

	@Override
	public void onRuntimeUnavailable() {
		runtime = null;
	}

	/** Opens JEI on the drops (0) or trades (1) pages; false if JEI isn't running. Must run on the client thread. */
	public static boolean showForTest(int page) {
		if (runtime == null) {
			return false;
		}
		runtime.getRecipesGui().showTypes(List.of(page == 0 ? DropsCategory.TYPE : TradesCategory.TYPE));
		return true;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		var gui = registration.getJeiHelpers().getGuiHelper();
		registration.addRecipeCategories(new DropsCategory(gui), new TradesCategory(gui));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalysts(DropsCategory.TYPE, ModBlocks.ALPHA_ALTAR);
		registration.addRecipeCatalysts(TradesCategory.TYPE, ModBlocks.ALPHA_ALTAR);
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(DropsCategory.TYPE, drops());
		registration.addRecipes(TradesCategory.TYPE, trades());
		registration.addRecipes(RecipeTypes.ANVIL, anvil(registration.getVanillaRecipeFactory()));
		info(registration);
	}

	// ---- Drops ----

	public static List<DropsCategory.Entry> drops() {
		List<DropsCategory.Entry> list = new ArrayList<>();
		for (HuntDefs.Hunt hunt : HuntDefs.HUNTS) {
			boolean cluster = HuntDefs.isCluster(hunt);
			String luck = cluster ? "Warm and Slimy Beef Jerky" : "Mystery Concoction";
			for (HuntDefs.Tier tier : hunt.tiers()) {
				list.add(new DropsCategory.Entry(hunt.name() + " Hunt - Tier " + tier.roman(), hunt.color(),
						slots(hunt.id(), HuntDefs.drops(tier), luck), "Rare odds: " + luck + " and Daily Quests improve them"));
			}
		}
		for (InterludeDefs.Stage stage : InterludeDefs.STAGES) {
			int n = stage.number();
			String title = "Interlude - Stage " + HuntDefs.ROMAN[n];
			list.add(new DropsCategory.Entry(title + " (Pom stays)", InterludeDefs.COLOR,
					slots("animal", InterludeDefs.pomDrops(n), "Mystery Concoction"), "Rare odds: the Mystery Concoction improves them"));
			list.add(new DropsCategory.Entry(title + " (Bartholomew stays)", InterludeDefs.COLOR,
					slots("cluster", InterludeDefs.bartholomewDrops(n), "Warm and Slimy Beef Jerky"),
					"Rare odds: the Beef Jerky improves them"));
		}
		return list;
	}

	private static List<DropsCategory.Slot> slots(String hunt, List<Drop> drops, String luck) {
		List<DropsCategory.Slot> slots = new ArrayList<>();
		for (Drop drop : drops) {
			ItemStack stack = new ItemStack(drop.item(), Math.max(1, drop.maxCount()));
			if (drop.id().equals("leaping_potion")) {
				stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.LEAPING));
			}
			List<Component> lines = new ArrayList<>();
			String count = drop.minCount() == drop.maxCount() ? "x" + drop.minCount() : "x" + drop.minCount() + "-" + drop.maxCount();
			if (drop.guaranteed()) {
				lines.add(Component.literal("Guaranteed, " + count).withColor(CLASS_COLORS[0]));
			} else {
				int cls = Math.max(0, Math.min(4, drop.dropClass()));
				String label = cls == 0 ? "No message" : CLASS_NAMES[cls];
				lines.add(Component.literal("Chance: " + odds(drop.chanceNum(), drop.chanceDen()) + ", " + count)
						.withColor(CLASS_COLORS[cls]));
				lines.add(Component.literal("Drop class: " + label).withColor(0xAAAAAA));
				int luckDen = HuntDefs.odds(hunt, drop.chanceNum(), drop.chanceDen(), true);
				if (luckDen != drop.chanceDen()) {
					lines.add(Component.literal("With the " + luck + ": " + odds(drop.chanceNum(), luckDen)).withColor(0xAAAAAA));
				}
			}
			slots.add(new DropsCategory.Slot(stack, lines));
		}
		return slots;
	}

	/** "1/25", or "2/25 (1 in 12.5)" when the numerator isn't 1. */
	static String odds(int num, int den) {
		if (num == 1) {
			return "1/" + den;
		}
		double inX = den / (double) num;
		String x = inX == Math.floor(inX) ? Integer.toString((int) inX) : String.format(java.util.Locale.ROOT, "%.1f", inX);
		return num + "/" + den + " (1 in " + x + ")";
	}

	// ---- Trades ----

	public static List<TradesCategory.Entry> trades() {
		List<TradesCategory.Entry> list = new ArrayList<>();
		for (HuntDefs.Hunt hunt : HuntDefs.HUNTS) {
			boolean cluster = HuntDefs.isCluster(hunt);
			for (int page = 0; page < 3; page++) {
				for (GearTrade trade : HuntDefs.trades(hunt, page)) {
					String when;
					if (trade.chapter() == HuntDefs.WAND_CHAPTER) {
						when = "After using the Magnetic Wand PRD";
					} else if (trade.chapter() >= 0) {
						when = "After " + (cluster ? "Towernaar's" : "Koro's") + " Chapter " + chapterName(trade.chapter(),
								cluster ? TowernaarScript.CHAPTERS.size() : KoroScript.CHAPTERS.size());
					} else if (trade.rank() > 0) {
						when = "Needs " + hunt.name() + " Rank " + HuntDefs.ROMAN[trade.rank()];
					} else {
						when = "Always on sale";
					}
					List<ItemStack> result = new ArrayList<>();
					for (Item item : trade.result()) {
						result.add(new ItemStack(item));
					}
					list.add(new TradesCategory.Entry(hunt.name() + " Gear: " + trade.name(), hunt.color(), stacks(trade.cost()),
							result, when));
				}
			}
		}
		for (KoroScript.Chapter chapter : KoroScript.CHAPTERS) {
			List<Cost> costs = new ArrayList<>();
			for (KoroScript.Step step : chapter.steps()) {
				costs.addAll(step.items());
			}
			list.add(new TradesCategory.Entry(chapter.title(), HuntDefs.ANIMAL.color(), stacks(costs),
					List.of(new ItemStack(chapter.reward())), "Koro, Animal Rank " + HuntDefs.ROMAN[chapter.rank()]
							+ (chapter.needsTierFour() ? " + a Tier IV kill" : "")));
		}
		for (TowernaarScript.Chapter chapter : TowernaarScript.CHAPTERS) {
			List<Cost> costs = new ArrayList<>();
			int kills = 0;
			boolean boss = false;
			for (TowernaarScript.Step step : chapter.steps()) {
				costs.addAll(step.items());
				kills += step.kills();
				boss |= step.kind() == TowernaarScript.Kind.BOSS;
			}
			String note = "Towernaar, Cluster Rank " + HuntDefs.ROMAN[chapter.rank()];
			if (kills > 0) {
				note += " + " + kills + " Rogue Hunter kills";
			}
			if (boss) {
				note += " + his fight";
			}
			list.add(new TradesCategory.Entry(chapter.title(), HuntDefs.CLUSTER.color(), stacks(costs),
					List.of(new ItemStack(chapter.reward())), note));
		}
		list.add(new TradesCategory.Entry("Towernaar: Magnetic Wand PRD", HuntDefs.CLUSTER.color(), stacks(TowernaarManager.PRD_COST),
				List.of(new ItemStack(ModShields.MAGNETIC_WAND_PRD)), "After the Final Chapter, until the PRD is used"));
		return list;
	}

	private static String chapterName(int index, int count) {
		return index >= count - 1 ? "Final" : HuntDefs.ROMAN[index + 1];
	}

	/** Costs as stacks, merging repeats of the same item. */
	private static List<ItemStack> stacks(List<Cost> costs) {
		Map<Item, Integer> merged = new LinkedHashMap<>();
		for (Cost cost : costs) {
			merged.merge(cost.item(), cost.count(), Integer::sum);
		}
		List<ItemStack> list = new ArrayList<>();
		merged.forEach((item, count) -> list.add(new ItemStack(item, count)));
		return list;
	}

	// ---- Anvil ----

	static List<IJeiAnvilRecipe> anvil(IVanillaRecipeFactory factory) {
		List<IJeiAnvilRecipe> list = new ArrayList<>();
		// Horseshoe on Pom's Skeletal Remains: one more star, up to 5.
		List<ItemStack> remains = new ArrayList<>();
		List<ItemStack> upgraded = new ArrayList<>();
		for (int stars = 0; stars < RemainsItem.MAX_STARS; stars++) {
			ItemStack left = new ItemStack(ModInterlude.POMS_SKELETAL_REMAINS);
			left.set(ModInterlude.HORSESHOES, stars);
			ItemStack out = new ItemStack(ModInterlude.POMS_SKELETAL_REMAINS);
			out.set(ModInterlude.HORSESHOES, stars + 1);
			remains.add(left);
			upgraded.add(out);
		}
		list.add(factory.createAnvilRecipe(remains, List.of(new ItemStack(ModInterlude.HORSESHOE)), upgraded,
				Identifier.fromNamespaceAndPath(HuntLike.MOD_ID, "anvil/horseshoe")));
		// Growing Slime Ball on a single-enchantment book: one level higher, up to the maximum.
		var level = Minecraft.getInstance().level;
		if (level != null) {
			level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements().forEach(holder -> {
				int max = holder.value().getMaxLevel();
				if (max < 2) {
					return;
				}
				List<ItemStack> books = new ArrayList<>();
				List<ItemStack> grown = new ArrayList<>();
				for (int lvl = 1; lvl < max; lvl++) {
					books.add(book(holder, lvl));
					grown.add(book(holder, lvl + 1));
				}
				String path = holder.key().identifier().getPath().replace('/', '_');
				list.add(factory.createAnvilRecipe(books, List.of(new ItemStack(ModInterlude.GROWING_SLIME_BALL)), grown,
						Identifier.fromNamespaceAndPath(HuntLike.MOD_ID, "anvil/growing_" + path)));
			});
		}
		return list;
	}

	private static ItemStack book(Holder<Enchantment> enchantment, int level) {
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		stored.set(enchantment, level);
		book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
		return book;
	}

	// ---- Info pages ----

	private static void info(IRecipeRegistration registration) {
		page(registration, ModBlocks.ALPHA_ALTAR.asItem(),
				"Right-click to open the Hunt menu: buy Ritual Tokens, start Hunting Quests and trade for gear.",
				"Ritual Tokens: 10 Logs buy 64 Animal tokens, 5 Soul Sand buy 64 Cluster tokens. Interlude Tokens: 1 Gold Ingot buys 16.",
				"Drops and trades from the Hunts are listed under the Alpha Altar's recipes.");
		page(registration, ModInterlude.GROWING_SLIME_BALL,
				"Dropped by Bartholomew in the Interlude.",
				"On an anvil, with an enchanted book that has exactly one enchantment: the book gains one level, up to the enchantment's maximum. Costs 10 levels.");
		page(registration, ModInterlude.POMS_SKELETAL_REMAINS,
				"Dropped by Pom in the Interlude, from Stage II.",
				"Right-click to call Pom's skeletal horse; right-click again to send it away.",
				"A Horseshoe on an anvil adds a star (up to 5). Each star makes the horse faster.");
		page(registration, ModInterlude.HORSESHOE,
				"Dropped by Bartholomew in the Interlude, from Stage II.",
				"On an anvil, adds a star to Pom's Skeletal Remains for 5 levels.");
		page(registration, ModInterlude.BOUNCY_SLIME_BALL,
				"Dropped by Bartholomew in the Interlude, from Stage II. Crafts the Slimy Rebounders.");
		page(registration, ModInterlude.SLIMY_REBOUNDERS,
				"Boots that bounce you back up after a fall of more than 1 block, and cancel fall damage.");
		page(registration, ModItems.MYSTERY_CONCOCTION,
				"Drink it to improve rare drop odds in the Animal Hunt and on Pom's side of the Interlude: \"1 in X\" becomes \"1 in X-1\".");
		page(registration, ModItems.WARM_AND_SLIMY_BEEF_JERKY,
				"Improves rare drop odds in the Cluster Hunt and on Bartholomew's side of the Interlude: \"1 in X\" becomes \"1 in X-1\".");
		page(registration, ModShields.MAGNETIC_WAND_PRD,
				"Bought from Towernaar after his Final Chapter. Using it unlocks the Magnetic Wand trade in the Cluster Gear tab.");
		page(registration, ModShields.MAGNETIC_WAND,
				"Hold right-click to cast a field that gathers nearby creatures to one spot.");
		page(registration, ModKoro.WOODEN_BELL, "Koro's Chapter I reward, then sold in the Animal Gear tab. Right-click to ring it.");
	}

	private static void page(IRecipeRegistration registration, Item item, String... lines) {
		Component[] components = new Component[lines.length];
		for (int i = 0; i < lines.length; i++) {
			components[i] = Component.literal(lines[i]);
		}
		registration.addIngredientInfo(item, components);
	}
}
