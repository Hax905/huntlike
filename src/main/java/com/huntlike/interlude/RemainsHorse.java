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
 * The skeleton horse called by the Slime Rider Remains: 40 health, fast and a high jumper. Its saddle can't be taken
 * off, sheared or dropped. If it dies, the Remains wait 3 minutes before calling it again.
 */
public class RemainsHorse extends SkeletonHorse {
	public RemainsHorse(EntityType<? extends SkeletonHorse> type, Level level) {
		super(type, level);
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
				player.getCooldowns().addCooldown(new ItemStack(ModInterlude.SLIME_RIDER_REMAINS), RemainsItem.DEATH_COOLDOWN);
				player.sendSystemMessage(com.huntlike.hunt.Messages.c("Your skeleton horse fell. The Remains need 3 minutes to call it again.",
						0xAAAAAA));
			}
			RemainsItem.forget(owner, this.getUUID());
		}
	}

	@Override
	public void tick() {
		super.tick();
		// A horse from an older call (left loaded somewhere) goes away once a new one exists.
		if (this.level() instanceof ServerLevel && this.tickCount % 20 == 0 && this.getOwnerReference() != null
				&& !RemainsItem.isCurrent(this.getOwnerReference().getUUID(), this.getUUID())) {
			this.discard();
		}
	}
}
