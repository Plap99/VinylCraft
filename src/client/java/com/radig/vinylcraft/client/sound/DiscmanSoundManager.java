package com.radig.vinylcraft.client.sound;

import java.nio.file.Path;

import com.radig.vinylcraft.client.config.VinylHudConfig;
import com.radig.vinylcraft.item.DiscmanData;
import com.radig.vinylcraft.item.VinylData;
import com.radig.vinylcraft.music.AlbumData;
import com.radig.vinylcraft.music.ModAlbums;
import com.radig.vinylcraft.music.TrackData;
import com.radig.vinylcraft.network.DiscmanActionPayload;
import com.radig.vinylcraft.player.DiscmanEquipment;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/** Audio privado del Discman. Sólo existe en el cliente del jugador que lo lleva. */
public final class DiscmanSoundManager {

    private static HeadphoneTrackPlayer activePlayer;
    private static String activeTrackKey;
    private static String activeAlbumId;
    private static int activeTrackIndex = -1;
    private static int startDelay;

    private static long localPlaybackTicks;
    private static long lastServerTicks = -1L;
    private static String observedAlbumId;
    private static boolean initialized;
    private static int syncCounter;

    private DiscmanSoundManager() {
    }

    public static void tick(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            reset();
            return;
        }

        ItemStack discman = DiscmanData.findEquipped(client.player);
        ItemStack vinyl = DiscmanData.findVinyl(client.player);

        if (
                discman.isEmpty()
                        || vinyl.isEmpty()
                        || !VinylData.hasAlbum(vinyl)
        ) {
            reset();
            return;
        }

        String albumId = VinylData.getAlbumId(vinyl);
        long serverTicks = DiscmanData.getPlaybackTicks(discman);
        int state = DiscmanData.getState(discman);

        if (
                !initialized
                        || albumId == null
                        || !albumId.equals(observedAlbumId)
        ) {
            observedAlbumId = albumId;
            localPlaybackTicks = serverTicks;
            lastServerTicks = serverTicks;
            initialized = true;
            stopAudio();
        } else {
            /*
             * Mientras reproduce, localPlaybackTicks es el reloj continuo del
             * cliente. El servidor recibe SYNC_TICKS cada segundo, pero esa
             * sincronización no se retransmite de vuelta al cliente.
             *
             * Antes comparábamos SIEMPRE el valor local contra el último valor
             * visible del servidor. Como ese valor podía quedarse en 0, al
             * llegar a ~31 ticks se interpretaba falsamente como un salto y la
             * canción volvía al inicio una y otra vez.
             *
             * Ahora sólo consideramos una corrección del servidor cuando el
             * valor recibido realmente CAMBIA (por ejemplo STOP, anterior,
             * siguiente o una reanudación sincronizada).
             */
            if (serverTicks != lastServerTicks) {
                boolean jumped =
                        serverTicks + 5L < localPlaybackTicks
                                || Math.abs(serverTicks - localPlaybackTicks) > 30L;

                if (jumped) {
                    localPlaybackTicks = serverTicks;
                    stopAudio();
                }

                lastServerTicks = serverTicks;
            }
        }

        if (state == DiscmanData.STOPPED) {
            localPlaybackTicks = serverTicks;
            stopAudio();
            return;
        }

        if (state == DiscmanData.PLAYING) {
            localPlaybackTicks++;
            syncCounter++;

            if (syncCounter >= 20) {
                syncCounter = 0;
                syncTicks();
            }
        }

        long totalTicks = DiscmanData.getTotalTicks(vinyl);

        if (totalTicks > 0L && localPlaybackTicks >= totalTicks) {
            localPlaybackTicks = 0L;
            sendAction(DiscmanActionPayload.STOP);
            stopAudio();
            return;
        }

        AlbumData album = albumId == null ? null : ModAlbums.get(albumId);

        if (album == null || album.isEmpty()) {
            stopAudio();
            return;
        }

        int trackIndex = resolveTrackIndex(album, localPlaybackTicks);
        TrackData track = album.getTrack(trackIndex);

        if (track == null) {
            stopAudio();
            return;
        }

        long trackStart = resolveTrackStartTick(album, trackIndex);
        long offsetTicks = Math.max(0L, localPlaybackTicks - trackStart);
        /*
         * Troll retro intencional: el Discman puede seguir reproduciendo y
         * avanzando el tiempo sin audífonos, pero no emite audio. Al colocar
         * audífonos arranca en el offset actual.
         */
        if (!DiscmanEquipment.hasHeadphones(client.player)) {
            stopAudio();
            return;
        }

        Path localPath = toLocalPath(track.audioFile());

        if (localPath == null || !HeadphoneAudioStreamFactory.supports(localPath)) {
            stopAudio();
            return;
        }

        tickTrack(
                client,
                albumId,
                trackIndex,
                localPath,
                offsetTicks,
                state == DiscmanData.PAUSED
        );
    }

    private static void tickTrack(
            Minecraft client,
            String albumId,
            int trackIndex,
            Path path,
            long offsetTicks,
            boolean paused) {

        String key = path.toAbsolutePath().normalize().toString();

        if (startDelay > 0) {
            startDelay--;
            return;
        }

        boolean changed =
                activePlayer != null
                        && (
                            !key.equals(activeTrackKey)
                                    || !albumId.equals(activeAlbumId)
                                    || trackIndex != activeTrackIndex
                        );

        if (changed || (activePlayer != null && activePlayer.hasStartupFailed())) {
            stopAudio();
            startDelay = 1;
            return;
        }

        if (activePlayer == null) {
            activePlayer = new HeadphoneTrackPlayer(
                    path,
                    offsetTicks / 20.0D
            );
            activeTrackKey = key;
            activeAlbumId = albumId;
            activeTrackIndex = trackIndex;
            activePlayer.start(paused);
        }

        float volume =
                0.65F
                        * (DiscmanData.getVolumePercent(discmanForVolume(client)) / 100.0F)
                        * VinylHudConfig.getMusicVolume()
                        * client.options.getFinalSoundSourceVolume(SoundSource.RECORDS);

        activePlayer.setVolume(volume);
        activePlayer.setPaused(paused);
    }

    private static ItemStack discmanForVolume(Minecraft client) {
        if (client == null || client.player == null) {
            return ItemStack.EMPTY;
        }

        return DiscmanData.findEquipped(client.player);
    }

    public static void sendVolume(int percent) {
        ClientPlayNetworking.send(
                new DiscmanActionPayload(
                        DiscmanActionPayload.SET_VOLUME,
                        Math.max(0, Math.min(100, percent))
                )
        );
    }

    public static long getLocalPlaybackTicks() {
        return Math.max(0L, localPlaybackTicks);
    }

    public static void sendAction(int action) {
        if (
                action == DiscmanActionPayload.PLAY_PAUSE
                        || action == DiscmanActionPayload.STOP
                        || action == DiscmanActionPayload.PREVIOUS
                        || action == DiscmanActionPayload.NEXT
                        || action == DiscmanActionPayload.EJECT
        ) {
            syncTicks();
        }

        ClientPlayNetworking.send(
                new DiscmanActionPayload(action, 0L)
        );
    }

    private static void syncTicks() {
        if (!initialized) {
            return;
        }

        ClientPlayNetworking.send(
                new DiscmanActionPayload(
                        DiscmanActionPayload.SYNC_TICKS,
                        Math.max(0L, localPlaybackTicks)
                )
        );
    }

    private static int resolveTrackIndex(AlbumData album, long ticks) {
        long accumulated = 0L;

        for (int index = 0; index < album.trackCount(); index++) {
            TrackData track = album.getTrack(index);

            if (track == null) {
                continue;
            }

            long duration = Math.max(1L, track.durationTicks());

            if (ticks < accumulated + duration) {
                return index;
            }

            accumulated += duration;
        }

        return Math.max(0, album.trackCount() - 1);
    }

    private static long resolveTrackStartTick(AlbumData album, int trackIndex) {
        long accumulated = 0L;

        for (int index = 0; index < trackIndex; index++) {
            TrackData track = album.getTrack(index);

            if (track != null) {
                accumulated += Math.max(1L, track.durationTicks());
            }
        }

        return accumulated;
    }

    private static Path toLocalPath(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            Path path = Path.of(value);
            return path.isAbsolute() ? path : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void stopAudio() {
        if (activePlayer != null) {
            activePlayer.stop();
        }

        activePlayer = null;
        activeTrackKey = null;
        activeAlbumId = null;
        activeTrackIndex = -1;
    }

    private static void reset() {
        stopAudio();
        initialized = false;
        observedAlbumId = null;
        localPlaybackTicks = 0L;
        lastServerTicks = -1L;
        syncCounter = 0;
        startDelay = 0;
    }
}
