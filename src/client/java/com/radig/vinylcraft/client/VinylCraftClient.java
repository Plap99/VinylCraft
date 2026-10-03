package com.radig.vinylcraft.client;

import com.radig.vinylcraft.block.entity.ModBlockEntities;
import com.radig.vinylcraft.client.render.VinylPlayerBlockEntityRenderer;
import com.radig.vinylcraft.client.render.VinylPlayerTonearmModel;
import com.radig.vinylcraft.client.render.VinylPlayerButtonModel;
import com.radig.vinylcraft.client.sound.VinylPlayerSoundManager;
import com.radig.vinylcraft.sound.VinylPlayerAudioBridge;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class VinylCraftClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        ModelLayerRegistry.registerModelLayer(
                VinylPlayerBlockEntityRenderer.TONEARM_LAYER,
                VinylPlayerTonearmModel::createLayer
        );

        ModelLayerRegistry.registerModelLayer(
                VinylPlayerBlockEntityRenderer.BUTTON_LAYER,
                VinylPlayerButtonModel::createLayer
        );

        BlockEntityRenderers.register(
                ModBlockEntities.VINYL_PLAYER,
                VinylPlayerBlockEntityRenderer::new
        );

        /*
         * Conectamos el BlockEntity común
         * con el sistema de audio del cliente.
         */
        VinylPlayerAudioBridge.setClientTicker(
                VinylPlayerSoundManager::tickPlayer
        );
    }
}