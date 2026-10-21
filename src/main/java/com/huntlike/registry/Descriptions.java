package com.huntlike.registry;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;

/** Adds the gray description line shown under an item's name. */
public final class Descriptions {
	private Descriptions() {
	}

	/** Adds the translated description "item.huntlike.&lt;name&gt;.desc" as gray, non-italic lore. */
	public static Item.Properties with(Item.Properties properties, String translationKey) {
		List<Component> lines = new java.util.ArrayList<>();
		Lore.gray(lines, Lore.english(translationKey));
		return properties.component(DataComponents.LORE, new ItemLore(lines));
	}
}
