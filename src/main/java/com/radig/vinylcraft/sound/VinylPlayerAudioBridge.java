package com.radig.vinylcraft.sound;

import java.util.function.Consumer;

import com.radig.vinylcraft.block.entity.VinylPlayerBlockEntity;

public final class VinylPlayerAudioBridge {

    /*
     * Por defecto no hace nada.
     *
     * El cliente registrará aquí el controlador
     * real de audio cuando inicie Minecraft.
     */
    private static Consumer<VinylPlayerBlockEntity> clientTicker =
            blockEntity -> {
            };

    private VinylPlayerAudioBridge() {
    }

    public static void setClientTicker(
            Consumer<VinylPlayerBlockEntity> ticker) {

        clientTicker = ticker;
    }

    public static void tickClient(
            VinylPlayerBlockEntity blockEntity) {

        clientTicker.accept(blockEntity);
    }
}