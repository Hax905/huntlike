package com.huntlike.registry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.huntlike.network.HuntPayloads;

/** Detect Nearby Souls: use it to highlight the nearest living entity for 3 seconds, seen only by you. */
public class SpiritualCatalystItem extends Item {
	public static final int HIGHLIGHT_TICKS = 60;
	public static final double RANGE = 32.0;

	/** How many souls one use highlights (1 for the Catalyst, 3 for the Spiritual Compass). */
	private final int souls;

	public SpiritualCatalystItem(Properties properties, int souls) {
		super(properties);
		this.souls = souls;
	}

	public static List<Component> compassLore() {
		List<Component> lore = new ArrayList<>();
		Lore.ability(lore, "Detect Nearby Souls", "Highlights the 3 nearest living entities for 3 seconds.");
		return lore;
	}

	public static List<Component> lore() {
		List<Component> lore = new ArrayList<>();
		Lore.gray(lore, Lore.english("item.huntlike.spiritual_catalyst.desc"));
		Lore.ability(lore, "Detect Nearby Souls", "Highlights the nearest living entity for 3 seconds.");
		return lore;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			List<LivingEntity> nearest = findNearest(serverPlayer, souls);
			if (!nearest.isEmpty()) {
				for (LivingEntity soul : nearest) {
					ServerPlayNetworking.send(serverPlayer, new HuntPayloads.Highlight(soul.getId(), HIGHLIGHT_TICKS));
				}
				level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
			} else {
				level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6F, 0.6F);
			}
			serverPlayer.getCooldowns().addCooldown(player.getItemInHand(hand), HIGHLIGHT_TICKS);
		}
		return InteractionResult.SUCCESS;
	}

	public static LivingEntity findNearest(ServerPlayer player) {
		List<LivingEntity> nearest = findNearest(player, 1);
		return nearest.isEmpty() ? null : nearest.get(0);
	}

	public static List<LivingEntity> findNearest(ServerPlayer player, int count) {
		return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RANGE),
				entity -> entity != player && entity.isAlive() && !entity.isSpectator() && !(entity instanceof ArmorStand)
						&& entity.distanceTo(player) <= RANGE)
				.stream().sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(player))).limit(count).toList();
	}
}
