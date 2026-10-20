package com.huntlike.registry;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import com.huntlike.HuntLike;

/** Every HuntLike block. */
public final class ModBlocks {
	private ModBlocks() {
	}

	/** Opens the Hunt menu (menu comes in a later version). */
	public static final Block ALPHA_ALTAR = register("alpha_altar", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(3.0F, 1200.0F).sound(SoundType.BONE_BLOCK)
					.requiresCorrectToolForDrops().lightLevel(state -> 7),
			Rarity.RARE);

	/** Resistance I and creeper protection in a 17x17x17 cube (effect comes in a later version). */
	public static final Block SOUL_MONOLITH = register("soul_monolith", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(4.0F, 1200.0F).sound(SoundType.STONE)
					.requiresCorrectToolForDrops().lightLevel(state -> 10),
			Rarity.EPIC);

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory,
			BlockBehaviour.Properties properties, Rarity rarity) {
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, HuntLike.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, blockKey, factory.apply(properties.setId(blockKey)));

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, HuntLike.id(name));
		Item.Properties itemProperties = Descriptions.with(
				new Item.Properties().rarity(rarity).useBlockDescriptionPrefix().setId(itemKey),
				"block.huntlike." + name + ".desc");
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, itemProperties));
		return block;
	}

	public static void initialize() {
		// Loading this class registers the blocks.
	}
}
