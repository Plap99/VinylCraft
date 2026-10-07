package com.radig.vinylcraft.block;

import com.mojang.serialization.MapCodec;
import com.radig.vinylcraft.block.entity.AlbumFrameBlockEntity;
import com.radig.vinylcraft.block.entity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

/**
 * Bloque invisible/ultrafino usado como soporte lógico para las portadas
 * colocadas en la pared. El render real lo hace AlbumFrameBlockEntityRenderer.
 */
public final class AlbumFrameBlock extends HorizontalDirectionalBlock implements EntityBlock {

    public static final MapCodec<AlbumFrameBlock> CODEC = simpleCodec(AlbumFrameBlock::new);

    private static final VoxelShape NORTH_SHAPE = Block.box(0, 0, 15, 16, 16, 16);
    private static final VoxelShape SOUTH_SHAPE = Block.box(0, 0, 0, 16, 16, 1);
    private static final VoxelShape EAST_SHAPE = Block.box(0, 0, 0, 1, 16, 16);
    private static final VoxelShape WEST_SHAPE = Block.box(15, 0, 0, 16, 16, 16);

    public AlbumFrameBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {

        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlbumFrameBlockEntity(pos, state);
    }

    /**
     * Coloca un mural cuadrado tomando target como el bloque inferior izquierdo
     * visto de frente. Crece hacia arriba y hacia la derecha del usuario.
     */
    public static boolean placeAlbum(
            Level level,
            BlockPos target,
            Direction facing,
            ItemStack sourceVinyl,
            int requestedSize,
            Player player) {

        if (level == null
                || target == null
                || facing == null
                || sourceVinyl == null
                || sourceVinyl.isEmpty()
                || !facing.getAxis().isHorizontal()) {
            return false;
        }

        String albumId = com.radig.vinylcraft.item.VinylData.getAlbumId(sourceVinyl);

        if (albumId == null || albumId.isBlank()) {
            return false;
        }

        int size = Math.max(1, Math.min(10, requestedSize));
        Direction right = facing.getCounterClockWise();
        Direction supportDirection = facing.getOpposite();

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BlockPos tilePos = target.above(y).relative(right, x);
                BlockPos supportPos = tilePos.relative(supportDirection);

                if (!level.getBlockState(tilePos).canBeReplaced()) {
                    return false;
                }

                if (!Block.canSupportCenter(level, supportPos, facing)) {
                    return false;
                }
            }
        }

        if (level.isClientSide()) {
            return true;
        }

        BlockState frameState = ModBlocks.ALBUM_FRAME
                .defaultBlockState()
                .setValue(FACING, facing);

        // Guardamos una copia exacta del vinilo montado. Sólo el bloque origen
        // conserva el ItemStack; las demás piezas apuntan a ese origen.
        ItemStack mountedVinyl = sourceVinyl.copyWithCount(1);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BlockPos tilePos = target.above(y).relative(right, x);
                level.setBlock(tilePos, frameState, Block.UPDATE_ALL);

                if (level.getBlockEntity(tilePos) instanceof AlbumFrameBlockEntity frame) {
                    frame.setAlbumData(
                            albumId,
                            size,
                            x,
                            y,
                            target,
                            x == 0 && y == 0 ? mountedVinyl : ItemStack.EMPTY
                    );
                }
            }
        }

        // El mural ya contiene físicamente el disco: sale de la mano.
        sourceVinyl.consume(1, player);

        return true;
    }
}
