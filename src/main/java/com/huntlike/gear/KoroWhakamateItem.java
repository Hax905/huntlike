package com.huntlike.gear;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.huntlike.entity.CorrosiveStinger;

/**
 * Koro Whakamate: built-in Fire Aspect III and Poison IV (12 s); Fatal Touch lets its poison kill, and gives
 * poison-immune mobs Wither IV instead; Corrosive Stinger (right-click) fires a black stinger for 12 damage.
 */
public class KoroWhakamateItem extends Item {
	private static final int EFFECT_TICKS = 20 * 12;
	private static final int STINGER_COOLDOWN = 20 * 15;

	/** Mobs poisoned by the dagger, until the given game time: Fatal Touch finishes them at half a heart. */
	private static final Map<UUID, Long> FATAL = new HashMap<>();

	public KoroWhakamateItem(Properties properties) {
		super(properties);
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.hurtEnemy(stack, target, attacker);
		if (!(target.level() instanceof ServerLevel level)) {
			return;
		}
		target.igniteForSeconds(12.0F);
		MobEffectInstance poison = new MobEffectInstance(MobEffects.POISON, EFFECT_TICKS, 3);
		if (target.canBeAffected(poison)) {
			target.addEffect(poison, attacker);
			FATAL.put(target.getUUID(), level.getGameTime() + EFFECT_TICKS);
		} else {
			target.addEffect(new MobEffectInstance(MobEffects.WITHER, EFFECT_TICKS, 3), attacker);
		}
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(stack)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			CorrosiveStinger stinger = new CorrosiveStinger(level, player);
			stinger.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 0.0F);
			level.addFreshEntity(stinger);
		}
		level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
		player.getCooldowns().addCooldown(stack, STINGER_COOLDOWN);
		return InteractionResult.SUCCESS;
	}

	/** Fatal Touch: vanilla poison stops at half a heart; this dagger's poison doesn't. */
	public static void tick(MinecraftServer server) {
		if (FATAL.isEmpty() || server.getTickCount() % 5 != 0) {
			return;
		}
		FATAL.entrySet().removeIf(entry -> {
			for (ServerLevel level : server.getAllLevels()) {
				Entity entity = level.getEntity(entry.getKey());
				if (entity instanceof LivingEntity living) {
					if (!living.isAlive() || level.getGameTime() > entry.getValue() || !living.hasEffect(MobEffects.POISON)) {
						return true;
					}
					if (living.getHealth() <= 1.0F) {
						living.hurtServer(level, level.damageSources().magic(), 2.0F);
						return true;
					}
					return false;
				}
			}
			return true;
		});
	}
}
