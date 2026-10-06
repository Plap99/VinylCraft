package com.radig.vinylcraft.recorder;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;

public final class VinylRecorderClientBridge {

    private static Consumer<BlockPos> openScreenHandler =
            pos -> { };

    private VinylRecorderClientBridge() {
    }

    public static void setOpenScreenHandler(
            Consumer<BlockPos> handler) {

        openScreenHandler =
                handler == null
                        ? pos -> { }
                        : handler;
    }

    public static void openRecorderScreen(BlockPos pos) {
        openScreenHandler.accept(pos);
    }
}
