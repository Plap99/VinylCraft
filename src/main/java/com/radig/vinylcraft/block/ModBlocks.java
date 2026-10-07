package com.radig.vinylcraft.block;

import com.radig.vinylcraft.VinylCraft;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.world.item.BlockItem;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {

    /*
     * =====================================================
     * VINYL PLAYERS
     * =====================================================
     */

    public static final Block OAK_VINYL_PLAYER =
            createVinylPlayer(ModBlockItemIds.OAK_VINYL_PLAYER);

    public static final Block SPRUCE_VINYL_PLAYER =
            createVinylPlayer(ModBlockItemIds.SPRUCE_VINYL_PLAYER);

    public static final Block BIRCH_VINYL_PLAYER =
            createVinylPlayer(ModBlockItemIds.BIRCH_VINYL_PLAYER);

    public static final Block JUNGLE_VINYL_PLAYER =
            createVinylPlayer(ModBlockItemIds.JUNGLE_VINYL_PLAYER);

    public static final Block ACACIA_VINYL_PLAYER =
            createVinylPlayer(ModBlockItemIds.ACACIA_VINYL_PLAYER);

    public static final Block DARK_OAK_VINYL_PLAYER =
            createVinylPlayer(ModBlockItemIds.DARK_OAK_VINYL_PLAYER);

    public static final Block MANGROVE_VINYL_PLAYER =
            createVinylPlayer(ModBlockItemIds.MANGROVE_VINYL_PLAYER);

    public static final Block CHERRY_VINYL_PLAYER =
            createVinylPlayer(ModBlockItemIds.CHERRY_VINYL_PLAYER);

    public static final Block PALE_OAK_VINYL_PLAYER =
            createVinylPlayer(ModBlockItemIds.PALE_OAK_VINYL_PLAYER);

    public static final Block BAMBOO_VINYL_PLAYER =
            createVinylPlayer(ModBlockItemIds.BAMBOO_VINYL_PLAYER);

    public static final Block CRIMSON_VINYL_PLAYER =
            createVinylPlayer(ModBlockItemIds.CRIMSON_VINYL_PLAYER);

    public static final Block WARPED_VINYL_PLAYER =
            createVinylPlayer(ModBlockItemIds.WARPED_VINYL_PLAYER);

    /*
     * =====================================================
     * VINYL RECORDERS
     * =====================================================
     */

    public static final Block OAK_VINYL_RECORDER =
            createVinylRecorder(ModBlockItemIds.OAK_VINYL_RECORDER);

    public static final Block SPRUCE_VINYL_RECORDER =
            createVinylRecorder(ModBlockItemIds.SPRUCE_VINYL_RECORDER);

    public static final Block BIRCH_VINYL_RECORDER =
            createVinylRecorder(ModBlockItemIds.BIRCH_VINYL_RECORDER);

    public static final Block JUNGLE_VINYL_RECORDER =
            createVinylRecorder(ModBlockItemIds.JUNGLE_VINYL_RECORDER);

    public static final Block ACACIA_VINYL_RECORDER =
            createVinylRecorder(ModBlockItemIds.ACACIA_VINYL_RECORDER);

    public static final Block DARK_OAK_VINYL_RECORDER =
            createVinylRecorder(ModBlockItemIds.DARK_OAK_VINYL_RECORDER);

    public static final Block MANGROVE_VINYL_RECORDER =
            createVinylRecorder(ModBlockItemIds.MANGROVE_VINYL_RECORDER);

    public static final Block CHERRY_VINYL_RECORDER =
            createVinylRecorder(ModBlockItemIds.CHERRY_VINYL_RECORDER);

    public static final Block PALE_OAK_VINYL_RECORDER =
            createVinylRecorder(ModBlockItemIds.PALE_OAK_VINYL_RECORDER);

    public static final Block BAMBOO_VINYL_RECORDER =
            createVinylRecorder(ModBlockItemIds.BAMBOO_VINYL_RECORDER);

    public static final Block CRIMSON_VINYL_RECORDER =
            createVinylRecorder(ModBlockItemIds.CRIMSON_VINYL_RECORDER);

    public static final Block WARPED_VINYL_RECORDER =
            createVinylRecorder(ModBlockItemIds.WARPED_VINYL_RECORDER);

    /** Bloque técnico sin item: cada pieza representa una zona del póster mural. */
    public static final Block ALBUM_FRAME =
            new AlbumFrameBlock(
                    BlockBehaviour.Properties.of()
                            .noCollision()
                            .noOcclusion()
                            .instabreak()
                            .noLootTable()
                            .setId(ModBlockItemIds.ALBUM_FRAME.block())
            );

    /*
     * =====================================================
     * CREACIÓN
     * =====================================================
     */

    private static Block createVinylPlayer(
            net.minecraft.references.BlockItemId id) {

        return new VinylPlayerBlock(
                BlockBehaviour.Properties.of()
                        .strength(1.2F, 3.0F)
                        .noOcclusion()
                        .setId(id.block())
        );
    }

    private static Block createVinylRecorder(
            net.minecraft.references.BlockItemId id) {

        return new VinylRecorderBlock(
                BlockBehaviour.Properties.of()
                        .strength(1.6F, 4.0F)
                        .noOcclusion()
                        .setId(id.block())
        );
    }

    /*
     * =====================================================
     * REGISTRO
     * =====================================================
     */

    public static void registerModBlocks() {

        VinylCraft.LOGGER.info(
                "Registrando bloques de VinylCraft..."
        );

        registerBlock(ModBlockItemIds.OAK_VINYL_PLAYER, OAK_VINYL_PLAYER);

        registerBlock(ModBlockItemIds.SPRUCE_VINYL_PLAYER, SPRUCE_VINYL_PLAYER);

        registerBlock(ModBlockItemIds.BIRCH_VINYL_PLAYER, BIRCH_VINYL_PLAYER);

        registerBlock(ModBlockItemIds.JUNGLE_VINYL_PLAYER, JUNGLE_VINYL_PLAYER);

        registerBlock(ModBlockItemIds.ACACIA_VINYL_PLAYER, ACACIA_VINYL_PLAYER);

        registerBlock(ModBlockItemIds.DARK_OAK_VINYL_PLAYER, DARK_OAK_VINYL_PLAYER);

        registerBlock(ModBlockItemIds.MANGROVE_VINYL_PLAYER, MANGROVE_VINYL_PLAYER);

        registerBlock(ModBlockItemIds.CHERRY_VINYL_PLAYER, CHERRY_VINYL_PLAYER);

        registerBlock(ModBlockItemIds.PALE_OAK_VINYL_PLAYER, PALE_OAK_VINYL_PLAYER);

        registerBlock(ModBlockItemIds.BAMBOO_VINYL_PLAYER, BAMBOO_VINYL_PLAYER);

        registerBlock(ModBlockItemIds.CRIMSON_VINYL_PLAYER, CRIMSON_VINYL_PLAYER);

        registerBlock(ModBlockItemIds.WARPED_VINYL_PLAYER, WARPED_VINYL_PLAYER);

        registerBlock(ModBlockItemIds.OAK_VINYL_RECORDER, OAK_VINYL_RECORDER);

        registerBlock(ModBlockItemIds.SPRUCE_VINYL_RECORDER, SPRUCE_VINYL_RECORDER);

        registerBlock(ModBlockItemIds.BIRCH_VINYL_RECORDER, BIRCH_VINYL_RECORDER);

        registerBlock(ModBlockItemIds.JUNGLE_VINYL_RECORDER, JUNGLE_VINYL_RECORDER);

        registerBlock(ModBlockItemIds.ACACIA_VINYL_RECORDER, ACACIA_VINYL_RECORDER);

        registerBlock(ModBlockItemIds.DARK_OAK_VINYL_RECORDER, DARK_OAK_VINYL_RECORDER);

        registerBlock(ModBlockItemIds.MANGROVE_VINYL_RECORDER, MANGROVE_VINYL_RECORDER);

        registerBlock(ModBlockItemIds.CHERRY_VINYL_RECORDER, CHERRY_VINYL_RECORDER);

        registerBlock(ModBlockItemIds.PALE_OAK_VINYL_RECORDER, PALE_OAK_VINYL_RECORDER);

        registerBlock(ModBlockItemIds.BAMBOO_VINYL_RECORDER, BAMBOO_VINYL_RECORDER);

        registerBlock(ModBlockItemIds.CRIMSON_VINYL_RECORDER, CRIMSON_VINYL_RECORDER);

        registerBlock(ModBlockItemIds.WARPED_VINYL_RECORDER, WARPED_VINYL_RECORDER);

        Registry.register(
                BuiltInRegistries.BLOCK,
                ModBlockItemIds.ALBUM_FRAME.block(),
                ALBUM_FRAME
        );

    }

    private static void registerBlock(
            net.minecraft.references.BlockItemId id,
            Block block) {

        Registry.register(
                BuiltInRegistries.BLOCK,
                id.block(),
                block
        );

        BlockItem blockItem =
                new BlockItem(
                        block,
                        new BlockItem.Properties()
                                .setId(id.item())
                );

        Registry.register(
                BuiltInRegistries.ITEM,
                id.item(),
                blockItem
        );
    }
}
