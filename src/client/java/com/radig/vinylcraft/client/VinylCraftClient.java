package com.radig.vinylcraft.client;

import com.radig.vinylcraft.block.entity.ModBlockEntities;
import com.radig.vinylcraft.client.render.VinylPlayerBlockEntityRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import com.radig.vinylcraft.client.render.VinylPlayerTonearmModel;

public class VinylCraftClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        ModelLayerRegistry.registerModelLayer(
                VinylPlayerBlockEntityRenderer.TONEARM_LAYER,
                VinylPlayerTonearmModel::createLayer
        );

        BlockEntityRenderers.register(
                ModBlockEntities.VINYL_PLAYER,
                VinylPlayerBlockEntityRenderer::new
        );
    }
}