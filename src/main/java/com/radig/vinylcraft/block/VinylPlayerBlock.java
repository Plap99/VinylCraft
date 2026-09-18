package com.radig.vinylcraft.block;

import com.mojang.serialization.MapCodec;
import com.radig.vinylcraft.block.entity.VinylPlayerBlockEntity;
import com.radig.vinylcraft.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.radig.vinylcraft.block.entity.ModBlockEntities;

public class VinylPlayerBlock extends HorizontalDirectionalBlock implements EntityBlock {

    private static final VoxelShape SHAPE =
            Block.box(1, 0, 1, 15, 8, 15);

    public static final MapCodec<VinylPlayerBlock> CODEC =
            simpleCodec(VinylPlayerBlock::new);

    public VinylPlayerBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VinylPlayerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType) {

        if (blockEntityType != ModBlockEntities.VINYL_PLAYER) {
            return null;
        }

        return (BlockEntityTicker<T>)
                (BlockEntityTicker<VinylPlayerBlockEntity>)
                        VinylPlayerBlockEntity::tick;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection().getOpposite()
                );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {

        builder.add(FACING);
    }

   @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {

        if (!(level.getBlockEntity(pos)
                instanceof VinylPlayerBlockEntity playerEntity)) {

            return InteractionResult.PASS;
        }

        // Si ya hay un vinilo...
        if (playerEntity.hasVinyl()) {

            // Shift + clic derecho = retirar vinilo.
            if (player.isShiftKeyDown()) {

                if (!level.isClientSide()) {
                    ItemStack vinyl = playerEntity.removeVinyl();

                    if (!player.getInventory().add(vinyl)) {
                        player.drop(vinyl, false);
                    }
                }

                return InteractionResult.SUCCESS;
            }

            // Clic derecho normal = Play / Pause.
            if (!level.isClientSide()) {
                playerEntity.setPlaying(
                        !playerEntity.isPlaying()
                );
            }

            return InteractionResult.SUCCESS;
        }

        // Si está vacío, solamente acepta Blank Vinyl.
        if (!stack.is(ModItems.BLANK_VINYL)) {
            return InteractionResult.PASS;
        }

        // Insertamos un solo vinilo.
        if (!level.isClientSide()) {

            if (playerEntity.insertVinyl(stack)) {

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {

        if (!(level.getBlockEntity(pos)
                instanceof VinylPlayerBlockEntity playerEntity)) {

            return super.useWithoutItem(
                    state,
                    level,
                    pos,
                    player,
                    hit
            );
        }

        if (!playerEntity.hasVinyl()) {
            return super.useWithoutItem(
                    state,
                    level,
                    pos,
                    player,
                    hit
            );
        }

        // Shift + clic derecho = retirar vinilo.
        if (player.isShiftKeyDown()) {

            if (!level.isClientSide()) {
                ItemStack vinyl = playerEntity.removeVinyl();

                if (!player.getInventory().add(vinyl)) {
                    player.drop(vinyl, false);
                }
            }

            return InteractionResult.SUCCESS;
        }

        // Clic derecho normal = Play / Pause.
        if (!level.isClientSide()) {
            playerEntity.setPlaying(
                    !playerEntity.isPlaying()
            );
        }

        return InteractionResult.SUCCESS;
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