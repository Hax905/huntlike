package com.huntlike.pet;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/** A Slimy Core surrounded by slime balls: used on the ground, the pet comes back as it was. */
public class RevivedCoreItem extends Item {
	public RevivedCoreItem(Properties properties) {
		super(properties);
	}

	@Override
	public void onCraftedBy(ItemStack stack, Player player) {
		super.onCraftedBy(stack, player);
		if (player instanceof ServerPlayer serverPlayer) {
			PetState state = PetManager.state(serverPlayer);
			if (!state.name.isEmpty()) {
				stack.set(DataComponents.CUSTOM_NAME, Component.literal("Revived " + state.name).withStyle(style -> style.withItalic(false)));
			}
		}
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getPlayer() instanceof ServerPlayer player) {
			Vec3 pos = Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
			if (PetManager.revive(player, pos)) {
				context.getItemInHand().consume(1, player);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.FAIL;
		}
		return InteractionResult.SUCCESS;
	}
}
