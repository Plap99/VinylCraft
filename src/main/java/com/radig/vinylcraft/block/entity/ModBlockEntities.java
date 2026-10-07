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

    /*
     * =====================================================
     * VINYL RECORDER
     * =====================================================
     */

    public static final BlockEntityType<VinylRecorderBlockEntity>
            VINYL_RECORDER =
            register(
                    "vinyl_recorder",
                    VinylRecorderBlockEntity::new,

                    ModBlocks.OAK_VINYL_RECORDER,
                    ModBlocks.SPRUCE_VINYL_RECORDER,
                    ModBlocks.BIRCH_VINYL_RECORDER,
                    ModBlocks.JUNGLE_VINYL_RECORDER,
                    ModBlocks.ACACIA_VINYL_RECORDER,
                    ModBlocks.DARK_OAK_VINYL_RECORDER,
                    ModBlocks.MANGROVE_VINYL_RECORDER,
                    ModBlocks.CHERRY_VINYL_RECORDER,
                    ModBlocks.PALE_OAK_VINYL_RECORDER,
                    ModBlocks.BAMBOO_VINYL_RECORDER,
                    ModBlocks.CRIMSON_VINYL_RECORDER,
                    ModBlocks.WARPED_VINYL_RECORDER
            );

    /*
     * =====================================================
     * ALBUM FRAME / POSTER
     * =====================================================
     */

    public static final BlockEntityType<AlbumFrameBlockEntity>
            ALBUM_FRAME =
            register(
                    "album_frame",
                    AlbumFrameBlockEntity::new,
                    ModBlocks.ALBUM_FRAME
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
