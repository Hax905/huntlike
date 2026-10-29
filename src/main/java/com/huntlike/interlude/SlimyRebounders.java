package com.huntlike.interlude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import com.huntlike.HuntLike;
import com.huntlike.registry.Lore;
import com.huntlike.registry.ModItems;

/**
 * Slimy Rebounders: +1 armor and nothing else, but a fall of more than 1 block bounces the wearer back up like a slime
 * block and never hurts (walking, stepping down and jumping on flat ground don't bounce). Sneaking lands normally and takes
 * the fall damage, as on a real slime block.
 */
public final class SlimyRebounders {
	private SlimyRebounders() {
	}

	/** Only a landing more than this many blocks below where the wearer last stood bounces. */
	public static final double MIN_DROP = 1.0;

	public static final TagKey<Item> REPAIRS = TagKey.create(Registries.ITEM, HuntLike.id("repairs_slimy_rebounders"));

	static Item create() {
		ResourceKey<EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID, HuntLike.id("slimy_rebounders"));
		Map<ArmorType, Integer> defense = new HashMap<>();
		for (ArmorType type : ArmorType.values()) {
			defense.put(type, 1);
		}
		ArmorMaterial material = new ArmorMaterial(15, defense, 25, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, REPAIRS, asset);
		ItemAttributeModifiers attributes = ItemAttributeModifiers.builder()
				.add(Attributes.ARMOR, new AttributeModifier(HuntLike.id("slimy_rebounders.armor"), 1.0, AttributeModifier.Operation.ADD_VALUE),
						EquipmentSlotGroup.FEET)
				.build();
		List<Component> lore = new ArrayList<>();
		Lore.gray(lore, "Between bounce and bounce they will make your adventures a blast!");
		return ModItems.register("slimy_rebounders", Item::new, new Item.Properties()
				.humanoidArmor(material, ArmorType.BOOTS)
				.attributes(attributes)
				.rarity(Rarity.RARE)
				.component(DataComponents.LORE, new ItemLore(lore)));
	}

	/** The entity wears Slimy Rebounders and isn't sneaking: it lands like on a slime block. */
	public static boolean bounces(Entity entity) {
		return entity instanceof LivingEntity living && !entity.isSuppressingBounce()
				&& living.getItemBySlot(EquipmentSlot.FEET).is(ModInterlude.SLIMY_REBOUNDERS);
	}
}
