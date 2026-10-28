package com.huntlike.interlude;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Pom's Skeletal Remains: a pile of bones. Right-click: bone particles at the ground for a moment, the skeleton-hurt sound,
 * and a saddled skeleton horse appears with you on it. Right-click while it's out: it vanishes. Never used up.
 */
public class RemainsItem extends Item {
	public static final int DEATH_COOLDOWN = 3 * 60 * 20;
	/** Each player's current horse (only one at a time). */
	private static final Map<UUID, UUID> HORSES = new HashMap<>();

	/** Horseshoes it can take; each one is a star. */
	public static final int MAX_STARS = 5;
	/** The horse's speed with no horseshoes; each star adds {@link #SPEED_PER_STAR}, up to 0.36 at 5 stars. */
	public static final double BASE_SPEED = 0.24;
	public static final double SPEED_PER_STAR = 0.024;

	public RemainsItem(Properties properties) {
		super(properties);
	}

	public static int stars(ItemStack stack) {
		return Math.max(0, Math.min(MAX_STARS, stack.getOrDefault(ModInterlude.HORSESHOES, 0)));
	}

	public static double speed(int stars) {
		return BASE_SPEED + SPEED_PER_STAR * stars;
	}

	@Override
	public net.minecraft.network.chat.Component getName(ItemStack stack) {
		int stars = stars(stack);
		if (stars == 0) {
			return super.getName(stack);
		}
		return net.minecraft.network.chat.Component.empty().append(super.getName(stack))
				.append(net.minecraft.network.chat.Component.literal(" " + "\u2605".repeat(stars)).withColor(0xFFAA00));
	}

	static boolean isCurrent(UUID owner, UUID horse) {
		return horse.equals(HORSES.get(owner));
	}

	static void forget(UUID owner, UUID horse) {
		HORSES.remove(owner, horse);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.SUCCESS;
		}
		UUID current = HORSES.get(player.getUUID());
		Entity existing = current == null ? null : serverLevel.getEntity(current);
		if (existing instanceof RemainsHorse horse && horse.isAlive()) {
			serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.BONE),
					horse.getX(), horse.getY() + 0.8, horse.getZ(), 20, 0.4, 0.5, 0.4, 0.05);
			level.playSound(null, horse.blockPosition(), SoundEvents.SKELETON_HORSE_DEATH, SoundSource.PLAYERS, 0.6F, 1.4F);
			HORSES.remove(player.getUUID());
			horse.discard();
			return InteractionResult.SUCCESS;
		}
		RemainsHorse horse = ModInterlude.REMAINS_HORSE.create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
		if (horse == null) {
			return InteractionResult.FAIL;
		}
		horse.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
		horse.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0);
		horse.setHealth(40.0F);
		horse.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed(stars(player.getItemInHand(hand))));
		horse.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(1.0);
		horse.setTamed(true);
		horse.setOwner(player);
		horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
		serverLevel.addFreshEntity(horse);
		HORSES.put(player.getUUID(), horse.getUUID());
		player.startRiding(horse);
		// Bones thrown at the ground, and the skeleton's hurt sound.
		serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.BONE),
				player.getX(), player.getY() + 0.2, player.getZ(), 30, 0.6, 0.1, 0.6, 0.15);
		serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.BONE_BLOCK.defaultBlockState()),
				player.getX(), player.getY() + 0.1, player.getZ(), 20, 0.5, 0.05, 0.5, 0.1);
		level.playSound(null, player.blockPosition(), SoundEvents.SKELETON_HURT, SoundSource.PLAYERS, 1.0F, 1.0F);
		return InteractionResult.SUCCESS;
	}
}
