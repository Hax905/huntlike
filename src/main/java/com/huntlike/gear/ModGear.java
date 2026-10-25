package com.huntlike.gear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import com.huntlike.HuntLike;
import com.huntlike.registry.Lore;
import com.huntlike.registry.ModItems;

/** The Animal Hunt Weapons and Hunt Armor. */
public final class ModGear {
	private ModGear() {
	}

	// ---------------------------------------------------------------- weapons

	/** Damage multipliers against Animal mobs and bosses, by weapon stage (1 = Skin Knife). */
	public static final float[] BIOHAZARD = {1.0F, 1.0F, 1.25F, 1.30F, 1.35F, 1.40F};
	/** Momentum per hit and its cap, by stage. */
	public static final float[] MOMENTUM_STEP = {0, 0, 0, 0.03F, 0.04F, 0.05F};
	public static final float[] MOMENTUM_CAP = {0, 0, 0, 0.60F, 0.80F, 1.00F};

	public static final Item SKIN_KNIFE = weapon("skin_knife", 1, ToolMaterial.IRON, Rarity.UNCOMMON,
			"A great skinning knife. Too big for small animals.");
	public static final Item MEAT_CUTTER = weapon("meat_cutter", 2, ToolMaterial.IRON, Rarity.UNCOMMON,
			"A big knife with the capabilities of cutting meat easily. Flesh is consuming the blade.");
	public static final Item LIMB_SEVERER = weapon("limb_severer", 3, ToolMaterial.IRON, Rarity.RARE,
			"A hacking blade. It is partially flesh, no longer just metal and wood.");
	public static final Item BONE_BREAKER = weapon("bone_breaker", 4, ToolMaterial.IRON, Rarity.RARE,
			"A big living mallet packing a hefty punch.");
	public static final Item BONE_PULVERIZER = weapon("bone_pulverizer", 5, ToolMaterial.DIAMOND, Rarity.EPIC,
			"A gigantic flesh mace tied to a bone as a handle. Since you fed it to make it bigger, it won't eat you!");

	public static final List<Item> WEAPONS = List.of(SKIN_KNIFE, MEAT_CUTTER, LIMB_SEVERER, BONE_BREAKER, BONE_PULVERIZER);

	/** 0 if the item isn't an Animal Hunt Weapon, else its stage 1 to 5. */
	public static int weaponStage(Item item) {
		int index = WEAPONS.indexOf(item);
		return index + 1;
	}

	private static Item weapon(String name, int stage, ToolMaterial material, Rarity rarity, String description) {
		List<Component> lore = new ArrayList<>();
		Lore.gray(lore, description);
		Lore.ability(lore, "Bountiful!", "Animal kills count double towards the quota.");
		if (stage >= 2) {
			ability(lore, "Biohazard", "Grants " + Math.round((BIOHAZARD[stage] - 1) * 100) + "% more damage towards animals.");
		}
		if (stage >= 3) {
			ability(lore, "Momentum", "Deals more damage to Animal creatures the more they are hit.");
		}
		if (stage >= 5) {
			ability(lore, "Fracture", "Fractures on the osseous structure of the organism cause it to move slower.");
		}
		Item.Properties properties = new Item.Properties()
				.sword(material, 3.0F, -2.4F)
				.rarity(rarity)
				.component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
				.component(DataComponents.LORE, new ItemLore(lore));
		return ModItems.register(name, Item::new, properties);
	}

	// ---------------------------------------------------------------- armor

	/** One Hunt Armor set. defense is helmet, chestplate, leggings, boots; animalReduction needs the full set. */
	public record ArmorSet(String hunt, String id, double[] defense, double toughness, int health, float animalReduction,
			float lungeResistance, Item helmet, Item chestplate, Item leggings, Item boots) {
		public List<Item> pieces() {
			return List.of(helmet, chestplate, leggings, boots);
		}
	}

	//                                                                   armor per piece        tough. health reduction lunge
	public static final ArmorSet ROTTEN = armorSet("animal", "rotten", new double[] {2, 6, 5, 2}, 0, 1, 0.10F, 0, 0, Rarity.UNCOMMON);
	public static final ArmorSet CARRION = armorSet("animal", "carrion", new double[] {2, 6, 5, 2}, 0, 2, 0.20F, 0, 0, Rarity.RARE);
	public static final ArmorSet ECOCIDE = armorSet("animal", "ecocide", new double[] {3, 8, 6, 3}, 2, 3, 0.30F, 0.20F, 0, Rarity.EPIC);

	/** Cluster Hunt Armor I: iron armor stats, Slippery, and less damage from Cluster creatures (full set). */
	public static final ArmorSet ICY = armorSet("cluster", "icy", new double[] {2, 6, 5, 2}, 0, 0, 0.05F, 0, 0, Rarity.UNCOMMON);
	/** Cluster Hunt Armor II: diamond armor, 1.5 toughness per piece, Slippery and Better Than Ever!. */
	public static final ArmorSet GEL = armorSet("cluster", "gel", new double[] {3, 8, 6, 3}, 1.5, 0, 0.08F, 0, 0, Rarity.RARE);
	/** Cluster Hunt Armor III: netherite armor stats, Slippery, Better Than Ever! and Room Temperature. */
	public static final ArmorSet THERMAL = armorSet("cluster", "thermal", new double[] {3, 8, 6, 3}, 3, 0, 0.15F, 0, 0.1, Rarity.EPIC);

	public static final List<ArmorSet> ARMOR_SETS = List.of(ROTTEN, CARRION, ECOCIDE, ICY, GEL, THERMAL);
	public static final List<ArmorSet> CLUSTER_SETS = List.of(ICY, GEL, THERMAL);

	/** Better Than Ever!: after escaping Yummy!, Temperature is this much more effective until the boss perishes. */
	public static final float BETTER_THAN_EVER = 0.20F;

	public static ArmorSet armorSetOf(Item item) {
		for (ArmorSet set : ARMOR_SETS) {
			if (set.pieces().contains(item)) {
				return set;
			}
		}
		return null;
	}

	private static ArmorSet armorSet(String hunt, String id, double[] defense, double toughness, int health, float reduction,
			float lunge, double knockback, Rarity rarity) {
		boolean cluster = hunt.equals("cluster");
		ResourceKey<EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID, HuntLike.id(id));
		Map<ArmorType, Integer> vanillaDefense = new HashMap<>();
		vanillaDefense.put(ArmorType.HELMET, (int) defense[0]);
		vanillaDefense.put(ArmorType.CHESTPLATE, (int) defense[1]);
		vanillaDefense.put(ArmorType.LEGGINGS, (int) defense[2]);
		vanillaDefense.put(ArmorType.BOOTS, (int) defense[3]);
		vanillaDefense.put(ArmorType.BODY, (int) defense[1]);
		ArmorMaterial material = new ArmorMaterial(37, vanillaDefense, 25, SoundEvents.ARMOR_EQUIP_LEATHER, (float) toughness, 0.0F,
				ItemTags.REPAIRS_LEATHER_ARMOR, asset);
		List<Component> lore = new ArrayList<>();
		if (cluster) {
			ability(lore, "Slippery", "Decreases damage from Sticky Fingers.");
			if (!id.equals("icy")) {
				ability(lore, "Better Than Ever!", "After being eaten by a Cluster boss, come back better than ever!");
			}
			if (id.equals("thermal")) {
				ability(lore, "Room Temperature", "Won't be able to be burnt or frozen.");
			}
			lore.add(Component.empty());
			Lore.gray(lore, "-" + percent(reduction) + "% damage from Cluster creatures (full set).");
		} else {
			ability(lore, "Egg Shell", "Attacks that involve Eggs are nullified.");
			ability(lore, "Animalistic", "The armor reacts to familiar tendencies of species and reduces damage from Animal creatures.");
			lore.add(Component.empty());
			Lore.gray(lore, "-" + percent(reduction) + "% damage from Animal creatures (full set).");
		}
		if (lunge > 0) {
			Lore.gray(lore, "-" + percent(lunge) + "% Animalia Lunge knockback per piece.");
		}
		ArmorType[] types = {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS};
		String[] names = {"helmet", "chestplate", "leggings", "boots"};
		EquipmentSlotGroup[] groups = {EquipmentSlotGroup.HEAD, EquipmentSlotGroup.CHEST, EquipmentSlotGroup.LEGS, EquipmentSlotGroup.FEET};
		Item[] items = new Item[4];
		for (int i = 0; i < 4; i++) {
			ItemAttributeModifiers.Builder attributes = ItemAttributeModifiers.builder();
			attributes.add(Attributes.ARMOR, new AttributeModifier(HuntLike.id("armor." + names[i]), defense[i],
					AttributeModifier.Operation.ADD_VALUE), groups[i]);
			if (toughness > 0) {
				attributes.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(HuntLike.id("toughness." + names[i]), toughness,
						AttributeModifier.Operation.ADD_VALUE), groups[i]);
			}
			if (health > 0) {
				attributes.add(Attributes.MAX_HEALTH, new AttributeModifier(HuntLike.id("health." + names[i]), health,
						AttributeModifier.Operation.ADD_VALUE), groups[i]);
			}
			Item.Properties properties = new Item.Properties()
					.humanoidArmor(material, types[i])
					.attributes(attributes.build())
					.rarity(rarity)
					.component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
					.component(DataComponents.LORE, new ItemLore(lore));
			items[i] = ModItems.register(id + "_" + names[i], Item::new, properties);
		}
		return new ArmorSet(hunt, id, defense, toughness, health, reduction, lunge, items[0], items[1], items[2], items[3]);
	}

	/** 0.15 -> "15", 0.0125 -> "1.25". */
	private static String percent(float fraction) {
		java.math.BigDecimal value = java.math.BigDecimal.valueOf(Math.round(fraction * 10000) / 100.0).stripTrailingZeros();
		return value.toPlainString();
	}

	private static void ability(List<Component> lore, String name, String description) {
		Lore.ability(lore, name, description);
	}

	public static void initialize() {
	}
}
