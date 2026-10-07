package com.radig.vinylcraft.network;

import com.radig.vinylcraft.block.entity.VinylRecorderBlockEntity;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class VinylRecorderNetworking {

    private VinylRecorderNetworking() {
    }

    public static void register() {

        PayloadTypeRegistry
                .serverboundPlay()
                .register(
                        StartVinylRecordingPayload.TYPE,
                        StartVinylRecordingPayload.CODEC
                );

        PayloadTypeRegistry
                .serverboundPlay()
                .register(
                        SetAlbumWallSizePayload.TYPE,
                        SetAlbumWallSizePayload.CODEC
                );

        ServerPlayNetworking.registerGlobalReceiver(
                StartVinylRecordingPayload.TYPE,
                (payload, context) -> {

                    var player = context.player();
                    var pos = payload.pos();

                    if (
                            player.distanceToSqr(
                                    pos.getX() + 0.5D,
                                    pos.getY() + 0.5D,
                                    pos.getZ() + 0.5D
                            ) > 64.0D
                    ) {
                        return;
                    }

                    if (!(
                            player.level().getBlockEntity(pos)
                                    instanceof VinylRecorderBlockEntity recorder
                    )) {
                        return;
                    }

                    recorder.startRecording(
                            payload.albumId(),
                            payload.trackDurations()
                    );
                }
        );

        ServerPlayNetworking.registerGlobalReceiver(
                SetAlbumWallSizePayload.TYPE,
                (payload, context) -> {

                    var player = context.player();
                    var pos = payload.pos();

                    if (player.distanceToSqr(
                            pos.getX() + 0.5D,
                            pos.getY() + 0.5D,
                            pos.getZ() + 0.5D
                    ) > 64.0D) {
                        return;
                    }

                    if (!(player.level().getBlockEntity(pos)
                            instanceof VinylRecorderBlockEntity recorder)) {
                        return;
                    }

                    if (!recorder.hasRecordedVinyl()) {
                        return;
                    }

                    com.radig.vinylcraft.item.VinylData.setWallSize(
                            recorder.getVinyl(),
                            payload.size()
                    );
                    recorder.setChanged();
                }
        );
    }
}
