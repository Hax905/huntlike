package com.huntlike.interlude;

import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The skeleton horse called by the Pom's Skeletal Remains: 40 health, fast and a high jumper. Its saddle can't be taken
 * off, sheared or dropped. If it dies, the Remains wait 3 minutes before calling it again.
 */
public class RemainsHorse extends SkeletonHorse {
	public RemainsHorse(EntityType<? extends SkeletonHorse> type, Level level) {
		super(type, level);
	}

	/**
	 * Always saddled, on both sides. The saddle item set on the server didn't always reach the client, and a client that
	 * thinks the horse has no saddle won't let its rider steer it.
	 */
	@Override
	public boolean isSaddled() {
		return true;
	}

	@Override
	protected boolean canShearEquipment(Player player) {
		return false;
	}

	@Override
	public void openCustomInventoryScreen(Player player) {
	}

	@Override
	protected void dropEquipment(ServerLevel level) {
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (this.level() instanceof ServerLevel level && this.getOwnerReference() != null) {
			UUID owner = this.getOwnerReference().getUUID();
			ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
			if (player != null) {
				player.getCooldowns().addCooldown(new ItemStack(ModInterlude.POMS_SKELETAL_REMAINS), RemainsItem.DEATH_COOLDOWN);
				player.sendSystemMessage(com.huntlike.hunt.Messages.c("Your skeleton horse fell. The Remains need 3 minutes to call it again.",
						0xAAAAAA));
			}
			RemainsItem.forget(owner, this.getUUID());
		}
	}

	@Override
	public void tick() {
		super.tick();
		// Show the saddle on the client too: put it on again a moment after spawning, so the change is sent.
		if (this.level() instanceof ServerLevel && this.tickCount == 2) {
			this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.SADDLE, ItemStack.EMPTY);
		} else if (this.level() instanceof ServerLevel && this.tickCount == 3) {
			this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.SADDLE, new ItemStack(net.minecraft.world.item.Items.SADDLE));
		}
		// A horse from an older call (left loaded somewhere) goes away once a new one exists.
		if (this.level() instanceof ServerLevel && this.tickCount % 20 == 0 && this.getOwnerReference() != null
				&& !RemainsItem.isCurrent(this.getOwnerReference().getUUID(), this.getUUID())) {
			this.discard();
		}
	}
}
