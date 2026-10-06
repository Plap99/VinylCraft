package com.radig.vinylcraft.block;

import com.mojang.serialization.MapCodec;
import com.radig.vinylcraft.block.entity.ModBlockEntities;
import com.radig.vinylcraft.block.entity.VinylPlayerBlockEntity;
import com.radig.vinylcraft.item.ModItems;

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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

public class VinylPlayerBlock
        extends HorizontalDirectionalBlock
        implements EntityBlock {

    public static final BooleanProperty OPEN =
            BooleanProperty.create("open");

    public static final MapCodec<VinylPlayerBlock> CODEC =
            simpleCodec(VinylPlayerBlock::new);

    /*
     * =====================================================
     * FORMAS DEL REPRODUCTOR
     * =====================================================
     *
     * El modelo base está construido mirando al NORTH.
     * Las demás formas se generan rotando esas mismas cajas
     * alrededor del centro del bloque, igual que el blockstate.
     */

    private static final VoxelShape CLOSED_NORTH =
            createClosedShape(Direction.NORTH);
    private static final VoxelShape CLOSED_EAST =
            createClosedShape(Direction.EAST);
    private static final VoxelShape CLOSED_SOUTH =
            createClosedShape(Direction.SOUTH);
    private static final VoxelShape CLOSED_WEST =
            createClosedShape(Direction.WEST);

    private static final VoxelShape OPEN_NORTH =
            createOpenShape(Direction.NORTH);
    private static final VoxelShape OPEN_EAST =
            createOpenShape(Direction.EAST);
    private static final VoxelShape OPEN_SOUTH =
            createOpenShape(Direction.SOUTH);
    private static final VoxelShape OPEN_WEST =
            createOpenShape(Direction.WEST);

    public VinylPlayerBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(OPEN, true)
        );
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state) {

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
    public BlockState getStateForPlacement(
            BlockPlaceContext context) {

        return defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection().getOpposite()
                );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {

        builder.add(FACING, OPEN);
    }

    /*
     * =====================================================
     * INTERACCIÓN
     * =====================================================
     *
     * Ya no usamos Shift ni UseBlockCallback.
     * Tanto la mano vacía como la mano con un objeto pasan
     * por el mismo manejador y se decide la acción según la
     * parte física del tocadiscos que recibió el clic.
     */

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {

        return handleInteraction(
                stack,
                state,
                level,
                pos,
                player,
                hit
        );
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {

        return handleInteraction(
                ItemStack.EMPTY,
                state,
                level,
                pos,
                player,
                hit
        );
    }

    private InteractionResult handleInteraction(
            ItemStack heldStack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {

        if (!(
                level.getBlockEntity(pos)
                instanceof VinylPlayerBlockEntity playerEntity
        )) {
            return InteractionResult.PASS;
        }

        LocalHit localHit = toModelCoordinates(
                state,
                pos,
                hit
        );

        boolean open = state.getValue(OPEN);

        /*
         * BOTONES DE TRANSPORTE
         *
         * Los botones están en el frente del mueble, fuera de la tapa.
         * Por eso funcionan tanto con la tapa abierta como cerrada.
         *
         * El disco, en cambio, sólo se puede insertar o retirar
         * cuando la tapa está abierta.
         */
        if (isPreviousTrackButton(localHit)) {
            if (!level.isClientSide() && playerEntity.hasVinyl()) {
                playerEntity.previousTrack();
            }

            return InteractionResult.SUCCESS;
        }

        if (isStopButton(localHit)) {
            if (!level.isClientSide() && playerEntity.hasVinyl()) {
                playerEntity.stopPlayback();
            }

            return InteractionResult.SUCCESS;
        }

        if (isPlayPauseButton(localHit)) {
            if (!level.isClientSide() && playerEntity.hasVinyl()) {
                playerEntity.setPlaying(
                        !playerEntity.isPlaying()
                );
            }

            return InteractionResult.SUCCESS;
        }

        if (isNextTrackButton(localHit)) {
            if (!level.isClientSide() && playerEntity.hasVinyl()) {
                playerEntity.nextTrack();
            }

            return InteractionResult.SUCCESS;
        }

        /*
         * TAPA CERRADA
         *
         * Los controles frontales ya se atendieron arriba.
         * Cualquier otro clic únicamente puede abrir la tapa.
         */
        if (!open) {
            if (isClosedLid(localHit)) {
                if (!level.isClientSide()) {
                    level.setBlock(
                            pos,
                            state.setValue(OPEN, true),
                            Block.UPDATE_ALL
                    );
                }
            }

            return InteractionResult.SUCCESS;
        }

        /*
         * PLATO / DISCO
         *
         * - Vacío + Blank Vinyl en mano: insertar.
         * - Disco colocado y detenido: retirar.
         * - Reproduciendo o pausado: no se puede retirar.
         */
        if (isDiscArea(localHit)) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            if (!playerEntity.hasVinyl()) {
                if (heldStack.is(ModItems.BLANK_VINYL)) {
                    if (playerEntity.insertVinyl(heldStack)) {
                        if (!player.getAbilities().instabuild) {
                            heldStack.shrink(1);
                        }
                    }
                }

                return InteractionResult.SUCCESS;
            }

            if (!playerEntity.isStopped()) {
                return InteractionResult.SUCCESS;
            }

            ItemStack vinyl = playerEntity.removeVinyl();

            if (!vinyl.isEmpty()) {
                if (!player.getInventory().add(vinyl)) {
                    player.drop(vinyl, false);
                }
            }

            return InteractionResult.SUCCESS;
        }

        /*
         * TAPA ABIERTA
         *
         * La comprobación de la tapa va DESPUÉS del plato. El ItemStack
         * del vinilo sobresale unas décimas por encima del modelo base y,
         * si comprobamos primero la tapa, un clic sobre el disco puede
         * terminar interpretándose como clic en la tapa y nunca retirarlo.
         */
        if (isOpenLid(localHit)) {
            if (!level.isClientSide()) {
                level.setBlock(
                        pos,
                        state.setValue(OPEN, false),
                        Block.UPDATE_ALL
                );
            }

            return InteractionResult.SUCCESS;
        }

        /*
         * Clic sobre madera u otra zona sin control.
         */
        return InteractionResult.SUCCESS;
    }

    /*
     * =====================================================
     * ZONAS DE CLIC
     * =====================================================
     *
     * Las coordenadas ya están normalizadas a la orientación
     * NORTH del JSON del modelo (escala 0..16), así que estas
     * zonas funcionan igual aunque el bloque mire a otro lado.
     */

    private static boolean isClosedLid(LocalHit hit) {
        return hit.y >= 5.35D
                && hit.x >= 1.4D
                && hit.x <= 14.6D
                && hit.z >= 1.4D
                && hit.z <= 14.1D;
    }

    private static boolean isOpenLid(LocalHit hit) {
        /*
         * La base seleccionable termina en Y=6. Los escalones
         * que representan la tapa abierta empiezan por encima.
         */
        return hit.y > 6.15D;
    }

    private static boolean isDiscArea(LocalHit hit) {
        /*
         * Un poco más amplia que la geometría del plato para incluir
         * el ItemStack 3D del vinilo. Esto hace que retirar el disco sea
         * consistente aunque el rayo golpee la etiqueta o el borde del item.
         */
        return hit.x >= 1.70D
                && hit.x <= 11.60D
                && hit.z >= 1.70D
                && hit.z <= 11.60D
                && hit.y >= 4.55D
                && hit.y <= 6.90D;
    }

    private static boolean isPreviousTrackButton(LocalHit hit) {
        return hit.x >= 11.30D
                && hit.x <= 13.95D
                && hit.z >= 0.00D
                && hit.z <= 1.55D
                && hit.y >= 1.25D
                && hit.y <= 3.15D;
    }

    private static boolean isStopButton(LocalHit hit) {
        return hit.x >= 8.05D
                && hit.x <= 10.70D
                && hit.z >= 0.00D
                && hit.z <= 1.55D
                && hit.y >= 1.25D
                && hit.y <= 3.15D;
    }

    private static boolean isPlayPauseButton(LocalHit hit) {
        return hit.x >= 4.80D
                && hit.x <= 7.45D
                && hit.z >= 0.00D
                && hit.z <= 1.55D
                && hit.y >= 1.25D
                && hit.y <= 3.15D;
    }

    private static boolean isNextTrackButton(LocalHit hit) {
        return hit.x >= 1.55D
                && hit.x <= 4.20D
                && hit.z >= 0.00D
                && hit.z <= 1.55D
                && hit.y >= 1.25D
                && hit.y <= 3.15D;
    }

    /*
     * Convierte el impacto del mundo a las coordenadas del
     * modelo NORTH original. Esto evita duplicar las zonas de
     * interacción para NORTH/EAST/SOUTH/WEST.
     */
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

    /*
     * =====================================================
     * VOXEL SHAPES
     * =====================================================
     */

    private static VoxelShape createClosedShape(
            Direction facing) {

        return Shapes.or(
                rotatedBox(
                        facing,
                        1.0D, 0.0D, 0.75D,
                        15.0D, 6.0D, 15.0D
                ),
                rotatedBox(
                        facing,
                        1.5D, 5.35D, 1.45D,
                        14.5D, 8.9D, 14.1D
                )
        );
    }

    private static VoxelShape createOpenShape(
            Direction facing) {

        return Shapes.or(
                /* Base */
                rotatedBox(
                        facing,
                        1.0D, 0.0D, 0.75D,
                        15.0D, 6.0D, 15.0D
                ),

                /*
                 * Tapa inclinada aproximada en escalones.
                 * Los valores siguen la inclinación real de 22.5°
                 * del modelo abierto.
                 */
                rotatedBox(
                        facing,
                        1.5D, 11.8D, 3.0D,
                        14.5D, 13.5D, 5.5D
                ),
                rotatedBox(
                        facing,
                        1.5D, 10.8D, 5.5D,
                        14.5D, 12.6D, 8.0D
                ),
                rotatedBox(
                        facing,
                        1.5D, 9.8D, 8.0D,
                        14.5D, 11.6D, 10.5D
                ),
                rotatedBox(
                        facing,
                        1.5D, 8.8D, 10.5D,
                        14.5D, 10.7D, 13.0D
                ),
                rotatedBox(
                        facing,
                        1.5D, 7.7D, 13.0D,
                        14.5D, 9.8D, 15.35D
                )
        );
    }

    private static VoxelShape rotatedBox(
            Direction facing,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ) {

        return switch (facing) {
            case EAST -> Block.box(
                    16.0D - maxZ,
                    minY,
                    minX,
                    16.0D - minZ,
                    maxY,
                    maxX
            );

            case SOUTH -> Block.box(
                    16.0D - maxX,
                    minY,
                    16.0D - maxZ,
                    16.0D - minX,
                    maxY,
                    16.0D - minZ
            );

            case WEST -> Block.box(
                    minZ,
                    minY,
                    16.0D - maxX,
                    maxZ,
                    maxY,
                    16.0D - minX
            );

            default -> Block.box(
                    minX,
                    minY,
                    minZ,
                    maxX,
                    maxY,
                    maxZ
            );
        };
    }

    private static VoxelShape shapeFor(
            boolean open,
            Direction facing) {

        if (open) {
            return switch (facing) {
                case EAST -> OPEN_EAST;
                case SOUTH -> OPEN_SOUTH;
                case WEST -> OPEN_WEST;
                default -> OPEN_NORTH;
            };
        }

        return switch (facing) {
            case EAST -> CLOSED_EAST;
            case SOUTH -> CLOSED_SOUTH;
            case WEST -> CLOSED_WEST;
            default -> CLOSED_NORTH;
        };
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {

        return shapeFor(
                state.getValue(OPEN),
                state.getValue(FACING)
        );
    }
}
