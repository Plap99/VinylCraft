package com.radig.vinylcraft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.radig.vinylcraft.block.ModBlocks;
import com.radig.vinylcraft.block.entity.ModBlockEntities;
import com.radig.vinylcraft.item.ModItems;
import com.radig.vinylcraft.sound.ModSounds;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import com.radig.vinylcraft.component.ModDataComponents;
import com.radig.vinylcraft.network.VinylRecorderNetworking;
import com.radig.vinylcraft.network.DiscmanNetworking;

public class VinylCraft implements ModInitializer {

    public static final String MOD_ID = "vinylcraft";

    public static final Logger LOGGER =
            LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

        ModDataComponents.registerModDataComponents();
        
        ModItems.registerModItems();
        ModBlocks.registerModBlocks();
        ModBlockEntities.registerModBlockEntities();
        ModSounds.registerModSounds();
        VinylRecorderNetworking.register();
        DiscmanNetworking.register();

        LOGGER.info("VinylCraft loaded successfully!");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
