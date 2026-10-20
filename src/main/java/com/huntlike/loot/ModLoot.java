package com.huntlike.loot;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import com.huntlike.registry.ModBlocks;

/** Witches (2%) and evokers (2.5%) drop the Alpha Altar when killed by a player. Looting has no effect. */
public final class ModLoot {
	private ModLoot() {
	}

	private static final ResourceKey<LootTable> WITCH = ResourceKey.create(Registries.LOOT_TABLE,
			Identifier.withDefaultNamespace("entities/witch"));
	private static final ResourceKey<LootTable> EVOKER = ResourceKey.create(Registries.LOOT_TABLE,
			Identifier.withDefaultNamespace("entities/evoker"));

	public static void initialize() {
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (!source.isBuiltin()) {
				return;
			}

			if (WITCH.equals(key)) {
				tableBuilder.withPool(altarPool(0.02F));
			} else if (EVOKER.equals(key)) {
				tableBuilder.withPool(altarPool(0.025F));
			}
		});
	}

	private static LootPool.Builder altarPool(float chance) {
		return LootPool.lootPool()
				.add(LootItem.lootTableItem(ModBlocks.ALPHA_ALTAR))
				.when(LootItemKilledByPlayerCondition.killedByPlayer())
				.when(LootItemRandomChanceCondition.randomChance(chance));
	}
}
