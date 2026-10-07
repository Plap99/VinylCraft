package com.radig.vinylcraft.block;

import com.mojang.serialization.MapCodec;
import com.radig.vinylcraft.block.entity.ModBlockEntities;
import com.radig.vinylcraft.block.entity.VinylRecorderBlockEntity;
import com.radig.vinylcraft.item.ModItems;
import com.radig.vinylcraft.recorder.VinylRecorderClientBridge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

public class VinylRecorderBlock
        extends HorizontalDirectionalBlock
        implements EntityBlock {

    public static final MapCodec<VinylRecorderBlock> CODEC =
            simpleCodec(VinylRecorderBlock::new);

    private static final VoxelShape SHAPE =
            Block.box(
                    1.0D, 0.0D, 1.0D,
                    15.0D, 14.2D, 15.0D
            );

    public VinylRecorderBlock(Properties properties) {
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
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VinylRecorderBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType) {

        if (blockEntityType != ModBlockEntities.VINYL_RECORDER) {
            return null;
        }

        return (BlockEntityTicker<T>)
                (BlockEntityTicker<VinylRecorderBlockEntity>)
                        VinylRecorderBlockEntity::tick;
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

        if (!(level.getBlockEntity(pos) instanceof VinylRecorderBlockEntity recorder)) {
            return InteractionResult.PASS;
        }

        LocalHit localHit = toModelCoordinates(state, pos, hit);

        /*
         * Inserción física del vinilo.
         *
         * Fuera de clonación acepta virgen o grabado.
         * Cuando el clonador espera destino:
         * - virgen  -> comienza automáticamente los 30 s de escritura.
         * - grabado -> entra, pero queda marcado como error por las luces.
         */
        if (
                !recorder.hasVinyl()
                        && stack.is(ModItems.BLANK_VINYL)
        ) {
            if (!level.isClientSide()) {
                if (recorder.insertVinyl(stack)) {
                    stack.consume(1, player);
                }
            }

            return InteractionResult.SUCCESS;
        }

        return handleRecorderInteraction(
                level,
                state,
                pos,
                player,
                recorder,
                localHit
        );
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {

        if (!(level.getBlockEntity(pos) instanceof VinylRecorderBlockEntity recorder)) {
            return InteractionResult.PASS;
        }

        return handleRecorderInteraction(
                level,
                state,
                pos,
                player,
                recorder,
                toModelCoordinates(state, pos, hit)
        );
    }

    private static InteractionResult handleRecorderInteraction(
            Level level,
            BlockState state,
            BlockPos pos,
            Player player,
            VinylRecorderBlockEntity recorder,
            LocalHit localHit) {

        if (!recorder.hasVinyl()) {
            return InteractionResult.SUCCESS;
        }

        /*
         * Mientras la aguja está leyendo/grabando, la máquina queda bloqueada.
         * Evita retirar el disco a mitad del proceso y elimina una vía de
         * duplicación o clonación incompleta.
         */
        if (recorder.isBusy()) {
            return InteractionResult.SUCCESS;
        }

        /*
         * INFO | REC | CLONAR
         *
         * Durante una clonación en espera los botones no cambian de modo:
         * el usuario debe retirar el original/disco incorrecto haciendo clic
         * en cualquier otra zona del grabador.
         */
        if (isInfoButton(localHit)) {
            if (
                    recorder.hasRecordedVinyl()
                            && (
                                !recorder.isCloneSessionActive()
                                || recorder.isCloneComplete()
                            )
            ) {
                openAlbumInfo(level, pos);
            }

            return InteractionResult.SUCCESS;
        }

        if (isRecordButton(localHit)) {
            if (
                    recorder.hasBlankVinyl()
                            && !recorder.isCloneSessionActive()
                            && !recorder.isCompletedLightOn()
            ) {
                openRecorderScreen(level, pos);
            }

            return InteractionResult.SUCCESS;
        }

        if (isCloneButton(localHit)) {
            if (
                    recorder.hasRecordedVinyl()
                            && !recorder.isCloneSessionActive()
                            && !recorder.isCompletedLightOn()
            ) {
                if (!level.isClientSide()) {
                    recorder.startCloneReading();
                }
            }

            return InteractionResult.SUCCESS;
        }

        /*
         * Cualquier otro clic retira el disco cuando la mecánica lo permite.
         * El inventario busca un hueco libre; si está lleno, el vinilo cae.
         *
         * Caso especial de clonación:
         * SOURCE_READY -> retirar original cambia a WAITING_BLANK.
         * WAITING_BLANK con disco grabado -> retirarlo conserva la espera.
         * COMPLETE -> retirar copia apaga verde y resetea la sesión.
         */
        return removeVinyl(level, pos, player, recorder);
    }

    private static boolean isInfoButton(LocalHit hit) {
        return hit.x >= 2.0D && hit.x <= 5.0D
                && hit.y >= 5.2D && hit.y <= 8.0D
                && hit.z >= 0.0D && hit.z <= 1.7D;
    }

    private static boolean isRecordButton(LocalHit hit) {
        return hit.x >= 6.5D && hit.x <= 9.5D
                && hit.y >= 5.2D && hit.y <= 8.0D
                && hit.z >= 0.0D && hit.z <= 1.7D;
    }

    private static boolean isCloneButton(LocalHit hit) {
        return hit.x >= 11.0D && hit.x <= 14.0D
                && hit.y >= 5.2D && hit.y <= 8.0D
                && hit.z >= 0.0D && hit.z <= 1.7D;
    }

    private static InteractionResult removeVinyl(
            Level level,
            BlockPos pos,
            Player player,
            VinylRecorderBlockEntity recorder) {

        if (recorder.isBusy()) {
            return InteractionResult.SUCCESS;
        }

        if (!level.isClientSide()) {
            ItemStack removed = recorder.removeVinyl();

            if (!removed.isEmpty() && !player.addItem(removed)) {
                Block.popResource(level, pos, removed);
            }
        }

        return InteractionResult.SUCCESS;
    }

    private static void openAlbumInfo(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            VinylRecorderClientBridge.openAlbumInfoScreen(pos);
        }
    }

    private static void openRecorderScreen(
            Level level,
            BlockPos pos) {

        if (level.isClientSide()) {
            VinylRecorderClientBridge.openRecorderScreen(pos);
        }
    }

    private static LocalHit toModelCoordinates(
            BlockState state,
            BlockPos pos,
            BlockHitResult hit) {

        double worldX =
                (hit.getLocation().x - pos.getX()) * 16.0D;
        double worldY =
                (hit.getLocation().y - pos.getY()) * 16.0D;
        double worldZ =
                (hit.getLocation().z - pos.getZ()) * 16.0D;

        Direction facing = state.getValue(FACING);

        return switch (facing) {
            case EAST -> new LocalHit(
                    worldZ,
                    worldY,
                    16.0D - worldX
            );

            case SOUTH -> new LocalHit(
                    16.0D - worldX,
                    worldY,
                    16.0D - worldZ
            );

            case WEST -> new LocalHit(
                    16.0D - worldZ,
                    worldY,
                    worldX
            );

            default -> new LocalHit(
                    worldX,
                    worldY,
                    worldZ
            );
        };
    }

    private record LocalHit(
            double x,
            double y,
            double z) {
    }
}
