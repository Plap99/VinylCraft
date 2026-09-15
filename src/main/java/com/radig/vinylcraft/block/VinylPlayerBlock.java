package com.radig.vinylcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VinylPlayerBlock extends Block {

    private static final VoxelShape SHAPE =
            Block.box(1, 0, 1, 15, 8, 15);

    public VinylPlayerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {

        return SHAPE;
    }
}