package com.radig.vinylcraft.block.entity;

import com.radig.vinylcraft.VinylCraft;
import com.radig.vinylcraft.block.ModBlocks;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.world.level.block.Block;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;


public class ModBlockEntities {

    /*
     * =====================================================
     * VINYL PLAYER
     * =====================================================
     *
     * Todas las variantes de madera comparten exactamente
     * el mismo BlockEntity.
     *
     * Por lo tanto:
     *
     * - inventario
     * - vinilo
     * - PLAY
     * - PAUSE
     * - STOP
     * - playbackTicks
     * - animación del brazo
     *
     * funcionan igual independientemente de la madera.
     * =====================================================
     */

    public static final BlockEntityType<VinylPlayerBlockEntity>
            VINYL_PLAYER =
            register(
                    "vinyl_player",
                    VinylPlayerBlockEntity::new,

                    ModBlocks.OAK_VINYL_PLAYER,
                    ModBlocks.SPRUCE_VINYL_PLAYER,
                    ModBlocks.BIRCH_VINYL_PLAYER,
                    ModBlocks.JUNGLE_VINYL_PLAYER,
                    ModBlocks.ACACIA_VINYL_PLAYER,
                    ModBlocks.DARK_OAK_VINYL_PLAYER,
                    ModBlocks.MANGROVE_VINYL_PLAYER,
                    ModBlocks.CHERRY_VINYL_PLAYER,
                    ModBlocks.PALE_OAK_VINYL_PLAYER,
                    ModBlocks.BAMBOO_VINYL_PLAYER,
                    ModBlocks.CRIMSON_VINYL_PLAYER,
                    ModBlocks.WARPED_VINYL_PLAYER
            );


    private static <T extends BlockEntity>
            BlockEntityType<T> register(
                    String name,
                    FabricBlockEntityTypeBuilder.Factory<? extends T> factory,
                    Block... blocks) {

        return Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                VinylCraft.id(name),
                FabricBlockEntityTypeBuilder
                        .<T>create(
                                factory,
                                blocks
                        )
                        .build()
        );
    }


    public static void registerModBlockEntities() {

        VinylCraft.LOGGER.info(
                "Registrando Block Entities de VinylCraft..."
        );
    }
}