package com.huntlike.gear;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.huntlike.entity.BossEgg;
import com.huntlike.hunt.BossAbilities;
import com.huntlike.hunt.BossController;
import com.huntlike.hunt.ClusterFight;
import com.huntlike.hunt.ClusterQuota;
import com.huntlike.hunt.QuestManager;

/** What the Animal Hunt Weapons and Armor do in a fight. */
public final class GearEffects {
	private GearEffects() {
	}

	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	/** Momentum stacks: boss -> player -> hits. */
	private static final Map<UUID, Map<UUID, Integer>> MOMENTUM = new HashMap<>();

	/** Animal mobs (the quota animals), Animal bosses and their herds. */
	public static boolean isAnimal(Entity entity) {
		return entity.getType().builtInRegistryHolder().is(QuestManager.QUOTA_ANIMALS)
				|| entity instanceof LivingEntity living && BossController.isBoss(living) && !ClusterFight.isBody(entity)
				|| BossAbilities.isHerd(entity);
	}

	/** Cluster mobs: slimes, magma cubes and the Cluster bosses' bodies. */
	public static boolean isCluster(Entity entity) {
		return ClusterQuota.isClusterMob(entity) || ClusterFight.isBody(entity);
	}

	/** Slippery: a full set of Cluster Hunt Armor. */
	public static boolean slippery(Player player) {
		ModGear.ArmorSet set = fullSet(player);
		return set != null && set.hunt().equals("cluster");
	}

	/** Better Than Ever!: a full set of Gel or Thermal Armor. */
	public static boolean betterThanEver(Player player) {
		ModGear.ArmorSet set = fullSet(player);
		return set == ModGear.GEL || set == ModGear.THERMAL;
	}

	/** Room Temperature: a full set of Thermal Armor. */
	public static boolean roomTemperature(Player player) {
		return fullSet(player) == ModGear.THERMAL;
	}

	/**
	 * Room Temperature, every tick: no burning, freezing or Slowness. Fire from a Cluster boss (Watch out!) still burns.
	 */
	public static void tick(net.minecraft.server.MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!roomTemperature(player)) {
				continue;
			}
			if (player.getRemainingFireTicks() > 0 && !ClusterFight.bossBurning(player)) {
				player.clearFire();
			}
			if (player.getTicksFrozen() > 0) {
				player.setTicksFrozen(0);
			}
			if (player.hasEffect(MobEffects.SLOWNESS)) {
				player.removeEffect(MobEffects.SLOWNESS);
			}
		}
	}

	/** Called with every damage amount, before armor: Biohazard, Momentum, Fracture and Animalistic. */
	public static float adjustDamage(LivingEntity target, DamageSource source, float amount) {
		Entity attacker = source.getEntity();
		if (attacker instanceof ServerPlayer player && source.getDirectEntity() == player && isAnimal(target)) {
			int stage = ModGear.weaponStage(player.getMainHandItem().getItem());
			if (stage > 0) {
				amount *= ModGear.BIOHAZARD[stage];
				if (stage >= 3 && BossController.isBoss(target)) {
					Map<UUID, Integer> stacks = MOMENTUM.computeIfAbsent(target.getUUID(), id -> new HashMap<>());
					int hits = stacks.getOrDefault(player.getUUID(), 0);
					amount *= 1.0F + Math.min(ModGear.MOMENTUM_CAP[stage], ModGear.MOMENTUM_STEP[stage] * hits);
					stacks.put(player.getUUID(), hits + 1);
				}
				if (stage >= 5) {
					target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20 * 3, 1), player);
				}
			}
		}
		if (target instanceof Player player && attacker != null && isAnimal(attacker) && !source.is(BossAbilities.SOUL_STRIKE)) {
			ModGear.ArmorSet set = fullSet(player);
			if (set != null && set.hunt().equals("animal")) {
				amount *= 1.0F - set.animalReduction();
			}
		}
		if (target instanceof Player player && attacker != null && isCluster(attacker)) {
			ModGear.ArmorSet set = fullSet(player);
			if (set != null && set.hunt().equals("cluster")) {
				amount *= 1.0F - set.animalReduction();
			}
		}
		return amount;
	}

	/** Egg Shell: a full set of Animal Hunt Armor ignores Egg Blaster's eggs. */
	public static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (entity instanceof Player player && roomTemperature(player)) {
			if (source.is(net.minecraft.tags.DamageTypeTags.IS_FREEZING)) {
				return false;
			}
			if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) && !(source.getEntity() != null && isCluster(source.getEntity()))
					&& !(player instanceof ServerPlayer serverPlayer && ClusterFight.bossBurning(serverPlayer))) {
				return false;
			}
		}
		if (entity instanceof Player player && source.getDirectEntity() instanceof BossEgg) {
			return armorPieces(player) < 4;
		}
		return true;
	}

	/** The armor set the player wears all four pieces of, or null. */
	public static ModGear.ArmorSet fullSet(Player player) {
		ModGear.ArmorSet first = null;
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			ModGear.ArmorSet set = ModGear.armorSetOf(player.getItemBySlot(slot).getItem());
			if (set == null || first != null && set != first) {
				return null;
			}
			first = set;
		}
		return first;
	}

	/** How many Animal Hunt Armor pieces the player wears. */
	public static int armorPieces(Player player) {
		int pieces = 0;
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			ModGear.ArmorSet set = ModGear.armorSetOf(player.getItemBySlot(slot).getItem());
			if (set != null && set.hunt().equals("animal")) {
				pieces++;
			}
		}
		return pieces;
	}

	/** Ecocide Armor: 20% less Animalia Lunge knockback per piece. */
	public static double lungeResistance(Player player) {
		double resistance = 0.0;
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			ModGear.ArmorSet set = ModGear.armorSetOf(player.getItemBySlot(slot).getItem());
			if (set != null) {
				resistance += set.lungeResistance();
			}
		}
		return Math.min(1.0, resistance);
	}

	/** Bountiful!: a kill with an Animal Hunt Weapon counts double. */
	public static int quotaKills(ServerPlayer killer) {
		ItemStack hand = killer.getMainHandItem();
		return ModGear.weaponStage(hand.getItem()) > 0 ? 2 : 1;
	}

	public static void forgetBoss(UUID boss) {
		MOMENTUM.remove(boss);
	}
}
