package com.huntlike.pet;

import java.util.UUID;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

import com.huntlike.registry.ModItems;

/**
 * A Slimy Core surrounded by slime balls. Crafting copies which pet the core was; used on the ground, that pet comes back as
 * it was (size, upgrades, chest).
 */
public class RevivedCoreItem extends Item {
	public RevivedCoreItem(Properties properties) {
		super(properties);
	}

	/** Runs while the crafting grid still holds the core: carry its pet over to the result. */
	@Override
	public void onCraftedBy(ItemStack stack, Player player) {
		super.onCraftedBy(stack, player);
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		for (Slot slot : player.containerMenu.slots) {
			if (slot.container instanceof CraftingContainer && slot.getItem().is(ModItems.SLIME_PET_CORE)) {
				ItemStack core = slot.getItem();
				stack.set(DataComponents.CUSTOM_DATA, core.get(DataComponents.CUSTOM_DATA));
				PetState pet = PetManager.pet(serverPlayer, PetManager.corePet(core));
				String name = pet != null ? pet.name : core.getHoverName().getString().replace(" Slimy Core", "");
				stack.set(DataComponents.CUSTOM_NAME, Component.literal("Revived " + name).withStyle(style -> style.withItalic(false)));
				return;
			}
		}
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getPlayer() instanceof ServerPlayer player) {
			UUID petId = PetManager.corePet(context.getItemInHand());
			Vec3 pos = Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
			if (petId != null && PetManager.revive(player, pos, petId)) {
				context.getItemInHand().consume(1, player);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.FAIL;
		}
		return InteractionResult.SUCCESS;
	}
}
