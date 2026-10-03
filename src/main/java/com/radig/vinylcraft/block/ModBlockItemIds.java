package com.radig.vinylcraft.block;

import com.radig.vinylcraft.VinylCraft;

import net.minecraft.resources.Identifier;
import net.minecraft.references.BlockItemId;

public class ModBlockItemIds {

    public static final BlockItemId OAK_VINYL_PLAYER =
            create("oak_vinyl_player");

    public static final BlockItemId SPRUCE_VINYL_PLAYER =
            create("spruce_vinyl_player");

    public static final BlockItemId BIRCH_VINYL_PLAYER =
            create("birch_vinyl_player");

    public static final BlockItemId JUNGLE_VINYL_PLAYER =
            create("jungle_vinyl_player");

    public static final BlockItemId ACACIA_VINYL_PLAYER =
            create("acacia_vinyl_player");

    public static final BlockItemId DARK_OAK_VINYL_PLAYER =
            create("dark_oak_vinyl_player");

    public static final BlockItemId MANGROVE_VINYL_PLAYER =
            create("mangrove_vinyl_player");

    public static final BlockItemId CHERRY_VINYL_PLAYER =
            create("cherry_vinyl_player");

    public static final BlockItemId PALE_OAK_VINYL_PLAYER =
            create("pale_oak_vinyl_player");

    public static final BlockItemId BAMBOO_VINYL_PLAYER =
            create("bamboo_vinyl_player");

    public static final BlockItemId CRIMSON_VINYL_PLAYER =
            create("crimson_vinyl_player");

    public static final BlockItemId WARPED_VINYL_PLAYER =
            create("warped_vinyl_player");


    private static BlockItemId create(String name) {

        Identifier id =
                Identifier.fromNamespaceAndPath(
                        VinylCraft.MOD_ID,
                        name
                );

        return BlockItemId.create(
                id,
                id
        );
    }
}