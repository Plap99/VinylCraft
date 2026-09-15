package com.radig.vinylcraft.block;

import com.radig.vinylcraft.VinylCraft;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.BlockItem;

public class ModBlocks {

    public static final Block VINYL_PLAYER = new VinylPlayerBlock(
            BlockBehaviour.Properties.of()
                    .strength(1.2F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .setId(ModBlockItemIds.VINYL_PLAYER.block()));

    public static final BlockItem VINYL_PLAYER_ITEM = new BlockItem(
            VINYL_PLAYER,
            new BlockItem.Properties()
                    .setId(ModBlockItemIds.VINYL_PLAYER.item()));

    public static void registerModBlocks() {
        VinylCraft.LOGGER.info("Registrando bloques de VinylCraft...");

        Registry.register(
                BuiltInRegistries.BLOCK,
                ModBlockItemIds.VINYL_PLAYER.block(),
                VINYL_PLAYER
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                ModBlockItemIds.VINYL_PLAYER.item(),
                VINYL_PLAYER_ITEM);
    }
}