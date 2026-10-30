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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import com.huntlike.HuntLike;
import com.huntlike.gear.ModKoro;
import com.huntlike.gear.ModShields;
import com.huntlike.hunt.HuntDefs;
import com.huntlike.hunt.HuntDefs.Cost;
import com.huntlike.hunt.HuntDefs.GearTrade;
import com.huntlike.hunt.KoroScript;
import com.huntlike.interlude.ModInterlude;
import com.huntlike.interlude.RemainsItem;
import com.huntlike.registry.ModBlocks;
import com.huntlike.registry.ModItems;
import com.huntlike.towernaar.TowernaarManager;
import com.huntlike.towernaar.TowernaarScript;

/**
 * JEI (and EMI, which loads JEI plugins) support: the two anvil uses, every Gear tab, Koro, Towernaar and PRD trade, and
 * info pages for items whose use isn't a recipe. Drops are left out on purpose, so players find them by hunting.
 */
public final class HuntLikeJeiPlugin implements IModPlugin {
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

	/** Opens JEI on the trades (0) or anvil (1) pages; false if JEI isn't running. Must run on the client thread. */
	public static boolean showForTest(int page) {
		if (runtime == null) {
			return false;
		}
		if (page == 0) {
			runtime.getRecipesGui().showTypes(List.of(TradesCategory.TYPE));
		} else {
			runtime.getRecipesGui().show(runtime.getJeiHelpers().getFocusFactory().createFocus(
					mezz.jei.api.recipe.RecipeIngredientRole.INPUT, mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
					new ItemStack(ModInterlude.GROWING_SLIME_BALL)));
		}
		return true;
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		var gui = registration.getJeiHelpers().getGuiHelper();
		registration.addRecipeCategories(new TradesCategory(gui));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.ALPHA_ALTAR),
				TradesCategory.TYPE);
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registration.addRecipes(TradesCategory.TYPE, trades());
		registration.addRecipes(RecipeTypes.ANVIL, anvil(registration.getVanillaRecipeFactory()));
		info(registration);
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
		// Growing Slime Ball: every enchantment below its maximum goes up one (books and any enchanted item). Shown per enchantment on
		// books, and on each diamond armor piece with Protection II and Unbreaking II.
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
			var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
			var protection = enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION);
			var unbreaking = enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING);
			List<ItemStack> armor = new ArrayList<>();
			List<ItemStack> grownArmor = new ArrayList<>();
			for (Item piece : List.of(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS)) {
				armor.add(armor(piece, protection, unbreaking, 2));
				grownArmor.add(armor(piece, protection, unbreaking, 3));
			}
			list.add(factory.createAnvilRecipe(armor, List.of(new ItemStack(ModInterlude.GROWING_SLIME_BALL)), grownArmor,
					Identifier.fromNamespaceAndPath(HuntLike.MOD_ID, "anvil/growing_armor")));
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

	private static ItemStack armor(Item piece, Holder<Enchantment> a, Holder<Enchantment> b, int level) {
		ItemStack stack = new ItemStack(piece);
		ItemEnchantments.Mutable enchants = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		enchants.set(a, level);
		enchants.set(b, level);
		stack.set(DataComponents.ENCHANTMENTS, enchants.toImmutable());
		return stack;
	}

	// ---- Info pages ----

	private static void info(IRecipeRegistration registration) {
		page(registration, ModBlocks.ALPHA_ALTAR.asItem(),
				"Right-click to open the Hunt menu: buy Ritual Tokens, start Hunting Quests and trade for gear.",
				"Ritual Tokens: 10 Logs buy 64 Animal tokens, 5 Soul Sand buy 64 Cluster tokens. Interlude Tokens: 1 Gold Ingot buys 16.",
				"The Hunt's trades are listed under the Alpha Altar's recipes.");
		page(registration, ModInterlude.GROWING_SLIME_BALL,
				"On an anvil, with an enchanted book or any enchanted item: every enchantment below its maximum goes up one level. Costs 5 levels per enchantment raised.");
		page(registration, ModInterlude.POMS_SKELETAL_REMAINS,
				"Right-click to call Pom's skeletal horse; right-click again to send it away.",
				"A Horseshoe on an anvil adds a star (up to 5). Each star makes the horse faster.");
		page(registration, ModInterlude.HORSESHOE,
				"On an anvil, adds a star to Pom's Skeletal Remains for 5 levels.");
		page(registration, ModInterlude.BOUNCY_SLIME_BALL,
				"Crafts the Slimy Rebounders.");
		page(registration, ModInterlude.SLIMY_REBOUNDERS,
				"Boots that bounce you back up after a fall of more than 1 block, and cancel fall damage.");
		page(registration, ModItems.MYSTERY_CONCOCTION,
				"Drink it to improve rare drop odds in the Animal Hunt and on Pom's side of the Interlude: \"1 in X\" becomes \"1 in X-1\".");
		page(registration, ModItems.WARM_AND_SLIMY_BEEF_JERKY,
				"Improves rare drop odds in the Cluster Hunt and on Bartholomew's side of the Interlude: \"1 in X\" becomes \"1 in X-1\".");
		page(registration, ModShields.MAGNETIC_WAND_PRD,
				"Bought from Towernaar after his Final Chapter. Using it unlocks the Magnetic Wand trade in the Cluster Gear tab.");
		page(registration, ModShields.MAGNETIC_WAND,
				"Right-click to pull creatures and dropped items to where you look. Creatures are slowed for 3 s once released.");
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
