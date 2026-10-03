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
     *
     * Todos utilizan VinylPlayerBlock.
     *
     * La diferencia entre ellos será únicamente
     * el tipo de madera / acabado visual.
     * =====================================================
     */

    public static final Block OAK_VINYL_PLAYER =
            createVinylPlayer(
                    ModBlockItemIds.OAK_VINYL_PLAYER
            );

    public static final Block SPRUCE_VINYL_PLAYER =
            createVinylPlayer(
                    ModBlockItemIds.SPRUCE_VINYL_PLAYER
            );

    public static final Block BIRCH_VINYL_PLAYER =
            createVinylPlayer(
                    ModBlockItemIds.BIRCH_VINYL_PLAYER
            );

    public static final Block JUNGLE_VINYL_PLAYER =
            createVinylPlayer(
                    ModBlockItemIds.JUNGLE_VINYL_PLAYER
            );

    public static final Block ACACIA_VINYL_PLAYER =
            createVinylPlayer(
                    ModBlockItemIds.ACACIA_VINYL_PLAYER
            );

    public static final Block DARK_OAK_VINYL_PLAYER =
            createVinylPlayer(
                    ModBlockItemIds.DARK_OAK_VINYL_PLAYER
            );

    public static final Block MANGROVE_VINYL_PLAYER =
            createVinylPlayer(
                    ModBlockItemIds.MANGROVE_VINYL_PLAYER
            );

    public static final Block CHERRY_VINYL_PLAYER =
            createVinylPlayer(
                    ModBlockItemIds.CHERRY_VINYL_PLAYER
            );

    public static final Block PALE_OAK_VINYL_PLAYER =
            createVinylPlayer(
                    ModBlockItemIds.PALE_OAK_VINYL_PLAYER
            );

    public static final Block BAMBOO_VINYL_PLAYER =
            createVinylPlayer(
                    ModBlockItemIds.BAMBOO_VINYL_PLAYER
            );

    public static final Block CRIMSON_VINYL_PLAYER =
            createVinylPlayer(
                    ModBlockItemIds.CRIMSON_VINYL_PLAYER
            );

    public static final Block WARPED_VINYL_PLAYER =
            createVinylPlayer(
                    ModBlockItemIds.WARPED_VINYL_PLAYER
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
                        .strength(
                                1.2F,
                                3.0F
                        )
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

        registerVinylPlayer(
                ModBlockItemIds.OAK_VINYL_PLAYER,
                OAK_VINYL_PLAYER
        );

        registerVinylPlayer(
                ModBlockItemIds.SPRUCE_VINYL_PLAYER,
                SPRUCE_VINYL_PLAYER
        );

        registerVinylPlayer(
                ModBlockItemIds.BIRCH_VINYL_PLAYER,
                BIRCH_VINYL_PLAYER
        );

        registerVinylPlayer(
                ModBlockItemIds.JUNGLE_VINYL_PLAYER,
                JUNGLE_VINYL_PLAYER
        );

        registerVinylPlayer(
                ModBlockItemIds.ACACIA_VINYL_PLAYER,
                ACACIA_VINYL_PLAYER
        );

        registerVinylPlayer(
                ModBlockItemIds.DARK_OAK_VINYL_PLAYER,
                DARK_OAK_VINYL_PLAYER
        );

        registerVinylPlayer(
                ModBlockItemIds.MANGROVE_VINYL_PLAYER,
                MANGROVE_VINYL_PLAYER
        );

        registerVinylPlayer(
                ModBlockItemIds.CHERRY_VINYL_PLAYER,
                CHERRY_VINYL_PLAYER
        );

        registerVinylPlayer(
                ModBlockItemIds.PALE_OAK_VINYL_PLAYER,
                PALE_OAK_VINYL_PLAYER
        );

        registerVinylPlayer(
                ModBlockItemIds.BAMBOO_VINYL_PLAYER,
                BAMBOO_VINYL_PLAYER
        );

        registerVinylPlayer(
                ModBlockItemIds.CRIMSON_VINYL_PLAYER,
                CRIMSON_VINYL_PLAYER
        );

        registerVinylPlayer(
                ModBlockItemIds.WARPED_VINYL_PLAYER,
                WARPED_VINYL_PLAYER
        );
    }


    private static void registerVinylPlayer(
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