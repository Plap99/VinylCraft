package com.radig.vinylcraft.client;

import com.radig.vinylcraft.block.entity.ModBlockEntities;
import com.radig.vinylcraft.client.render.VinylPlayerBlockEntityRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public class VinylCraftClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        BlockEntityRenderers.register(
                ModBlockEntities.VINYL_PLAYER,
                VinylPlayerBlockEntityRenderer::new
        );
    }
}