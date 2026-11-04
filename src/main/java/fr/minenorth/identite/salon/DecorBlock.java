package fr.minenorth.identite.salon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Meuble de salon (desserte, miroir) : décoratif, orienté vers le joueur qui le pose. */
public class DecorBlock extends HorizontalDirectionalBlock {
    /** Miroir : plaqué contre le mur derrière lui, quel que soit le sens. */
    private final boolean wall;

    public DecorBlock(boolean wall, Properties properties) {
        super(properties);
        this.wall = wall;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (!wall) return Block.box(1, 0, 2, 15, 12, 14);
        return switch (state.getValue(FACING)) {
            case SOUTH -> Block.box(0, 0, 0, 16, 16, 2);
            case WEST -> Block.box(14, 0, 0, 16, 16, 16);
            case EAST -> Block.box(0, 0, 0, 2, 16, 16);
            default -> Block.box(0, 0, 14, 16, 16, 16);
        };
    }
}
