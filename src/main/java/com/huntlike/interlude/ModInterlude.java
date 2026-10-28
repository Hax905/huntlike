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

	public static final Item GROWING_SLIME_BALL = ModItems.register("growing_slime_ball", Item::new, new Item.Properties()
			.rarity(Rarity.RARE).component(DataComponents.LORE, new ItemLore(lore(
					"A fragment of Bartholomew. His courage and valor keep him alive, and for some reason it was a weird reaction "
							+ "with enchanted books. (Use it)"))));
	public static final Item POMS_SKELETAL_REMAINS = ModItems.register("poms_skeletal_remains", RemainsItem::new, new Item.Properties()
			.stacksTo(1).rarity(Rarity.EPIC).component(DataComponents.LORE, new ItemLore(lines("The bones are alive!", "(Right-Click)"))));

	/** Horseshoes put on Pom's Skeletal Remains (0 to 5), shown as stars after its name. */
	public static final net.minecraft.core.component.DataComponentType<Integer> HORSESHOES = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE, HuntLike.id("horseshoes"),
			net.minecraft.core.component.DataComponentType.<Integer>builder().persistent(com.mojang.serialization.Codec.INT)
					.networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT).build());

	/** Put on Pom's Skeletal Remains at an anvil, up to 5 times: each one adds a star and speed. */
	public static final Item HORSESHOE = ModItems.register("horseshoe", Item::new, new Item.Properties()
			.rarity(Rarity.RARE).component(DataComponents.LORE, new ItemLore(lines("Can be put on a certain horse..."))));
	public static final Item BOUNCY_SLIME_BALL = ModItems.register("bouncy_slime_ball", Item::new, new Item.Properties()
			.rarity(Rarity.UNCOMMON).component(DataComponents.LORE, new ItemLore(lines(
					"A bouncy slime gel of some sorts. It barely has a shape so it isn't a ball!"))));
	public static final Item SLIMY_REBOUNDERS = SlimyRebounders.create();

	/** Lines one under the other, with no blank line between them. */
	static List<Component> lines(String... lines) {
		List<Component> lore = new ArrayList<>();
		for (String line : lines) {
			Lore.gray(lore, line);
		}
		return lore;
	}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(POM, ZombieHorse.createAttributes());
		FabricDefaultAttributeRegistry.register(BARTHOLOMEW,
				net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(net.minecraft.world.entity.EntityTypes.SLIME));
		FabricDefaultAttributeRegistry.register(REMAINS_HORSE, SkeletonHorse.createAttributes());
		SlimeRiderFight.initialize();
	}
}
