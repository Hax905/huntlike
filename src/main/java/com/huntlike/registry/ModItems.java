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
	public static final Item SPIRITUAL_CATALYST = simple("spiritual_catalyst", Rarity.EPIC);

	// Spirit Lure drop
	public static final Item SOUL_STONE = simple("soul_stone", Rarity.UNCOMMON);

	/** Eaten once for better rare-drop odds. */
	public static final Item MYSTERY_CONCOCTION = register("mystery_concoction", MysteryConcoctionItem::new,
			new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)
					.food(new net.minecraft.world.food.FoodProperties(2, 0.2F, true))
					.component(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(List.of(
							net.minecraft.network.chat.Component.literal("The type of meal you regret to eat...")
									.withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GRAY).withItalic(false)),
							net.minecraft.network.chat.Component.literal("But just one bite wouldn't kill me!")
									.withStyle(style -> style.withColor(net.minecraft.ChatFormatting.RED).withItalic(false))))));

	/** Eaten instantly: 8 hunger, 8 saturation. */
	public static final Item GOLDEN_MEAT = register("golden_meat", Item::new,
			new Item.Properties().rarity(Rarity.RARE)
					.component(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(
							Lore.wrapGray("You don't even want to chew.")))
					.food(new net.minecraft.world.food.FoodProperties(8, 8.0F, false),
							net.minecraft.world.item.component.Consumables.defaultFood().consumeSeconds(0.0F).build()));

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
