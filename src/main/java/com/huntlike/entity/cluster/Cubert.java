package com.huntlike.entity.cluster;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.huntlike.hunt.ClusterFight;

/**
 * Tier I Cluster boss, Cubert, The Overslime Fragment.
 * Template entity: it looks like the vanilla slime for now. Its splitting (Mitosis) and abilities are run by
 * ClusterFight, never by vanilla splitting.
 */
public class Cubert extends Slime {
	public Cubert(EntityType<? extends Slime> type, Level level) {
		super(type, level);
	}

	@Override
	protected int getSplitCount() {
		return 0;
	}

	@Override
	protected void addTargetingGoals() {
	}

	@Override
	public void jumpFromGround() {
		super.jumpFromGround();
		ClusterFight.onJump(this);
	}

	@Override
	public void playerTouch(Player player) {
		if (ClusterFight.canTouch(this, player)) {
			super.playerTouch(player);
		}
	}

	@Override
	protected boolean canDealDamage() {
		return this.isEffectiveAi();
	}

	@Override
	protected float getAttackDamage() {
		return (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
	}
}
