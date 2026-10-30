package com.huntlike.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.huntlike.hunt.QuestManager;

/** The Soul Nexus: right-click to open your soul's triangle (Health, Defense, Damage). */
public class SoulNexusBlock extends Block {
	public SoulNexusBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer) {
			QuestManager.openNexus(serverPlayer);
		}
		return InteractionResult.SUCCESS;
	}
}
