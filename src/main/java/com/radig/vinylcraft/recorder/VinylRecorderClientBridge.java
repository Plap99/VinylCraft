package com.radig.vinylcraft.recorder;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;

public final class VinylRecorderClientBridge {

    private static Consumer<BlockPos> openScreenHandler =
            pos -> { };

    private static Consumer<BlockPos> openInfoHandler = pos -> { };

    public static void setOpenAlbumInfoHandler(Consumer<BlockPos> handler) {
        openInfoHandler = handler == null ? pos -> { } : handler;
    }

    public static void openAlbumInfoScreen(BlockPos pos) {
        openInfoHandler.accept(pos);
    }

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
