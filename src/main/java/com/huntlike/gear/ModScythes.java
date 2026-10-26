package com.huntlike.gear;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import com.huntlike.entity.cluster.TheVile;
import com.huntlike.hunt.ClusterFight;
import com.huntlike.registry.Lore;
import com.huntlike.registry.ModItems;

/**
 * The Cluster Hunt Weapons (scythes): Slice and Dice, Swing (Attack Sweep) and Sublimation (Temperature).
 * Damage to Cluster creatures = (hit + Cluster bonus) x (1 + Temperature / 100) when the Temperature is the right one.
 */
public final class ModScythes {
	private ModScythes() {
	}

	/** One scythe stage. Sweep is on top of the vanilla sweep's 1 block. */
	public record Stats(int stage, String id, String name, String act, float damage, float speed, int clusterBonus,
			int temperature, int sweep, Rarity rarity, String description) {
	}

	public static final List<Stats> STATS = List.of(
			new Stats(1, "slick_kama", "Slick Kama", "Genesis", 4, 1.6F, 2, 0, 0, Rarity.UNCOMMON,
					"A kama forged with iron converted into a cluster killing weapon."),
			new Stats(2, "polished_khopesh", "Polished Khopesh", "Act I", 4, 1.6F, 3, 0, 1, Rarity.UNCOMMON,
					"The weapon was modified to cut clusters with less effort."),
			new Stats(3, "slimy_falx", "Slimy Falx", "Act II", 4, 1.6F, 4, 5, 2, Rarity.RARE,
					"A system similar to a cluster's biological immune system is growing inside the metal."),
			new Stats(4, "slimysteel_glaive", "Slimysteel Glaive", "Act III", 4, 1.6F, 5, 15, 3, Rarity.RARE,
					"The weapon's blade can now change between two states, cold and hot."),
			new Stats(5, "scythe_of_sublimation", "Scythe of Sublimation", "Apotheosis", 5, 1.6F, 5, 30, 3, Rarity.EPIC,
					"The now slimy scythe changes states aggressively in no time."));

	public static final Item SLICK_KAMA = scythe(STATS.get(0));
	public static final Item POLISHED_KHOPESH = scythe(STATS.get(1));
	public static final Item SLIMY_FALX = scythe(STATS.get(2));
	public static final Item SLIMYSTEEL_GLAIVE = scythe(STATS.get(3));
	public static final Item SCYTHE_OF_SUBLIMATION = scythe(STATS.get(4));

	public static final List<Item> SCYTHES = List.of(SLICK_KAMA, POLISHED_KHOPESH, SLIMY_FALX, SLIMYSTEEL_GLAIVE, SCYTHE_OF_SUBLIMATION);

	/** Sublimation's switching cooldown, in ticks. */
	public static final int SWITCH_COOLDOWN = 40;
	/** Vanilla sweep range around the hit mob, in blocks. */
	public static final double VANILLA_SWEEP = 1.0;
	private static final String FROST_KEY = "huntlike_frost";

	private static Item scythe(Stats stats) {
		ItemAttributeModifiers attributes = ItemAttributeModifiers.builder()
				.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, stats.damage() - 1.0,
						AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, stats.speed() - 4.0,
						AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.build();
		Item.Properties properties = new Item.Properties()
				.stacksTo(1)
				.attributes(attributes)
				.enchantable(14)
				.rarity(stats.rarity())
				.component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
				.component(DataComponents.LORE, new ItemLore(lore(stats, false)));
		return ModItems.register(stats.id(), p -> new ScytheItem(p, stats), properties);
	}

	/** Tooltip: description, abilities, then the Cluster stats (Temperature shows the current state). */
	public static List<Component> lore(Stats stats, boolean frost) {
		List<Component> lore = new ArrayList<>();
		Lore.gray(lore, stats.description());
		Lore.ability(lore, "Slice and Dice", "Cluster kills count double towards the quota.");
		if (stats.sweep() > 0) {
			Lore.ability(lore, "Swing", "Sweep damage will affect more creatures around it.");
		}
		if (stats.temperature() > 0) {
			Lore.ability(lore, "Sublimation", "Your weapon can now change its temperature from pure energy.");
			Lore.gray(lore, "Sneak + right-click to switch.");
		}
		lore.add(Component.empty());
		lore.add(stat("+" + stats.clusterBonus() + " Cluster Damage", ChatFormatting.BLUE));
		if (stats.sweep() > 0) {
			lore.add(stat("+" + stats.sweep() + " Attack Sweep", ChatFormatting.BLUE));
		}
		if (stats.temperature() > 0) {
			lore.add(frost ? stat("Temperature: -" + stats.temperature() + "° (Freezing)", ChatFormatting.AQUA)
					: stat("Temperature: +" + stats.temperature() + "° (Searing)", ChatFormatting.GOLD));
		}
		return lore;
	}

	private static Component stat(String text, ChatFormatting color) {
		return Component.literal(text).withStyle(style -> style.withColor(color).withItalic(false));
	}

	public static Stats stats(ItemStack stack) {
		return stack.getItem() instanceof ScytheItem scythe ? scythe.stats : null;
	}

	/** Negative Temperature (frost look). New scythes start positive. */
	public static boolean isFrost(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && data.copyTag().getBooleanOr(FROST_KEY, false);
	}

	public static void setFrost(ItemStack stack, boolean frost) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(FROST_KEY, frost));
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(frost), List.of(), List.of()));
		stack.set(DataComponents.LORE, new ItemLore(lore(stats(stack), frost)));
	}

	/** Magma cubes and The Vile's magma core are weak to negative Temperature; slimes to positive. */
	public static boolean magmaLike(Entity entity) {
		return entity instanceof MagmaCube || entity instanceof TheVile vile && vile.isMagmaCore();
	}

	/** (amount + Cluster bonus) x Temperature, for a scythe hit on a Cluster creature. */
	public static float clusterDamage(ServerPlayer player, ItemStack stack, Entity target, float amount) {
		Stats stats = stats(stack);
		float damage = amount + stats.clusterBonus();
		if (stats.temperature() > 0 && isFrost(stack) == magmaLike(target)) {
			damage *= 1.0F + stats.temperature() / 100.0F * ClusterFight.temperatureMultiplier(player);
		}
		return damage;
	}

	// ---------------------------------------------------------------- sweep

	private static final Set<UUID> SWEEPING = new HashSet<>();

	/** A full-strength scythe hit (any crit included) sweeps every mob around the target for the scythe's attack damage. */
	public static void maybeSweep(ServerPlayer player, LivingEntity target, DamageSource source, float amount) {
		ItemStack stack = player.getMainHandItem();
		Stats stats = stats(stack);
		if (stats == null || !source.is(DamageTypes.PLAYER_ATTACK) || source.getDirectEntity() != player
				|| SWEEPING.contains(player.getUUID())) {
			return;
		}
		float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		if (amount < base * 0.9F) {
			return;
		}
		double range = VANILLA_SWEEP + stats.sweep();
		ServerLevel level = (ServerLevel) player.level();
		AABB box = target.getBoundingBox().inflate(range, 0.25, range);
		List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, box, entity -> entity != target && entity != player
				&& entity.isAlive() && !(entity instanceof Player) && !(entity instanceof ArmorStand) && !entity.isSpectator()
				&& !(entity instanceof TamableAnimal pet && pet.isOwnedBy(player))
				&& player.distanceToSqr(entity) < (3.0 + range) * (3.0 + range));
		SWEEPING.add(player.getUUID());
		try {
			for (LivingEntity entity : hit) {
				entity.knockback(0.4, Math.sin(player.getYRot() * (Math.PI / 180.0)), -Math.cos(player.getYRot() * (Math.PI / 180.0)));
				entity.hurtServer(level, level.damageSources().playerAttack(player), base);
			}
		} finally {
			SWEEPING.remove(player.getUUID());
		}
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
		double dx = -Math.sin(player.getYRot() * (Math.PI / 180.0));
		double dz = Math.cos(player.getYRot() * (Math.PI / 180.0));
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, player.getX() + dx, player.getY(0.5), player.getZ() + dz, 0, dx, 0.0, dz, 0.0);
		if (stats.temperature() > 0) {
			level.sendParticles(isFrost(stack) ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME, target.getX(), target.getY(0.5),
					target.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
		}
	}

	public static boolean isSweeping(Player player) {
		return SWEEPING.contains(player.getUUID());
	}

	/** Slice and Dice: a cluster finished with a scythe counts double. */
	public static int quotaMultiplier(ServerPlayer killer) {
		return stats(killer.getMainHandItem()) != null ? 2 : 1;
	}

	public static void initialize() {
	}

	/** A scythe: sneak + right-click switches the Temperature (Sublimation); a plain right-click lets the offhand act. */
	public static class ScytheItem extends Item {
		final Stats stats;

		public ScytheItem(Properties properties, Stats stats) {
			super(properties);
			this.stats = stats;
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (hand != InteractionHand.MAIN_HAND || stats.temperature() <= 0 || !player.isShiftKeyDown()) {
				return InteractionResult.PASS;
			}
			if (player.getCooldowns().isOnCooldown(stack)) {
				return InteractionResult.FAIL;
			}
			if (player instanceof ServerPlayer serverPlayer) {
				switchTemperature(serverPlayer, stack);
			}
			return InteractionResult.SUCCESS;
		}
	}

	public static void switchTemperature(ServerPlayer player, ItemStack stack) {
		boolean frost = !isFrost(stack);
		setFrost(stack, frost);
		player.getCooldowns().addCooldown(stack, SWITCH_COOLDOWN);
		ServerLevel level = (ServerLevel) player.level();
		level.sendParticles(frost ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME, player.getX(), player.getY(1.0), player.getZ(), 16,
				0.4, 0.4, 0.4, 0.02);
		level.playSound(null, player.blockPosition(), frost ? SoundEvents.GLASS_BREAK : SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS,
				0.7F, 1.2F);
		player.sendSystemMessage(frost ? Component.literal("Temperature: -" + stats(stack).temperature() + "° (Freezing)")
				.withStyle(ChatFormatting.AQUA) : Component.literal("Temperature: +" + stats(stack).temperature() + "° (Searing)")
				.withStyle(ChatFormatting.GOLD), true);
	}
}
