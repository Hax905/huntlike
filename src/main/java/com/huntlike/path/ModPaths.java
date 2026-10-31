package com.huntlike.path;

import java.util.List;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.ItemLore;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import com.huntlike.HuntLike;
import com.huntlike.registry.Lore;
import com.huntlike.registry.ModItems;

/** The Paths' items and mobs (Path of the Wildfire: the Blazing Hearts, the Blazing Piglin and Wither Skeleton). */
public final class ModPaths {
	private ModPaths() {
	}

	private static final String HEART_LORE = "The heart pulses with force and heat. It feels warm... It is attracted to beings consumed by their sins.";

	public static final Item BLAZING_HEART = ModItems.register("blazing_heart", BlazingHeartItem::new, new Item.Properties()
			.rarity(Rarity.RARE).stacksTo(16).fireResistant()
			.food(new FoodProperties(20, 0.6F, true), Consumables.defaultFood().consumeSeconds(BlazingHeartItem.EAT_TICKS / 20.0F).build())
			.component(DataComponents.LORE, new ItemLore(Lore.wrapGray(HEART_LORE))));
	/** Quest items (not edible): from the Blazing Piglin, the Blazing Wither Skeleton and the Demonling. */
	public static final Item BLAZING_HEART_STAR = ModItems.register("blazing_heart_star", Item::new, new Item.Properties()
			.rarity(Rarity.EPIC).stacksTo(16).fireResistant().component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
			.component(DataComponents.LORE, new ItemLore(questLore())));
	public static final Item BLAZING_HEART_STAR2 = ModItems.register("blazing_heart_star2", Item::new, new Item.Properties()
			.rarity(Rarity.EPIC).stacksTo(16).fireResistant().component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
			.component(DataComponents.LORE, new ItemLore(questLore())));

	/** From the Demonling: used on a spawner, it spawns Imps. */
	public static final Item BLAZING_HEART_STAR3 = ModItems.register("blazing_heart_star3", Item::new, new Item.Properties()
			.rarity(Rarity.EPIC).stacksTo(16).fireResistant().component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
			.component(DataComponents.LORE, new ItemLore(questLore())));

	private static List<Component> questLore() {
		List<Component> lines = new java.util.ArrayList<>(Lore.wrapGray(HEART_LORE));
		lines.add(Component.literal("Quest Item").withStyle(style -> style.withColor(0xFFAA00).withItalic(false)));
		return lines;
	}

	/** The Chalice's blood (0-100). */
	public static final net.minecraft.core.component.DataComponentType<Integer> BLOOD = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, HuntLike.id("blood"),
			net.minecraft.core.component.DataComponentType.<Integer>builder().persistent(com.mojang.serialization.Codec.INT)
					.networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT).build());

	public static final net.minecraft.world.level.block.Block BLAZING_SPAWNER = block("blazing_spawner", BlazingSpawnerBlock::new,
			net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.NETHER)
					.strength(-1.0F, 3600000.0F).noLootTable().noOcclusion().sound(net.minecraft.world.level.block.SoundType.SPAWNER)
					.lightLevel(state -> 12));
	public static final net.minecraft.world.level.block.Block CHALICE = block("chalice", ChaliceBlock::new,
			net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.GOLD)
					.strength(1.5F, 6.0F).requiresCorrectToolForDrops().noOcclusion().sound(net.minecraft.world.level.block.SoundType.METAL));
	public static final Item BLAZING_SPAWNER_ITEM = ModItems.register("blazing_spawner",
			p -> new net.minecraft.world.item.BlockItem(BLAZING_SPAWNER, p), new Item.Properties().rarity(Rarity.EPIC).useBlockDescriptionPrefix());
	public static final Item CHALICE_ITEM = ModItems.register("chalice", p -> new ChaliceItem(CHALICE, p), new Item.Properties()
			.rarity(Rarity.EPIC).stacksTo(1).useBlockDescriptionPrefix()
			.component(DataComponents.LORE, new ItemLore(Lore.wrapGray("Hold it in your off-hand. It thirsts for the blood of the living."))));
	public static final Item FULL_CHALICE = ModItems.register("full_chalice", FullChaliceItem::new, new Item.Properties()
			.rarity(Rarity.EPIC).stacksTo(1).component(DataComponents.LORE, new ItemLore(Lore.wrapGray("Something stirs in the blood..."))));

	public static final net.minecraft.world.level.block.entity.BlockEntityType<BlazingSpawnerBlockEntity> BLAZING_SPAWNER_ENTITY =
			Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, HuntLike.id("blazing_spawner"),
					new net.minecraft.world.level.block.entity.BlockEntityType<>(BlazingSpawnerBlockEntity::new, java.util.Set.of(BLAZING_SPAWNER)));

	private static net.minecraft.world.level.block.Block block(String name,
			java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, net.minecraft.world.level.block.Block> factory,
			net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
		ResourceKey<net.minecraft.world.level.block.Block> key = ResourceKey.create(Registries.BLOCK, HuntLike.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	public static final EntityType<BlazingPiglin> BLAZING_PIGLIN = entity("blazing_piglin",
			EntityType.Builder.<BlazingPiglin>of(BlazingPiglin::new, MobCategory.MONSTER).sized(0.6F, 1.95F).eyeHeight(1.79F).fireImmune()
					.clientTrackingRange(8));
	public static final EntityType<BlazingWitherSkeleton> BLAZING_WITHER_SKELETON = entity("blazing_wither_skeleton",
			EntityType.Builder.<BlazingWitherSkeleton>of(BlazingWitherSkeleton::new, MobCategory.MONSTER).sized(0.7F, 2.4F).eyeHeight(2.1F)
					.fireImmune().clientTrackingRange(8));

	public static final EntityType<Demonling> DEMONLING = entity("demonling",
			EntityType.Builder.<Demonling>of(Demonling::new, MobCategory.MONSTER).sized(0.6F, 1.8F).fireImmune().clientTrackingRange(8));
	public static final EntityType<Imp> IMP = entity("imp",
			EntityType.Builder.<Imp>of(Imp::new, MobCategory.MONSTER).sized(0.6F, 1.8F).fireImmune().clientTrackingRange(8));

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> entity(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, HuntLike.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(BLAZING_PIGLIN, BlazingPiglin.createAttributes());
		FabricDefaultAttributeRegistry.register(BLAZING_WITHER_SKELETON, BlazingWitherSkeleton.createAttributes());
		FabricDefaultAttributeRegistry.register(DEMONLING, Demonling.createAttributes());
		FabricDefaultAttributeRegistry.register(IMP, Imp.createAttributes());
		WildfirePath.initialize();
	}
}
