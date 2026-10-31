package com.huntlike.path;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/** What the Blazing Spawner leaves behind: a small chalice (6 x 8 pixels). Mined with any pickaxe. */
public class ChaliceBlock extends Block {
	private static final VoxelShape SHAPE = Shapes.or(Block.box(6, 0, 6, 10, 1, 10), Block.box(7, 1, 7, 9, 4, 9), Block.box(5, 4, 5, 11, 8, 11));

	public ChaliceBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
