package com.huntlike.hunt;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import com.huntlike.HuntLike;
import com.huntlike.registry.ModItems;

/**
 * Low Quality Mystery Meat instantly tames a wild wolf. A Large Femur permanently doubles a tamed wolf's
 * health and damage (once per wolf).
 */
public final class WolfTreats {
	private WolfTreats() {
	}

	public static final net.minecraft.resources.Identifier FEMUR_HEALTH = HuntLike.id("large_femur.health");
	public static final net.minecraft.resources.Identifier FEMUR_DAMAGE = HuntLike.id("large_femur.damage");

	public static void initialize() {
		UseEntityCallback.EVENT.register(WolfTreats::onUseEntity);
	}

	private static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity,
			EntityHitResult hit) {
		if (player.isSpectator() || entity.getType() != EntityTypes.WOLF || !(entity instanceof Wolf wolf)) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (stack.is(ModItems.LOW_QUALITY_MYSTERY_MEAT) && !wolf.isTame()) {
			if (player instanceof ServerPlayer serverPlayer) {
				tame(serverPlayer, wolf, stack);
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(ModItems.LARGE_FEMUR) && wolf.isTame() && wolf.isOwnedBy(player)) {
			if (player instanceof ServerPlayer serverPlayer) {
				return giveFemur(serverPlayer, wolf, stack) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	public static void tame(ServerPlayer player, Wolf wolf, ItemStack stack) {
		stack.consume(1, player);
		wolf.stopBeingAngry();
		wolf.setTarget(null);
		wolf.getNavigation().stop();
		wolf.tame(player);
		wolf.setOrderedToSit(true);
		wolf.level().broadcastEntityEvent(wolf, (byte) 7);
	}

	public static boolean hasFemur(Wolf wolf) {
		AttributeInstance health = wolf.getAttribute(Attributes.MAX_HEALTH);
		return health != null && health.hasModifier(FEMUR_HEALTH);
	}

	/** Doubles the wolf's health and damage for good. False if it already had a Femur. */
	public static boolean giveFemur(ServerPlayer player, Wolf wolf, ItemStack stack) {
		if (hasFemur(wolf)) {
			return false;
		}
		stack.consume(1, player);
		wolf.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(
				new AttributeModifier(FEMUR_HEALTH, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		AttributeInstance damage = wolf.getAttribute(Attributes.ATTACK_DAMAGE);
		if (damage != null) {
			damage.addPermanentModifier(new AttributeModifier(FEMUR_DAMAGE, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		wolf.setHealth(wolf.getMaxHealth());
		ServerLevel level = (ServerLevel) wolf.level();
		level.sendParticles(ParticleTypes.HEART, wolf.getX(), wolf.getY() + 0.8, wolf.getZ(), 7, 0.4, 0.4, 0.4, 0.0);
		level.playSound(null, wolf.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 1.0F, 0.8F);
		QuestManager.award(player, "animal/happier_dog");
		return true;
	}
}
