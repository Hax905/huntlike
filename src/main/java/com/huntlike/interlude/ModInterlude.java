package com.huntlike.interlude;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import com.huntlike.HuntLike;
import com.huntlike.registry.Lore;
import com.huntlike.registry.ModItems;

/** The Slime Rider Interlude's mobs (Pom, Bartholomew, the Remains' skeleton horse) and items. */
public final class ModInterlude {
	private ModInterlude() {
	}

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> entity(String name, EntityType.EntityFactory<T> factory,
			MobCategory category, float width, float height, boolean fireImmune) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, HuntLike.id(name));
		EntityType.Builder<T> builder = EntityType.Builder.of(factory, category).sized(width, height).clientTrackingRange(10);
		if (fireImmune) {
			builder = builder.fireImmune();
		}
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	// Pom is undead but the fight shouldn't end because of the sun: it doesn't burn.
	public static final EntityType<Pom> POM = entity("pom", Pom::new, MobCategory.MONSTER, 1.3964844F, 1.6F, true);
	public static final EntityType<Bartholomew> BARTHOLOMEW = entity("bartholomew", Bartholomew::new, MobCategory.MONSTER, 0.52F,
			0.52F, false);
	public static final EntityType<RemainsHorse> REMAINS_HORSE = entity("remains_horse", RemainsHorse::new, MobCategory.CREATURE,
			1.3964844F, 1.6F, false);

	private static List<Component> lore(String... lines) {
		List<Component> lore = new ArrayList<>();
		for (int i = 0; i < lines.length; i++) {
			if (i > 0) {
				lore.add(Component.empty());
			}
			Lore.gray(lore, lines[i]);
		}
		return lore;
	}

	public static final Item GROWING_SLIMEBALL = ModItems.register("growing_slimeball", Item::new, new Item.Properties()
			.rarity(Rarity.RARE).component(DataComponents.LORE, new ItemLore(lore(
					"Put it on an anvil with an enchanted book that holds a single enchantment: the enchantment gains a level, "
							+ "up to its maximum. Costs 10 levels."))));
	public static final Item SLIME_RIDER_REMAINS = ModItems.register("slime_rider_remains", RemainsItem::new, new Item.Properties()
			.stacksTo(1).rarity(Rarity.EPIC).component(DataComponents.LORE, new ItemLore(lore(
					"A pile of bones from Pom's old mount days.",
					"Right-click to call a saddled skeleton horse and ride it. Right-click again to send it away."))));

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(POM, ZombieHorse.createAttributes());
		FabricDefaultAttributeRegistry.register(BARTHOLOMEW,
				net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(net.minecraft.world.entity.EntityTypes.SLIME));
		FabricDefaultAttributeRegistry.register(REMAINS_HORSE, SkeletonHorse.createAttributes());
		SlimeRiderFight.initialize();
	}
}
