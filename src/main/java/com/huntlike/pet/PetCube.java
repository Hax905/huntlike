package com.huntlike.pet;

import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.huntlike.entity.ClusterBall;

/**
 * A player's slime pet (or one of its pieces once split). It hops after its owner, fights what the owner fights, and a big
 * one can be ridden. Everything that must outlive this entity is kept in the owner's PetState; this entity only carries the
 * owner tag, the pet generation and its size.
 */
public abstract class PetCube extends AbstractCubeMob implements PlayerRideableJumping, HasCustomInventoryScreen {
	public static final String OWNER_PREFIX = "huntlike_pet:";
	public static final String GEN_PREFIX = "huntlike_petgen:";
	public static final String ID_PREFIX = "huntlike_petid:";

	/** Ridden: 110% of the player's sprinting speed (about 6.2 blocks/s), in blocks per tick. */
	public static final double RIDE_SPEED = 5.612 * 1.1 / 20.0;
	/** On water (or lava for a magma pet): 150% of the player's walking speed (about 6.5 blocks/s; it slides along). */
	public static final double SWIM_SPEED = 4.317 * 1.5 / 20.0;

	private int hopTimer;
	private int attackCooldown;
	private int ballCooldown;
	private boolean attackJump;
	private boolean hitThisJump;
	private boolean wasOnGround = true;
	private float jumpPending;
	private double closestToOwner = Double.MAX_VALUE;
	private int stuckTicks;

	protected PetCube(EntityType<? extends PetCube> type, Level level) {
		super(type, level);
		this.moveControl = new MoveControl(this);
		this.setPersistenceRequired();
	}

	/** Magma cube form (fire immune, swims in lava, sets targets on fire). */
	public abstract boolean isMagma();

	/** A big pet carrying its chest: the chest is drawn inside it. */
	private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> CHEST =
			net.minecraft.network.syncher.SynchedEntityData.defineId(PetCube.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CHEST, false);
	}

	public boolean hasChest() {
		return this.entityData.get(CHEST);
	}

	public void setChest(boolean chest) {
		if (hasChest() != chest) {
			this.entityData.set(CHEST, chest);
		}
	}

	// ---------------------------------------------------------------- identity

	public UUID ownerId() {
		for (String tag : this.entityTags()) {
			if (tag.startsWith(OWNER_PREFIX)) {
				try {
					return UUID.fromString(tag.substring(OWNER_PREFIX.length()));
				} catch (IllegalArgumentException e) {
					return null;
				}
			}
		}
		return null;
	}

	/** Which of the owner's pets this entity belongs to (null on pets from before several pets were allowed). */
	public UUID petId() {
		for (String tag : this.entityTags()) {
			if (tag.startsWith(ID_PREFIX)) {
				try {
					return UUID.fromString(tag.substring(ID_PREFIX.length()));
				} catch (IllegalArgumentException e) {
					return null;
				}
			}
		}
		return null;
	}

	public int generation() {
		for (String tag : this.entityTags()) {
			if (tag.startsWith(GEN_PREFIX)) {
				try {
					return Integer.parseInt(tag.substring(GEN_PREFIX.length()));
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		}
		return -1;
	}

	public boolean isOwnedBy(Entity entity) {
		return entity != null && entity.getUUID().equals(ownerId());
	}

	public ServerPlayer owner() {
		UUID id = ownerId();
		return id == null || !(this.level() instanceof ServerLevel level) ? null : level.getServer().getPlayerList().getPlayer(id);
	}

	// ---------------------------------------------------------------- vanilla behavior turned off

	@Override
	protected void registerGoals() {
	}

	@Override
	protected void addBehaviourGoals() {
	}

	@Override
	protected void addTargetingGoals() {
	}

	@Override
	protected int getSplitCount() {
		return 0;
	}

	@Override
	protected boolean canDealDamage() {
		return false;
	}

	@Override
	public void playerTouch(Player player) {
	}

	@Override
	public void checkDespawn() {
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public AbstractCubeMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return null;
	}

	// ---------------------------------------------------------------- server behavior

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel level && this.isAlive()) {
			PetManager.tickPiece(this, level);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (!PetManager.allowDamage(this, source)) {
			return false;
		}
		boolean hurt = super.hurtServer(level, source, amount);
		if (hurt) {
			PetManager.onPieceDamaged(this, level);
		}
		return hurt;
	}

	@Override
	public void remove(RemovalReason reason) {
		if (reason == RemovalReason.KILLED && this.level() instanceof ServerLevel level && this.isDeadOrDying()) {
			PetManager.onPieceKilled(this, level);
		}
		super.remove(reason);
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			return PetManager.interact(this, serverPlayer, hand);
		}
		return isOwnedBy(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}

	/** Attack damage by size: small 2, medium 4, big 6; a magma pet +2 and fire. */
	public float attackDamage() {
		int size = this.getSize();
		float damage = size >= 4 ? 6.0F : size >= 2 ? 4.0F : 2.0F;
		return isMagma() ? damage + 2.0F : damage;
	}

	/** Hops toward its target, or after its owner; contact damage; a slimeball when a jump attack misses. */
	void serverMove(ServerLevel level, ServerPlayer owner, PetState state, LivingEntity target) {
		if (attackCooldown > 0) {
			attackCooldown--;
		}
		if (ballCooldown > 0) {
			ballCooldown--;
		}
		boolean inFluid = this.isInWater() || isMagma() && this.isInLava();
		if (inFluid && (this.getFluidHeight(FluidTags.WATER) > 0.3 || this.getFluidHeight(FluidTags.LAVA) > 0.3)) {
			Vec3 v = this.getDeltaMovement();
			this.setDeltaMovement(v.x, Math.max(v.y, 0.08), v.z);
		}
		// Contact damage while touching the target.
		if (target != null && attackCooldown <= 0 && this.getBoundingBox().inflate(0.25).intersects(target.getBoundingBox())) {
			if (target.hurtServer(level, this.damageSources().mobAttack(this), attackDamage())) {
				if (isMagma()) {
					target.igniteForSeconds(4.0F);
				}
				this.playSound(this.getAttackSound(), 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
			}
			attackCooldown = 20;
			hitThisJump = true;
		}
		boolean onGround = this.onGround();
		if (onGround && !wasOnGround && attackJump) {
			if (!hitThisJump && target != null && target.isAlive() && ballCooldown <= 0) {
				throwBall(level, target);
			}
			attackJump = false;
		}
		wasOnGround = onGround;

		Vec3 destination = null;
		boolean sitting = state.sitting;
		if (target != null && !sitting) {
			destination = target.position();
		} else if (!sitting && owner != null && owner.level() == level && this.distanceToSqr(owner) > 3.5 * 3.5) {
			destination = owner.position();
		}
		if (destination == null) {
			return;
		}
		double dx = destination.x - this.getX();
		double dz = destination.z - this.getZ();
		double length = Math.sqrt(dx * dx + dz * dz);
		if (length < 0.01) {
			return;
		}
		float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
		this.setYRot(yaw);
		this.yBodyRot = yaw;
		this.yHeadRot = yaw;
		double speed = this.getSize() >= 4 ? 0.33 : 0.28;
		if (inFluid) {
			speed = SWIM_SPEED;
		}
		double vx = dx / length * Math.min(speed, length);
		double vz = dz / length * Math.min(speed, length);
		if (onGround) {
			if (--hopTimer <= 0) {
				this.jumpFromGround();
				Vec3 v = this.getDeltaMovement();
				this.setDeltaMovement(vx, v.y, vz);
				hopTimer = (target != null ? 10 : 16) + this.random.nextInt(8);
				attackJump = target != null;
				hitThisJump = false;
			}
		} else {
			Vec3 v = this.getDeltaMovement();
			this.setDeltaMovement(vx, v.y, vz);
		}
	}

	private void throwBall(ServerLevel level, LivingEntity target) {
		ClusterBall ball = new ClusterBall(level, this, isMagma() ? net.minecraft.world.item.Items.MAGMA_CREAM
				: net.minecraft.world.item.Items.SLIME_BALL, 4.0F, true);
		ball.setPos(this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ());
		double dx = target.getX() - ball.getX();
		double dz = target.getZ() - ball.getZ();
		double dy = target.getY(0.5) - ball.getY();
		double flat = Math.sqrt(dx * dx + dz * dz);
		ball.shoot(dx, dy + flat * 0.12, dz, 1.0F, 1.0F);
		level.addFreshEntity(ball);
		ballCooldown = 40;
		this.playSound(net.minecraft.sounds.SoundEvents.SNOWBALL_THROW, 1.0F, 0.6F);
	}

	protected abstract SoundEvent getAttackSound();

	/**
	 * Ticks spent more than 6 blocks from the owner without getting at least half a block closer (a stuck pet).
	 * Called once per tick with the current distance.
	 */
	public int stuckTicks(double distance) {
		if (distance <= 6.0) {
			resetStuck();
			closestToOwner = distance;
			return 0;
		}
		if (distance < closestToOwner - 0.5) {
			closestToOwner = distance;
			stuckTicks = 0;
		} else {
			stuckTicks++;
		}
		return stuckTicks;
	}

	public void resetStuck() {
		closestToOwner = Double.MAX_VALUE;
		stuckTicks = 0;
	}

	/** Never attacks players, armor stands or its owner's pets. */
	public static boolean validTarget(LivingEntity entity, PetCube pet) {
		return entity != null && entity.isAlive() && !(entity instanceof Player) && !(entity instanceof ArmorStand)
				&& !(entity instanceof PetCube other && other.ownerId() != null && other.ownerId().equals(pet.ownerId()))
				&& entity.level() == pet.level() && entity.distanceToSqr(pet) < 24 * 24;
	}

	// ---------------------------------------------------------------- riding (big pet)

	@Override
	public LivingEntity getControllingPassenger() {
		return this.getFirstPassenger() instanceof Player player ? player : super.getControllingPassenger();
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return this.getPassengers().isEmpty() && passenger instanceof Player && this.getSize() >= 4;
	}

	@Override
	protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
		return Vec3.ZERO;
	}

	@Override
	protected float getRiddenSpeed(Player player) {
		return 0.0F;
	}

	/** Steered by the rider: hops forward while moving, floats and slides on water (and lava as a magma cube). */
	@Override
	protected void tickRidden(Player player, Vec3 travelVector) {
		super.tickRidden(player, travelVector);
		this.setYRot(player.getYRot());
		this.yRotO = this.getYRot();
		this.yBodyRot = this.getYRot();
		this.yHeadRot = this.getYRot();
		double forward = player.zza;
		double strafe = player.xxa;
		double rad = this.getYRot() * (Math.PI / 180.0);
		double dx = strafe * Math.cos(rad) - forward * Math.sin(rad);
		double dz = forward * Math.cos(rad) + strafe * Math.sin(rad);
		double length = Math.sqrt(dx * dx + dz * dz);
		if (length > 1.0E-4) {
			dx /= length;
			dz /= length;
		}
		boolean moving = length > 1.0E-4;
		boolean inFluid = this.isInWater() || isMagma() && this.isInLava();
		Vec3 v = this.getDeltaMovement();
		if (inFluid) {
			double height = Math.max(this.getFluidHeight(FluidTags.WATER), isMagma() ? this.getFluidHeight(FluidTags.LAVA) : 0.0);
			double vy = height > 0.25 ? 0.08 : Math.max(v.y, 0.0);
			this.setDeltaMovement(moving ? dx * SWIM_SPEED : v.x * 0.5, vy, moving ? dz * SWIM_SPEED : v.z * 0.5);
			jumpPending = 0.0F;
			return;
		}
		if (this.onGround()) {
			if (moving || jumpPending > 0.0F) {
				this.jumpFromGround();
				double vy = this.getDeltaMovement().y + 0.35 * jumpPending;
				this.setDeltaMovement(moving ? dx * RIDE_SPEED : 0.0, vy, moving ? dz * RIDE_SPEED : 0.0);
				jumpPending = 0.0F;
			} else {
				this.setDeltaMovement(v.x * 0.5, v.y, v.z * 0.5);
			}
		} else if (moving) {
			this.setDeltaMovement(dx * RIDE_SPEED, v.y, dz * RIDE_SPEED);
		}
	}

	@Override
	public void onPlayerJump(int power) {
		jumpPending = power >= 90 ? 1.0F : Math.max(0.3F, power / 90.0F);
	}

	@Override
	public boolean canJump() {
		return this.getSize() >= 4;
	}

	@Override
	public void handleStartJump(int power) {
	}

	@Override
	public void handleStopJump() {
	}

	@Override
	public void openCustomInventoryScreen(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			PetManager.openMenu(this, serverPlayer);
		}
	}
}
