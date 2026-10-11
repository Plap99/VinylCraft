package com.radig.vinylcraft.client.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.radig.vinylcraft.block.entity.VinylPlayerBlockEntity;
import com.radig.vinylcraft.client.config.VinylHudConfig;
import com.radig.vinylcraft.client.sound.DiscmanSoundManager;
import com.radig.vinylcraft.client.sound.VinylPlayerSoundManager;
import com.radig.vinylcraft.item.DiscmanData;
import com.radig.vinylcraft.item.VinylData;
import com.radig.vinylcraft.music.AlbumData;
import com.radig.vinylcraft.music.ModAlbums;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/** Reúne las fuentes visibles del HUD y define su prioridad. */
public final class PlaybackHudManager {

    private PlaybackHudManager() {
    }

    public static List<PlaybackHudSource> collectSources(
            boolean discmanOnly) {

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null) {
            return List.of();
        }

        VinylHudConfig.SourceMode mode = VinylHudConfig.getSourceMode();
        List<PlaybackHudSource> sources = new ArrayList<>();

        if (mode != VinylHudConfig.SourceMode.PLAYERS_ONLY) {
            addDiscmanSource(minecraft, sources);
        }

        if (
                !discmanOnly
                        && mode != VinylHudConfig.SourceMode.DISCMAN_ONLY
        ) {
            addVinylPlayers(minecraft, sources);
        }

        sources.sort(
                Comparator
                        .comparingInt((PlaybackHudSource source) ->
                                source.isDiscman() ? 0 : 1)
                        .thenComparingDouble(PlaybackHudSource::distanceSquared)
        );

        return sources;
    }

    private static void addDiscmanSource(
            Minecraft minecraft,
            List<PlaybackHudSource> sources) {

        ItemStack discman = DiscmanData.findEquipped(minecraft.player);
        ItemStack vinyl = DiscmanData.findVinyl(minecraft.player);

        if (
                discman.isEmpty()
                        || vinyl.isEmpty()
                        || !VinylData.hasAlbum(vinyl)
        ) {
            return;
        }

        String albumId = VinylData.getAlbumId(vinyl);
        AlbumData album = albumId == null ? null : ModAlbums.get(albumId);

        if (album == null || album.isEmpty()) {
            return;
        }

        sources.add(
                new PlaybackHudSource(
                        PlaybackHudSource.SourceType.DISCMAN,
                        album,
                        DiscmanSoundManager.getLocalPlaybackTicks(),
                        -1.0D
                )
        );
    }

    private static void addVinylPlayers(
            Minecraft minecraft,
            List<PlaybackHudSource> sources) {

        for (VinylPlayerBlockEntity playerEntity :
                VinylPlayerSoundManager.getHudPlayers()) {

            if (playerEntity == null || playerEntity.isRemoved()) {
                continue;
            }

            String albumId = VinylData.getAlbumId(playerEntity.getVinyl());
            AlbumData album = albumId == null ? null : ModAlbums.get(albumId);

            if (album == null || album.isEmpty()) {
                continue;
            }

            var pos = playerEntity.getBlockPos();
            double dx = minecraft.player.getX() - (pos.getX() + 0.5D);
            double dy = minecraft.player.getY() - (pos.getY() + 0.5D);
            double dz = minecraft.player.getZ() - (pos.getZ() + 0.5D);
            double distanceSquared = dx * dx + dy * dy + dz * dz;

            sources.add(
                    new PlaybackHudSource(
                            PlaybackHudSource.SourceType.VINYL_PLAYER,
                            album,
                            playerEntity.getPlaybackTicks(),
                            distanceSquared
                    )
            );
        }
    }
}
