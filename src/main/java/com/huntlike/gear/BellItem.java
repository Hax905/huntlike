package com.huntlike.gear;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Ring it (right-click): heals, then Absorption for 10 seconds; 10-second cooldown. */
public class BellItem extends Item {
	private static final int COOLDOWN = 20 * 10;

	private final float heal;
	private final int absorptionLevel;

	public BellItem(Properties properties, float heal, int absorptionLevel) {
		super(properties);
		this.heal = heal;
		this.absorptionLevel = absorptionLevel;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(stack)) {
			return InteractionResult.PASS;
		}
		level.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0F, 1.2F);
		if (!level.isClientSide()) {
			player.heal(heal);
			player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, COOLDOWN, absorptionLevel));
		}
		player.getCooldowns().addCooldown(stack, COOLDOWN);
		return InteractionResult.SUCCESS;
	}
}
