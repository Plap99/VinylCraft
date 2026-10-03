package com.radig.vinylcraft.item;

import java.util.function.Function;

import com.radig.vinylcraft.VinylCraft;
import com.radig.vinylcraft.block.ModBlocks;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;


public class ModItems {

    public static Item register(
            ResourceKey<Item> itemKey,
            Function<Item.Properties, Item> itemFactory,
            Item.Properties properties) {

        Item item =
                itemFactory.apply(
                        properties.setId(itemKey)
                );

        Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                item
        );

        return item;
    }


    public static final Item BLANK_VINYL =
        register(
                ModItemIds.BLANK_VINYL,
                VinylItem::new,
                new Item.Properties()
        );


    public static void registerModItems() {

        VinylCraft.LOGGER.info(
                "Registering VinylCraft items..."
        );


        /*
         * =====================================================
         * INGREDIENTES
         * =====================================================
         */

        CreativeModeTabEvents
                .modifyOutputEvent(
                        CreativeModeTabs.INGREDIENTS
                )
                .register(
                        creativeTab ->
                                creativeTab.accept(
                                        BLANK_VINYL
                                )
                );


        /*
         * =====================================================
         * BLOQUES FUNCIONALES
         * =====================================================
         *
         * Aquí añadimos todas las variantes
         * del Vinyl Player al inventario creativo.
         * =====================================================
         */

        CreativeModeTabEvents
                .modifyOutputEvent(
                        CreativeModeTabs.FUNCTIONAL_BLOCKS
                )
                .register(creativeTab -> {

                    creativeTab.accept(
                            ModBlocks.OAK_VINYL_PLAYER
                    );

                    creativeTab.accept(
                            ModBlocks.SPRUCE_VINYL_PLAYER
                    );

                    creativeTab.accept(
                            ModBlocks.BIRCH_VINYL_PLAYER
                    );

                    creativeTab.accept(
                            ModBlocks.JUNGLE_VINYL_PLAYER
                    );

                    creativeTab.accept(
                            ModBlocks.ACACIA_VINYL_PLAYER
                    );

                    creativeTab.accept(
                            ModBlocks.DARK_OAK_VINYL_PLAYER
                    );

                    creativeTab.accept(
                            ModBlocks.MANGROVE_VINYL_PLAYER
                    );

                    creativeTab.accept(
                            ModBlocks.CHERRY_VINYL_PLAYER
                    );

                    creativeTab.accept(
                            ModBlocks.PALE_OAK_VINYL_PLAYER
                    );

                    creativeTab.accept(
                            ModBlocks.BAMBOO_VINYL_PLAYER
                    );

                    creativeTab.accept(
                            ModBlocks.CRIMSON_VINYL_PLAYER
                    );

                    creativeTab.accept(
                            ModBlocks.WARPED_VINYL_PLAYER
                    );
                });
    }
}