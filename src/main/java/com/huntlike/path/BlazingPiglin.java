package com.huntlike.path;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A piglin fed a Blazing Heart: 60 health, 10 contact damage, 20% bigger, no abilities. It fights the player who fed it
 * and drops a Blazing Heart ★. (Piglin model with a white outline until its own sprite is ready.)
 */
public class BlazingPiglin extends Piglin {
	public static final float HEALTH = 60.0F;
	public static final float DAMAGE = 10.0F;

	public BlazingPiglin(EntityType<? extends AbstractPiglin> type, Level level) {
		super(type, level);
		this.setImmuneToZombification(true);
		this.setPersistenceRequired();
		this.xpReward = 20;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Piglin.createAttributes().add(Attributes.MAX_HEALTH, HEALTH).add(Attributes.ATTACK_DAMAGE, DAMAGE)
				.add(Attributes.SCALE, 1.2).add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true));
	}

	/** No piglin brain (bartering, fleeing, zombifying): it only hunts its feeder. */
	@Override
	protected void customServerAiStep(ServerLevel level) {
		BlazingMob.huntFeeder(this, level);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		this.spawnAtLocation(level, new ItemStack(ModPaths.BLAZING_HEART_STAR));
	}
}
