package com.radig.vinylcraft.recorder;

import java.util.function.Consumer;
import java.util.function.BiConsumer;

import net.minecraft.core.BlockPos;

public final class VinylRecorderClientBridge {

    private static Consumer<BlockPos> openScreenHandler =
            pos -> { };

    private static Consumer<BlockPos> openInfoHandler = pos -> { };

    private static BiConsumer<String, String> cloneAlbumHandler =
            (sourceAlbumId, targetAlbumId) -> { };

    public static void setOpenAlbumInfoHandler(Consumer<BlockPos> handler) {
        openInfoHandler = handler == null ? pos -> { } : handler;
    }

    public static void openAlbumInfoScreen(BlockPos pos) {
        openInfoHandler.accept(pos);
    }

    public static void setCloneAlbumHandler(
            BiConsumer<String, String> handler) {

        cloneAlbumHandler = handler == null
                ? (sourceAlbumId, targetAlbumId) -> { }
                : handler;
    }

    public static void ensureIndependentCloneAlbum(
            String sourceAlbumId,
            String targetAlbumId) {

        if (sourceAlbumId == null
                || sourceAlbumId.isBlank()
                || targetAlbumId == null
                || targetAlbumId.isBlank()) {
            return;
        }

        cloneAlbumHandler.accept(sourceAlbumId, targetAlbumId);
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
