package com.huntlike.path;

import java.util.EnumSet;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * An Imp, from a spawner given a Blazing Heart ★★★: a blaze with twice its health (40) and damage (12 contact, fireballs x2),
 * shooting twice as often. A small, very dark red blaze with three rods and a horn.
 */
public class Imp extends Blaze {
	public Imp(EntityType<? extends Blaze> type, Level level) {
		super(type, level);
		this.xpReward = 20;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Blaze.createAttributes().add(Attributes.MAX_HEALTH, 40.0).add(Attributes.ATTACK_DAMAGE, 12.0);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		// Its own attack: the blaze's, twice as fast.
		this.goalSelector.removeAllGoals(goal -> goal.getClass().getName().contains("BlazeAttackGoal"));
		this.goalSelector.addGoal(4, new ImpAttackGoal(this));
	}

	/** The blaze's attack at double speed: a 1.5 s charge, three fireballs 3 ticks apart, then 2.5 s of rest. */
	static class ImpAttackGoal extends Goal {
		private final Imp imp;
		private int attackStep;
		private int attackTime;
		private int lastSeen;

		ImpAttackGoal(Imp imp) {
			this.imp = imp;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = imp.getTarget();
			return target != null && target.isAlive() && imp.canAttack(target);
		}

		@Override
		public void start() {
			attackStep = 0;
		}

		@Override
		public void stop() {
			lastSeen = 0;
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			attackTime--;
			LivingEntity target = imp.getTarget();
			if (target == null) {
				return;
			}
			boolean visible = imp.getSensing().hasLineOfSight(target);
			lastSeen = visible ? 0 : lastSeen + 1;
			double dist = imp.distanceToSqr(target);
			if (dist < 4.0) {
				if (!visible) {
					return;
				}
				if (attackTime <= 0) {
					attackTime = 10;
					imp.doHurtTarget((ServerLevel) imp.level(), target);
				}
				imp.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
			} else if (dist < followDistance() * followDistance() && visible) {
				double dx = target.getX() - imp.getX();
				double dy = target.getY(0.5) - imp.getY(0.5);
				double dz = target.getZ() - imp.getZ();
				if (attackTime <= 0) {
					attackStep++;
					if (attackStep == 1) {
						attackTime = 30;
					} else if (attackStep <= 4) {
						attackTime = 3;
					} else {
						attackTime = 50;
						attackStep = 0;
					}
					if (attackStep > 1) {
						double spread = Math.sqrt(Math.sqrt(dist)) * 0.5;
						if (!imp.isSilent()) {
							imp.level().levelEvent(null, 1018, imp.blockPosition(), 0);
						}
						Vec3 dir = new Vec3(imp.getRandom().triangle(dx, 2.297 * spread), dy, imp.getRandom().triangle(dz, 2.297 * spread));
						SmallFireball fireball = new SmallFireball(imp.level(), imp, dir.normalize());
						fireball.setPos(fireball.getX(), imp.getY(0.5) + 0.5, fireball.getZ());
						imp.level().addFreshEntity(fireball);
					}
				}
				imp.getLookControl().setLookAt(target, 10.0F, 10.0F);
			} else if (lastSeen < 5) {
				imp.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0);
			}
		}

		private double followDistance() {
			return imp.getAttributeValue(Attributes.FOLLOW_RANGE);
		}
	}
}
