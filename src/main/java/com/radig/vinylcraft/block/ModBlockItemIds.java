package com.radig.vinylcraft.block;

import com.radig.vinylcraft.VinylCraft;

import net.minecraft.resources.Identifier;
import net.minecraft.references.BlockItemId;

public class ModBlockItemIds {

    public static final BlockItemId VINYL_PLAYER = create("vinyl_player");

    private static BlockItemId create(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(VinylCraft.MOD_ID, name);
        return BlockItemId.create(id, id);
    }
}