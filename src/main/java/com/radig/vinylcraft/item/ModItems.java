package com.radig.vinylcraft.item;

import java.util.function.Function;

import com.radig.vinylcraft.VinylCraft;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;

public class ModItems {

    public static Item register(
            ResourceKey<Item> itemKey,
            Function<Item.Properties, Item> itemFactory,
            Item.Properties properties) {

        Item item = itemFactory.apply(properties.setId(itemKey));

        Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                item
        );

        return item;
    }

    public static final Item BLANK_VINYL = register(
            ModItemIds.BLANK_VINYL,
            Item::new,
            new Item.Properties()
    );

    public static void registerModItems() {
        VinylCraft.LOGGER.info("Registering VinylCraft items...");

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
                .register(creativeTab -> creativeTab.accept(BLANK_VINYL));
    }
}