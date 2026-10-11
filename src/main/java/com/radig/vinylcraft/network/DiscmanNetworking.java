package com.radig.vinylcraft.network;

import com.radig.vinylcraft.item.DiscmanData;
import com.radig.vinylcraft.item.VinylData;
import com.radig.vinylcraft.player.DiscmanEquipmentHolder;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Acciones de reproducción del Discman equipado en los slots reales. */
public final class DiscmanNetworking {

    private DiscmanNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry
                .serverboundPlay()
                .register(
                        DiscmanActionPayload.TYPE,
                        DiscmanActionPayload.CODEC
                );

        ServerPlayNetworking.registerGlobalReceiver(
                DiscmanActionPayload.TYPE,
                (payload, context) -> handle(context.player(), payload)
        );
    }

    private static void handle(
            ServerPlayer player,
            DiscmanActionPayload payload) {

        ItemStack discman = DiscmanData.findEquipped(player);
        ItemStack vinyl = DiscmanData.findVinyl(player);

        if (discman.isEmpty()) {
            return;
        }

        boolean validVinyl =
                !vinyl.isEmpty()
                        && VinylData.hasAlbum(vinyl);

        switch (payload.action()) {
            case DiscmanActionPayload.PLAY_PAUSE -> {
                if (validVinyl) {
                    togglePlayPause(discman, vinyl);
                }
            }
            case DiscmanActionPayload.STOP -> {
                DiscmanData.setState(discman, DiscmanData.STOPPED);
                DiscmanData.setPlaybackTicks(discman, 0L);
            }
            case DiscmanActionPayload.PREVIOUS -> {
                if (validVinyl) {
                    DiscmanData.previousTrack(discman, vinyl);
                }
            }
            case DiscmanActionPayload.NEXT -> {
                if (validVinyl) {
                    DiscmanData.nextTrack(discman, vinyl);
                }
            }
            case DiscmanActionPayload.SYNC_TICKS -> {
                if (validVinyl) {
                    long total = DiscmanData.getTotalTicks(vinyl);
                    long safe = Math.max(0L, payload.value());

                    if (total > 0L) {
                        safe = Math.min(safe, total);
                    }

                    DiscmanData.setPlaybackTicks(discman, safe);
                }
            }
            case DiscmanActionPayload.SET_VOLUME -> {
                DiscmanData.setVolumePercent(
                        discman,
                        (int) Math.max(0L, Math.min(100L, payload.value()))
                );
            }
            /* INSERT/EJECT son legado del Paso 21: los slots reales los sustituyen. */
            default -> {
            }
        }

        player.getInventory().setChanged();
        ((DiscmanEquipmentHolder) player)
                .vinylcraft$getDiscmanEquipment()
                .setChanged();

        if (payload.action() != DiscmanActionPayload.SYNC_TICKS) {
            player.inventoryMenu.broadcastFullState();

            if (player.containerMenu != player.inventoryMenu) {
                player.containerMenu.broadcastFullState();
            }
        }
    }

    private static void togglePlayPause(
            ItemStack discman,
            ItemStack vinyl) {

        int state = DiscmanData.getState(discman);

        if (state == DiscmanData.PLAYING) {
            DiscmanData.setState(discman, DiscmanData.PAUSED);
            return;
        }

        long total = DiscmanData.getTotalTicks(vinyl);

        if (total > 0L && DiscmanData.getPlaybackTicks(discman) >= total) {
            DiscmanData.setPlaybackTicks(discman, 0L);
        }

        DiscmanData.setState(discman, DiscmanData.PLAYING);
    }
}
