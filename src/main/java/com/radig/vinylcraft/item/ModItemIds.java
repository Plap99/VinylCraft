package com.radig.vinylcraft.item;

import com.radig.vinylcraft.VinylCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class ModItemIds {

    public static final ResourceKey<Item> BLANK_VINYL = create("blank_vinyl");

    public static final ResourceKey<Item> DISCMAN = create("discman");

    public static final ResourceKey<Item> WHITE_HEADPHONES = create("white_headphones");
    public static final ResourceKey<Item> ORANGE_HEADPHONES = create("orange_headphones");
    public static final ResourceKey<Item> MAGENTA_HEADPHONES = create("magenta_headphones");
    public static final ResourceKey<Item> LIGHT_BLUE_HEADPHONES = create("light_blue_headphones");
    public static final ResourceKey<Item> YELLOW_HEADPHONES = create("yellow_headphones");
    public static final ResourceKey<Item> LIME_HEADPHONES = create("lime_headphones");
    public static final ResourceKey<Item> PINK_HEADPHONES = create("pink_headphones");
    public static final ResourceKey<Item> GRAY_HEADPHONES = create("gray_headphones");
    public static final ResourceKey<Item> LIGHT_GRAY_HEADPHONES = create("light_gray_headphones");
    public static final ResourceKey<Item> CYAN_HEADPHONES = create("cyan_headphones");
    public static final ResourceKey<Item> PURPLE_HEADPHONES = create("purple_headphones");
    public static final ResourceKey<Item> BLUE_HEADPHONES = create("blue_headphones");
    public static final ResourceKey<Item> BROWN_HEADPHONES = create("brown_headphones");
    public static final ResourceKey<Item> GREEN_HEADPHONES = create("green_headphones");
    public static final ResourceKey<Item> RED_HEADPHONES = create("red_headphones");
    public static final ResourceKey<Item> BLACK_HEADPHONES = create("black_headphones");

    private static ResourceKey<Item> create(String name) {
        return ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(VinylCraft.MOD_ID, name)
        );
    }
}