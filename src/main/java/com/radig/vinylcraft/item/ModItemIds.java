package com.radig.vinylcraft.item;

import com.radig.vinylcraft.VinylCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class ModItemIds {

    public static final ResourceKey<Item> BLANK_VINYL = create("blank_vinyl");

    private static ResourceKey<Item> create(String name) {
        return ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(VinylCraft.MOD_ID, name)
        );
    }
}