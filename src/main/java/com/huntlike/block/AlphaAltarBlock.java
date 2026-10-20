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

/** The Alpha Altar: right-click to open the Hunt menu. */
public class AlphaAltarBlock extends Block {
	public AlphaAltarBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer) {
			QuestManager.openMenu(serverPlayer);
		}
		return InteractionResult.SUCCESS;
	}
}
