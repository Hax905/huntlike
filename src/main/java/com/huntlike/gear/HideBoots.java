package com.huntlike.gear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
 * The Hide Boots line (Thick Hide, Winged [Act I], Hardened [Act II], Speedy [Act III], Hermes [Apotheosis])
 * and the FeatherChute. Upgrades happen in the Smithing Table with High Quality Mystery Meat as the template.
 */
public final class HideBoots {
	private HideBoots() {
	}

	public static final TagKey<Item> REPAIRS = TagKey.create(Registries.ITEM, HuntLike.id("repairs_hide_boots"));

	/** Blocks fallen before the Slow Falling kicks in. */
	public static final double GLIDE_AFTER = 3.0;
	/** FeatherChute durability lost per second of gliding. */
	public static final int CHUTE_TICKS_PER_DAMAGE = 20;

	private static final String WALK = "Walks on Powder Snow.";
	private static final String GLIDE = "Grants Slow Falling I after falling 3 blocks. Sneak to cancel.";
	private static final String SPEED = "+20% movement speed.";

	//                                                             armor toughness kb   speed  durability fireproof
	public static final Item THICK_HIDE_BOOTS = boots("thick_hide_boots", "thick_hide", 2, 0, 0, 0, 15, false, Rarity.UNCOMMON,
			WALK);
	public static final Item WINGED_HIDE_BOOTS = boots("winged_hide_boots", "winged_hide", 2, 0, 0, 0, 15, false, Rarity.UNCOMMON,
			WALK, GLIDE);
	public static final Item HARDENED_HIDE_BOOTS = boots("hardened_hide_boots", "hardened_hide", 3, 2, 0, 0, 33, false, Rarity.RARE,
			WALK, GLIDE);
	public static final Item SPEEDY_HIDE_BOOTS = boots("speedy_hide_boots", "speedy_hide", 3, 2, 0, 0.2, 33, false, Rarity.RARE,
			WALK, GLIDE, SPEED);
	public static final Item HERMES_BOOTS = boots("hermes_boots", "hermes", 3, 3, 0.1, 0.2, 37, true, Rarity.EPIC,
			WALK, GLIDE, SPEED, "Can't burn in fire or lava.");

	public static final List<Item> BOOTS = List.of(THICK_HIDE_BOOTS, WINGED_HIDE_BOOTS, HARDENED_HIDE_BOOTS, SPEEDY_HIDE_BOOTS,
			HERMES_BOOTS);
	/** Boots that glide (Winged and up). */
	public static final List<Item> GLIDING_BOOTS = BOOTS.subList(1, BOOTS.size());

	public static final Item FEATHER_CHUTE = featherChute();

	private static Item boots(String name, String assetId, double armor, double toughness, double knockback, double speed,
			int durability, boolean fireproof, Rarity rarity, String... lines) {
		ResourceKey<EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID, HuntLike.id(assetId));
		Map<ArmorType, Integer> defense = new HashMap<>();
		for (ArmorType type : ArmorType.values()) {
			defense.put(type, (int) armor);
		}
		ArmorMaterial material = new ArmorMaterial(durability, defense, 25, SoundEvents.ARMOR_EQUIP_LEATHER, (float) toughness,
				(float) knockback, REPAIRS, asset);
		ItemAttributeModifiers.Builder attributes = ItemAttributeModifiers.builder();
		EquipmentSlotGroup feet = EquipmentSlotGroup.FEET;
		attributes.add(Attributes.ARMOR, new AttributeModifier(HuntLike.id("hide_boots.armor"), armor,
				AttributeModifier.Operation.ADD_VALUE), feet);
		if (toughness > 0) {
			attributes.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(HuntLike.id("hide_boots.toughness"), toughness,
					AttributeModifier.Operation.ADD_VALUE), feet);
		}
		if (knockback > 0) {
			attributes.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(HuntLike.id("hide_boots.knockback"), knockback,
					AttributeModifier.Operation.ADD_VALUE), feet);
		}
		if (speed > 0) {
			attributes.add(Attributes.MOVEMENT_SPEED, new AttributeModifier(HuntLike.id("hide_boots.speed"), speed,
					AttributeModifier.Operation.ADD_MULTIPLIED_BASE), feet);
		}
		List<Component> lore = new ArrayList<>();
		for (String line : lines) {
			Lore.gray(lore, line);
		}
		Item.Properties properties = new Item.Properties()
				.humanoidArmor(material, ArmorType.BOOTS)
				.attributes(attributes.build())
				.rarity(rarity)
				.component(DataComponents.LORE, new ItemLore(lore));
		if (fireproof) {
			properties.fireResistant();
		}
		return ModItems.register(name, Item::new, properties);
	}

	private static Item featherChute() {
		List<Component> lore = new ArrayList<>();
		Lore.gray(lore, "Already trademarked!");
		lore.add(Component.empty());
		Lore.gray(lore, "Grants Slow Falling I in the offhand after falling 3 blocks. Sneak to cancel.");
		Item.Properties properties = new Item.Properties()
				.durability(500)
				.repairable(net.minecraft.world.item.Items.FEATHER)
				.rarity(Rarity.UNCOMMON)
				.component(DataComponents.LORE, new ItemLore(lore));
		return ModItems.register("feather_chute", Item::new, properties);
	}

	/** True when the entity's boots let it walk on Powder Snow. */
	public static boolean walksOnPowderSnow(Entity entity) {
		return entity instanceof LivingEntity living && BOOTS.contains(living.getItemBySlot(EquipmentSlot.FEET).getItem());
	}

	// ---------------------------------------------------------------- gliding

	private static final Set<UUID> GLIDING = new HashSet<>();
	private static final Map<UUID, Integer> CHUTE_TICKS = new HashMap<>();

	public static boolean isGliding(ServerPlayer player) {
		return GLIDING.contains(player.getUUID());
	}

	public static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			tickPlayer(player);
		}
	}

	private static void tickPlayer(ServerPlayer player) {
		UUID id = player.getUUID();
		boolean boots = GLIDING_BOOTS.contains(player.getItemBySlot(EquipmentSlot.FEET).getItem());
		ItemStack offhand = player.getItemBySlot(EquipmentSlot.OFFHAND);
		boolean chute = !boots && offhand.is(FEATHER_CHUTE);
		boolean grounded = player.onGround() || player.isInWater() || player.isInLava() || player.isFallFlying()
				|| player.getAbilities().flying || player.isPassenger() || player.onClimbable() || player.isSpectator();
		if ((!boots && !chute) || grounded || player.isShiftKeyDown()) {
			GLIDING.remove(id);
			return;
		}
		if (!GLIDING.contains(id)) {
			if (player.fallDistance < GLIDE_AFTER || player.getDeltaMovement().y >= 0) {
				return;
			}
			GLIDING.add(id);
		}
		MobEffectInstance current = player.getEffect(MobEffects.SLOW_FALLING);
		if (current == null || current.getDuration() <= 5) {
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 5, 0, false, false, true));
		}
		if (chute) {
			int ticks = CHUTE_TICKS.merge(id, 1, Integer::sum);
			if (ticks >= CHUTE_TICKS_PER_DAMAGE) {
				CHUTE_TICKS.put(id, 0);
				offhand.hurtAndBreak(1, player, EquipmentSlot.OFFHAND);
			}
		}
	}

	public static void forget(ServerPlayer player) {
		GLIDING.remove(player.getUUID());
		CHUTE_TICKS.remove(player.getUUID());
	}

	public static void initialize() {
	}
}
