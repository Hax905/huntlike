package com.huntlike.rogue;

import java.util.UUID;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * A Rogue Hunter: hunts one player during their Hunt Quests. Invisible (only the hunted player sees its red outline) until
 * it's hit the first time. 40 health and 8 damage; a special one has 80 and 12.
 */
public class RogueHunter extends Monster {
	public static final float HEALTH = 40.0F;
	public static final float DAMAGE = 8.0F;
	public static final float SPECIAL_HEALTH = 80.0F;
	public static final float SPECIAL_DAMAGE = 12.0F;
	/** Red outline color. */
	public static final int OUTLINE = 0xFF2020;

	private static final EntityDataAccessor<String> TARGET = SynchedEntityData.defineId(RogueHunter.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Boolean> SPECIAL = SynchedEntityData.defineId(RogueHunter.class, EntityDataSerializers.BOOLEAN);

	public RogueHunter(EntityType<? extends RogueHunter> type, Level level) {
		super(type, level);
		this.xpReward = 0;
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, HEALTH)
				.add(Attributes.ATTACK_DAMAGE, DAMAGE)
				// About a player's running speed.
				.add(Attributes.MOVEMENT_SPEED, 0.33)
				.add(Attributes.FOLLOW_RANGE, 64.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(TARGET, "");
		builder.define(SPECIAL, false);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
	}

	/** The hunted player's UUID (synced: only they see the outline). */
	public UUID hunted() {
		String id = this.entityData.get(TARGET);
		try {
			return id.isEmpty() ? null : UUID.fromString(id);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public void setHunted(UUID player) {
		this.entityData.set(TARGET, player.toString());
	}

	public boolean isSpecial() {
		return this.entityData.get(SPECIAL);
	}

	public void setSpecial(boolean special) {
		this.entityData.set(SPECIAL, special);
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(special ? SPECIAL_HEALTH : HEALTH);
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(special ? SPECIAL_DAMAGE : DAMAGE);
		this.setHealth(this.getMaxHealth());
	}

	/** A player's reach: about 3 blocks. */
	@Override
	protected AABB getAttackBoundingBox(double range) {
		return this.getBoundingBox().inflate(2.4, 0.5, 2.4);
	}

	@Override
	public int getTeamColor() {
		return OUTLINE;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt && this.isInvisible()) {
			this.setInvisible(false);
		}
		return hurt;
	}

	@Override
	public void checkDespawn() {
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel level) {
			RogueHunters.tickHunter(this, level);
		}
	}
}
