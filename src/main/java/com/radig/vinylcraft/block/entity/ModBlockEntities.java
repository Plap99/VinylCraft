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

    public static final BlockEntityType<VinylPlayerBlockEntity> VINYL_PLAYER =
            register(
                    "vinyl_player",
                    VinylPlayerBlockEntity::new,
                    ModBlocks.VINYL_PLAYER
            );

    private static <T extends BlockEntity> BlockEntityType<T> register(
            String name,
            FabricBlockEntityTypeBuilder.Factory<? extends T> factory,
            Block... blocks) {

        return Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                VinylCraft.id(name),
                FabricBlockEntityTypeBuilder.<T>create(factory, blocks).build()
        );
    }

    public static void registerModBlockEntities() {
        VinylCraft.LOGGER.info("Registrando Block Entities de VinylCraft...");
    }
}