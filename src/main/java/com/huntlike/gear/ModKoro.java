package com.huntlike.gear;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemLore;

import com.huntlike.registry.Lore;
import com.huntlike.registry.ModItems;

/** Koro's gifts: the three bells (Special Item) and Koro Whakamate (Second Special Item). */
public final class ModKoro {
	private ModKoro() {
	}

	public static final Item WOODEN_BELL = bell("wooden_bell", 2.0F, 0, Rarity.UNCOMMON,
			"A magical wooden bell that refreshes your soul.");
	public static final Item GOLDEN_BELL = bell("golden_bell", 4.0F, 0, Rarity.RARE,
			"The fine and expensive metals and hide provide a beautiful sight and sound that nourish the soul.");
	public static final Item MOSSY_BELL = bell("mossy_bell", 6.0F, 1, Rarity.EPIC,
			"The metals of the bell strike with themselves and the moss, producing a healing sound that mends the soul.");

	public static final Item KORO_WHAKAMATE;

	/** Only used to draw the Corrosive Stinger in flight. */
	public static final Item CORROSIVE_STINGER = ModItems.register("corrosive_stinger", Item::new, new Item.Properties());

	static {
		List<Component> lore = new ArrayList<>();
		lore.add(Component.literal("Fire Aspect III").withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false)));
		lore.add(Component.literal("Poison IV").withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false)));
		lore.add(Component.empty());
		Lore.gray(lore, "A spiritual dagger manifested by the unknown territory. Various magical effects linger from the blade.");
		Lore.ability(lore, "Fatal Touch", "Holds a magical aura that looks like fumes. The fumes are a manifestation of harmful magic.");
		Lore.ability(lore, "Corrosive Stinger", "A ranged attack that corrodes the target's matter, dealing heavy damage.");
		lore.add(Component.literal("Right-click, 15 second cooldown.").withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY)
				.withItalic(false)));
		KORO_WHAKAMATE = ModItems.register("koro_whakamate", KoroWhakamateItem::new, new Item.Properties()
				.sword(ToolMaterial.IRON, 1.0F, -2.0F)
				.rarity(Rarity.EPIC)
				.component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
				.component(DataComponents.LORE, new ItemLore(lore)));
	}

	private static Item bell(String name, float heal, int absorption, Rarity rarity, String description) {
		List<Component> lore = new ArrayList<>();
		Lore.gray(lore, description);
		// What to do with it: the "Safe and Sound" ability (no description) and how to use it.
		Lore.ability(lore, "Safe and Sound", "");
		lore.add(Component.literal("Right-click, 10 second cooldown.").withStyle(style -> style.withColor(ChatFormatting.DARK_GRAY)
				.withItalic(false)));
		return ModItems.register(name, properties -> new BellItem(properties, heal, absorption), new Item.Properties()
				.stacksTo(1)
				.rarity(rarity)
				.component(DataComponents.LORE, new ItemLore(lore)));
	}

	public static void initialize() {
	}
}
