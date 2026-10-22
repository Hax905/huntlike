package com.huntlike.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A Hunting Trophy: a golden plaque hung on a wall like an item frame. Keeps its owner's lore when broken. */
public class TrophyBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	private static final VoxelShape NORTH = box(0, 0, 15, 16, 16, 16);
	private static final VoxelShape SOUTH = box(0, 0, 0, 16, 16, 1);
	private static final VoxelShape EAST = box(0, 0, 0, 1, 16, 16);
	private static final VoxelShape WEST = box(15, 0, 0, 16, 16, 16);

	public TrophyBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	/** The plaque faces away from the wall it was placed on. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		Direction facing = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
		return defaultBlockState().setValue(FACING, facing);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case SOUTH -> SOUTH;
			case EAST -> EAST;
			case WEST -> WEST;
			default -> NORTH;
		};
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	/** The dropped trophy keeps its owner's lore. */
	@Override
	protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,
			net.minecraft.world.level.storage.loot.LootParams.Builder params) {
		java.util.List<net.minecraft.world.item.ItemStack> drops = super.getDrops(state, params);
		BlockEntity entity = params.getOptionalParameter(
				net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
		if (entity instanceof TrophyBlockEntity trophy && !trophy.lore().lines().isEmpty()) {
			for (net.minecraft.world.item.ItemStack drop : drops) {
				if (drop.is(asItem())) {
					drop.set(net.minecraft.core.component.DataComponents.LORE, trophy.lore());
				}
			}
		}
		return drops;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new TrophyBlockEntity(pos, state);
	}
}
