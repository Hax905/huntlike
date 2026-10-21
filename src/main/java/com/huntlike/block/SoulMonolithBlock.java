package com.huntlike.block;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Soul Monolith: every player inside its 17x17x17 cube (8 blocks out in every direction) has Resistance I;
 * several Monoliths don't stack. Creepers flee from it and can't explode inside its area.
 */
public class SoulMonolithBlock extends Block {
	public static final int RADIUS = 8;
	private static final int INTERVAL = 5;
	private static final int FLEE_RADIUS = 12;

	/** Monoliths known to be active, per dimension. */
	private static final Map<ResourceKey<Level>, Set<BlockPos>> ACTIVE = new HashMap<>();

	public SoulMonolithBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		level.scheduleTick(pos, this, INTERVAL);
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		// Restarts the effect loop for Monoliths placed before this version.
		return true;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
			level.scheduleTick(pos, this, INTERVAL);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		ACTIVE.computeIfAbsent(level.dimension(), key -> new HashSet<>()).add(pos.immutable());
		AABB area = area(pos);
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0, true, false, true));
		}
		Vec3 center = Vec3.atCenterOf(pos);
		for (Creeper creeper : level.getEntitiesOfClass(Creeper.class, area.inflate(FLEE_RADIUS - RADIUS))) {
			Vec3 away = creeper.position().subtract(center).multiply(1, 0, 1);
			if (away.lengthSqr() < 0.01) {
				away = new Vec3(1, 0, 0);
			}
			Vec3 target = center.add(away.normalize().scale(FLEE_RADIUS + 4));
			creeper.getNavigation().moveTo(target.x, creeper.getY(), target.z, 1.3);
			creeper.setSwellDir(-1);
		}
		level.scheduleTick(pos, this, INTERVAL);
	}

	private static AABB area(BlockPos pos) {
		return new AABB(pos.getX() - RADIUS, pos.getY() - RADIUS, pos.getZ() - RADIUS,
				pos.getX() + RADIUS + 1, pos.getY() + RADIUS + 1, pos.getZ() + RADIUS + 1);
	}

	/** Whether a position is inside an active Monolith's cube. */
	public static boolean protects(Level level, BlockPos at) {
		Set<BlockPos> monoliths = ACTIVE.get(level.dimension());
		if (monoliths == null || monoliths.isEmpty()) {
			return false;
		}
		monoliths.removeIf(pos -> level.isLoaded(pos) && !(level.getBlockState(pos).getBlock() instanceof SoulMonolithBlock));
		for (BlockPos pos : monoliths) {
			if (Math.abs(at.getX() - pos.getX()) <= RADIUS && Math.abs(at.getY() - pos.getY()) <= RADIUS
					&& Math.abs(at.getZ() - pos.getZ()) <= RADIUS) {
				return true;
			}
		}
		return false;
	}
}
