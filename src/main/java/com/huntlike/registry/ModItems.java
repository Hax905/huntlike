package com.huntlike.registry;

import java.util.List;
import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import com.huntlike.HuntLike;

/** Every HuntLike item. */
public final class ModItems {
	private ModItems() {
	}

	// Animal Hunt guaranteed drop and its crafting chain
	public static final Item LOW_QUALITY_MYSTERY_MEAT = simple("low_quality_mystery_meat", Rarity.COMMON);
	public static final Item DECENT_QUALITY_MYSTERY_MEAT = simple("decent_quality_mystery_meat", Rarity.UNCOMMON);
	public static final Item HIGH_QUALITY_MYSTERY_MEAT = simple("high_quality_mystery_meat", Rarity.RARE);

	// Animal Hunt rare drops
	public static final Item THICK_HIDE = simple("thick_hide", Rarity.COMMON);
	public static final Item LARGE_FEMUR = simple("large_femur", Rarity.UNCOMMON);
	public static final Item GOLDEN_MOSS = simple("golden_moss", Rarity.RARE);
	public static final Item SPIRITUAL_CATALYST = register("spiritual_catalyst", p -> new SpiritualCatalystItem(p, 1),
			new Item.Properties().rarity(Rarity.EPIC).component(net.minecraft.core.component.DataComponents.LORE,
					new net.minecraft.world.item.component.ItemLore(SpiritualCatalystItem.lore())));

	// Cluster Hunt drops and their crafting chain (descriptions come later)
	public static final Item THERMAL_SLIMEBALL = plain("thermal_slimeball", Rarity.COMMON);
	public static final Item FROZEN_SLIME_BALL = plain("frozen_slime_ball", Rarity.UNCOMMON);
	public static final Item GELID_SLIME_BALL = plain("gelid_slime_ball", Rarity.RARE);
	public static final Item SLIMYSTEEL = plain("slimysteel", Rarity.UNCOMMON);
	public static final Item IONIC_LIQUID = plain("ionic_liquid", Rarity.COMMON);
	public static final Item SLIMY_CORE = plain("slimy_core", Rarity.UNCOMMON);
	public static final Item SUBLIMATING_MAGMA = plain("sublimating_magma", Rarity.UNCOMMON);
	public static final Item CORRODED_PALLADIUM = plain("corroded_palladium", Rarity.RARE);
	public static final Item DIGESTED_PLATINUM_ISOTOPE = plain("digested_platinum_isotope", Rarity.EPIC);

	// Spirit Lure drop
	public static final Item SOUL_STONE = simple("soul_stone", Rarity.UNCOMMON);

	/** Eaten once for better rare-drop odds. */
	public static final Item MYSTERY_CONCOCTION = register("mystery_concoction", MysteryConcoctionItem::concoction,
			oneTime("The type of meal you regret to eat...", "But just one bite wouldn't kill me!"));

	/** Eaten instantly: 8 hunger, 8 saturation. */
	public static final Item GOLDEN_MEAT = register("golden_meat", Item::new,
			new Item.Properties().rarity(Rarity.RARE)
					.component(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(
							Lore.wrapGray("You don't even want to chew.")))
					.food(new net.minecraft.world.food.FoodProperties(8, 8.0F, false),
							net.minecraft.world.item.component.Consumables.defaultFood().consumeSeconds(0.0F).build()));

	/** Eaten once for better Cluster rare-drop odds. */
	public static final Item WARM_AND_SLIMY_BEEF_JERKY = register("warm_and_slimy_beef_jerky", MysteryConcoctionItem::jerky,
			oneTime("The slimy feel makes you want to never touch it again.", "But it looks like such a gummy!"));

	/** A One-time Consumable luck food: Epic, stacks to 16, 2 hunger, always edible, gray then red description. */
	private static Item.Properties oneTime(String gray, String red) {
		return new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)
				.food(new net.minecraft.world.food.FoodProperties(2, 0.2F, true))
				.component(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(
						net.minecraft.network.chat.Component.literal("One-time Consumable")
								.withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GRAY).withItalic(false)),
						net.minecraft.network.chat.Component.literal(gray)
								.withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GRAY).withItalic(false)),
						net.minecraft.network.chat.Component.literal(red)
								.withStyle(style -> style.withColor(net.minecraft.ChatFormatting.RED).withItalic(false)))));
	}

	/** Detect Nearby Souls, three at a time (Spiritual Catalyst in a compass recipe). */
	public static final Item SPIRITUAL_COMPASS = register("spiritual_compass", p -> new SpiritualCatalystItem(p, 3),
			new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).component(net.minecraft.core.component.DataComponents.LORE,
					new net.minecraft.world.item.component.ItemLore(SpiritualCatalystItem.compassLore())));

	/** What's left of a slime pet whose every piece died. Can't burn in fire or lava. */
	public static final Item SLIME_PET_CORE = register("slime_pet_core", Item::new, new Item.Properties().rarity(Rarity.RARE)
			.stacksTo(1).fireResistant().component(net.minecraft.core.component.DataComponents.LORE,
					new net.minecraft.world.item.component.ItemLore(Lore.wrapGray("Surround it with slime balls in a crafting table to bring your slime back."))));
	/** The core after the revive recipe: used on the ground, the pet comes back. */
	public static final Item REVIVED_SLIME_PET_CORE = register("revived_slime_pet_core", com.huntlike.pet.RevivedCoreItem::new,
			new Item.Properties().rarity(Rarity.RARE).stacksTo(1).fireResistant().component(net.minecraft.core.component.DataComponents.LORE,
					new net.minecraft.world.item.component.ItemLore(Lore.wrapGray("Use it on the ground to bring your slime back!"))));

	/** The Hunting Trophy block item of a Hunt. */
	public static Item trophy(String hunt) {
		return switch (hunt) {
			default -> ModBlocks.ANIMAL_TROPHY.asItem();
		};
	}

	/** An item without a description line (yet). */
	private static Item plain(String name, Rarity rarity) {
		return register(name, Item::new, new Item.Properties().rarity(rarity));
	}

	private static Item simple(String name, Rarity rarity) {
		Item.Properties properties = Descriptions.with(new Item.Properties().rarity(rarity), "item.huntlike." + name + ".desc");
		return register(name, Item::new, properties);
	}

	public static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, HuntLike.id(name));
		Item item = factory.apply(properties.setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static void initialize() {
		// Loading this class registers the items.
	}
}
