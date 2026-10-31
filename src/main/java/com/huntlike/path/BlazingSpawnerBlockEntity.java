package com.huntlike.path;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.huntlike.hunt.Messages;
import com.huntlike.hunt.QuestManager;

/**
 * The Blazing Spawner's rounds: 1 Imp, then 2, then 3. A round starts once the last one's Imps are all dead. After the
 * third, the spawner burns out into a Chalice and the player who lit it reaches the Path of the Wildfire's sixth goal.
 */
public class BlazingSpawnerBlockEntity extends BlockEntity {
	public static final int ROUNDS = 3;
	/** Ticks between a cleared round and the next one (and before the first). */
	public static final int ROUND_DELAY = 3 * 20;

	private UUID owner;
	private int round;
	private int delay = ROUND_DELAY;
	private final List<UUID> imps = new ArrayList<>();

	public BlazingSpawnerBlockEntity(BlockPos pos, BlockState state) {
		super(ModPaths.BLAZING_SPAWNER_ENTITY, pos, state);
	}

	public void setOwner(UUID owner) {
		this.owner = owner;
		setChanged();
	}

	public int round() {
		return round;
	}

	public List<UUID> imps() {
		return imps;
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, BlazingSpawnerBlockEntity spawner) {
		if (!(level instanceof ServerLevel server)) {
			return;
		}
		if (server.getGameTime() % 4 == 0) {
			server.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.01);
		}
		spawner.imps.removeIf(id -> {
			Entity imp = server.getEntity(id);
			return imp == null || !imp.isAlive();
		});
		if (!spawner.imps.isEmpty()) {
			return;
		}
		if (spawner.delay > 0) {
			spawner.delay--;
			return;
		}
		if (spawner.round >= ROUNDS) {
			spawner.finish(server, pos);
			return;
		}
		spawner.round++;
		spawner.delay = ROUND_DELAY;
		for (int i = 0; i < spawner.round; i++) {
			Imp imp = ModPaths.IMP.create(server, EntitySpawnReason.SPAWNER);
			if (imp == null) {
				continue;
			}
			double angle = Math.PI * 2 * i / spawner.round;
			imp.snapTo(pos.getX() + 0.5 + Math.cos(angle) * 1.5, pos.getY() + 1.0, pos.getZ() + 0.5 + Math.sin(angle) * 1.5,
					server.getRandom().nextFloat() * 360.0F, 0.0F);
			imp.setPersistenceRequired();
			server.addFreshEntity(imp);
			spawner.imps.add(imp.getUUID());
			server.sendParticles(ParticleTypes.LAVA, imp.getX(), imp.getY() + 0.5, imp.getZ(), 6, 0.2, 0.3, 0.2, 0.0);
		}
		server.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.2F, 0.6F);
		spawner.setChanged();
	}

	private void finish(ServerLevel level, BlockPos pos) {
		level.setBlockAndUpdate(pos, ModPaths.CHALICE.defaultBlockState());
		level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.02);
		level.sendParticles(ParticleTypes.ASH, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 60, 0.5, 0.5, 0.5, 0.02);
		level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.2F, 0.6F);
		ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
		if (player != null) {
			player.sendSystemMessage(Messages.c("The spawner burns out... and leaves a chalice behind.", WildfirePath.COLOR));
			WildfirePath.reach(player, QuestManager.data(player), 5);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		String id = input.getStringOr("owner", "");
		owner = id.isEmpty() ? null : UUID.fromString(id);
		round = input.getIntOr("round", 0);
		delay = input.getIntOr("delay", ROUND_DELAY);
		imps.clear();
		for (String imp : input.getStringOr("imps", "").split(",")) {
			if (!imp.isEmpty()) {
				imps.add(UUID.fromString(imp));
			}
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (owner != null) {
			output.putString("owner", owner.toString());
		}
		output.putInt("round", round);
		output.putInt("delay", delay);
		output.putString("imps", String.join(",", imps.stream().map(UUID::toString).toList()));
	}
}
