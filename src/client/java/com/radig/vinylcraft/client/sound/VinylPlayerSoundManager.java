package com.radig.vinylcraft.client.sound;

import java.nio.file.Path;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.radig.vinylcraft.block.entity.VinylPlayerBlockEntity;
import com.radig.vinylcraft.client.config.VinylHudConfig;
import com.radig.vinylcraft.client.mixin.SoundEngineAccessor;
import com.radig.vinylcraft.client.mixin.SoundManagerAccessor;
import com.radig.vinylcraft.item.VinylData;
import com.radig.vinylcraft.music.AlbumData;
import com.radig.vinylcraft.music.ModAlbums;
import com.radig.vinylcraft.music.TrackData;
import com.radig.vinylcraft.music.TrackSoundResolver;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;

public final class VinylPlayerSoundManager {

    private static final Map<
            VinylPlayerBlockEntity,
            VinylPlayerSoundInstance
    > ACTIVE_SOUNDS =
            new IdentityHashMap<>();

    private static final Map<
            VinylPlayerBlockEntity,
            String
    > ACTIVE_SOUND_TRACKS =
            new IdentityHashMap<>();

    private static final Set<
            VinylPlayerBlockEntity
    > PAUSED_PLAYERS =
            Collections.newSetFromMap(
                    new IdentityHashMap<>()
            );

    private static final Map<
            VinylPlayerBlockEntity,
            PositionalLocalTrackPlayer
    > LOCAL_PLAYERS =
            new IdentityHashMap<>();

    private static final Map<
            VinylPlayerBlockEntity,
            String
    > LOCAL_TRACKS =
            new IdentityHashMap<>();

    /*
     * Al cambiar de pista dejamos un tick para que OpenAL libere el
     * canal streaming anterior antes de pedir el siguiente. Sin esta
     * pequeña espera, la primera transición tras entrar al mundo podía
     * quedarse sin canal y producir silencio hasta cambiar de pista.
     */
    private static final Map<
            VinylPlayerBlockEntity,
            Integer
    > LOCAL_START_DELAY =
            new IdentityHashMap<>();

    private static final Map<
            VinylPlayerBlockEntity,
            Long
    > LAST_PLAYBACK_TICKS =
            new IdentityHashMap<>();

    private VinylPlayerSoundManager() {
    }

    public static void tickPlayer(
            VinylPlayerBlockEntity playerEntity) {

        if (
                playerEntity.isRemoved()
                        || !playerEntity.hasVinyl()
                        || playerEntity.isStopped()
        ) {
            stopMinecraftSound(playerEntity);
            stopLocalSound(playerEntity);
            LOCAL_START_DELAY.remove(playerEntity);
            LAST_PLAYBACK_TICKS.remove(playerEntity);
            return;
        }

        long playbackTicks = playerEntity.getPlaybackTicks();
        Long previousPlaybackTicks = LAST_PLAYBACK_TICKS.put(
                playerEntity,
                playbackTicks
        );

        boolean transportJumpedBack =
                previousPlaybackTicks != null
                        && playbackTicks + 5L < previousPlaybackTicks;

        AlbumData album = getAlbum(playerEntity);

        if (album == null || album.isEmpty()) {
            stopMinecraftSound(playerEntity);
            stopLocalSound(playerEntity);
            return;
        }

        int trackIndex = resolveTrackIndex(
                album,
                playerEntity.getPlaybackTicks()
        );

        TrackData track = album.getTrack(trackIndex);

        if (track == null) {
            return;
        }

        long trackStartTicks = resolveTrackStartTick(
                album,
                trackIndex
        );

        long offsetInTrackTicks = Math.max(
                0L,
                playbackTicks - trackStartTicks
        );

        Path localPath = toLocalPath(track.audioFile());

        if (localPath != null && LocalAudioStreamFactory.supports(localPath)) {
            stopMinecraftSound(playerEntity);
            tickLocalTrack(
                    playerEntity,
                    track,
                    localPath,
                    transportJumpedBack,
                    offsetInTrackTicks
            );
            return;
        }

        stopLocalSound(playerEntity);
        tickMinecraftTrack(
                playerEntity,
                track,
                transportJumpedBack
        );
    }

    private static void tickLocalTrack(
            VinylPlayerBlockEntity playerEntity,
            TrackData track,
            Path localPath,
            boolean forceRestart,
            long offsetInTrackTicks) {

        String key = localPath.toAbsolutePath().normalize().toString();

        Integer delay = LOCAL_START_DELAY.get(playerEntity);

        if (delay != null) {
            if (delay <= 1) {
                LOCAL_START_DELAY.remove(playerEntity);
            } else {
                LOCAL_START_DELAY.put(playerEntity, delay - 1);
            }

            return;
        }

        PositionalLocalTrackPlayer current =
                LOCAL_PLAYERS.get(playerEntity);

        String currentKey =
                LOCAL_TRACKS.get(playerEntity);

        boolean changedTrack =
                current != null
                        && currentKey != null
                        && !key.equals(currentKey);

        boolean channelFailedToStart =
                current != null
                        && current.hasStartupFailed();

        /*
         * Si ya había un canal, primero lo soltamos y esperamos un tick.
         * Esto evita la carrera del pool STREAMING de OpenAL que podía
         * dejar muda la pista 2 en la primera reproducción del mundo.
         * Si la creación del nuevo canal falla por esa misma carrera,
         * reintentamos también después de liberar el intento fallido.
         */
        if (
                (forceRestart && current != null)
                        || changedTrack
                        || channelFailedToStart
        ) {
            stopLocalSound(playerEntity);
            LOCAL_START_DELAY.put(playerEntity, 1);
            return;
        }

        if (current == null) {
            var pos = playerEntity.getBlockPos();

            current = new PositionalLocalTrackPlayer(
                    localPath,
                    new net.minecraft.world.phys.Vec3(
                            pos.getX() + 0.5D,
                            pos.getY() + 0.5D,
                            pos.getZ() + 0.5D
                    ),
                    offsetInTrackTicks / 20.0D
            );

            current.setAttenuationDistance(
                    VinylHudConfig.getSoundDistance()
            );

            LOCAL_PLAYERS.put(playerEntity, current);
            LOCAL_TRACKS.put(playerEntity, key);
            current.start(playerEntity.isPaused());
        }

        float volume =
                0.65F
                        * VinylHudConfig.getMusicVolume()
                        * Minecraft.getInstance()
                                .options
                                .getFinalSoundSourceVolume(
                                        net.minecraft.sounds.SoundSource.RECORDS
                                );

        current.setAttenuationDistance(
                VinylHudConfig.getSoundDistance()
        );
        current.setVolume(volume);
        current.setPaused(playerEntity.isPaused());
    }

    private static void tickMinecraftTrack(
            VinylPlayerBlockEntity playerEntity,
            TrackData track,
            boolean forceRestart) {

        SoundManager soundManager =
                Minecraft.getInstance()
                        .getSoundManager();

        VinylPlayerSoundInstance currentSound =
                ACTIVE_SOUNDS.get(playerEntity);

        String currentTrack =
                ACTIVE_SOUND_TRACKS.get(playerEntity);

        if (currentSound != null) {
            currentSound.setConfiguredVolume(
                    VinylHudConfig.getMusicVolume()
            );
        }

        if (
                currentSound != null
                        && (
                            forceRestart
                                    || !track.id().equals(currentTrack)
                        )
        ) {
            soundManager.stop(currentSound);
            ACTIVE_SOUNDS.remove(playerEntity);
            ACTIVE_SOUND_TRACKS.remove(playerEntity);
            PAUSED_PLAYERS.remove(playerEntity);
            currentSound = null;
        }

        if (playerEntity.isPaused()) {
            if (
                    currentSound != null
                            && !currentSound.isStopped()
            ) {
                currentSound.setPausedVolume(true);

                if (pauseSound(currentSound)) {
                    PAUSED_PLAYERS.add(playerEntity);
                }
            }

            return;
        }

        if (
                currentSound == null
                        || currentSound.isStopped()
        ) {
            SoundEvent soundEvent =
                    TrackSoundResolver.resolve(track);

            if (soundEvent == null) {
                return;
            }

            VinylPlayerSoundInstance newSound =
                    new VinylPlayerSoundInstance(
                            playerEntity,
                            soundEvent
                    );

            newSound.setConfiguredVolume(
                    VinylHudConfig.getMusicVolume()
            );
            newSound.setPausedVolume(false);

            ACTIVE_SOUNDS.put(playerEntity, newSound);
            ACTIVE_SOUND_TRACKS.put(playerEntity, track.id());
            PAUSED_PLAYERS.remove(playerEntity);
            soundManager.play(newSound);
            return;
        }

        if (PAUSED_PLAYERS.contains(playerEntity)) {
            currentSound.setPausedVolume(false);

            if (resumeSound(currentSound)) {
                PAUSED_PLAYERS.remove(playerEntity);
            }
        }
    }

    private static AlbumData getAlbum(
            VinylPlayerBlockEntity playerEntity) {

        String albumId =
                VinylData.getAlbumId(
                        playerEntity.getVinyl()
                );

        if (albumId == null) {
            return null;
        }

        return ModAlbums.get(albumId);
    }

    private static int resolveTrackIndex(
            AlbumData album,
            long playbackTicks) {

        long accumulated = 0L;

        for (int index = 0; index < album.trackCount(); index++) {
            TrackData track = album.getTrack(index);

            if (track == null) {
                continue;
            }

            long duration = Math.max(1L, track.durationTicks());

            if (playbackTicks < accumulated + duration) {
                return index;
            }

            accumulated += duration;
        }

        return Math.max(0, album.trackCount() - 1);
    }


    private static long resolveTrackStartTick(
            AlbumData album,
            int trackIndex) {

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

    private static void stopMinecraftSound(
            VinylPlayerBlockEntity playerEntity) {

        SoundManager soundManager =
                Minecraft.getInstance()
                        .getSoundManager();

        VinylPlayerSoundInstance sound =
                ACTIVE_SOUNDS.remove(playerEntity);

        if (sound != null) {
            soundManager.stop(sound);
        }

        ACTIVE_SOUND_TRACKS.remove(playerEntity);
        PAUSED_PLAYERS.remove(playerEntity);
    }

    private static void stopLocalSound(
            VinylPlayerBlockEntity playerEntity) {

        PositionalLocalTrackPlayer player =
                LOCAL_PLAYERS.remove(playerEntity);

        if (player != null) {
            player.stop();
        }

        LOCAL_TRACKS.remove(playerEntity);
        LOCAL_START_DELAY.remove(playerEntity);
    }

    private static boolean pauseSound(
            SoundInstance sound) {

        ChannelAccess.ChannelHandle channel =
                getChannel(sound);

        if (
                channel == null
                        || channel.isStopped()
        ) {
            return false;
        }

        channel.execute(
                audioChannel -> {
                    audioChannel.setVolume(0.0F);
                    audioChannel.pause();
                }
        );

        return true;
    }

    private static boolean resumeSound(
            SoundInstance sound) {

        ChannelAccess.ChannelHandle channel =
                getChannel(sound);

        if (
                channel == null
                        || channel.isStopped()
        ) {
            return false;
        }

        float restoredVolume =
                sound.getVolume()
                        * Minecraft.getInstance()
                                .options
                                .getFinalSoundSourceVolume(
                                        sound.getSource()
                                );

        restoredVolume =
                Math.max(
                        0.0F,
                        Math.min(1.0F, restoredVolume)
                );

        final float volume = restoredVolume;

        channel.execute(
                audioChannel -> {
                    audioChannel.setVolume(volume);
                    audioChannel.unpause();
                }
        );

        return true;
    }

    private static ChannelAccess.ChannelHandle getChannel(
            SoundInstance sound) {

        SoundManager soundManager =
                Minecraft.getInstance()
                        .getSoundManager();

        SoundEngine soundEngine =
                ((SoundManagerAccessor) soundManager)
                        .vinylcraft$getSoundEngine();

        return ((SoundEngineAccessor) soundEngine)
                .vinylcraft$getInstanceToChannel()
                .get(sound);
    }

    /**
     * Devuelve el tocadiscos activo más cercano al jugador para el HUD.
     * Incluye reproducción normal y PAUSE; STOP desaparece del HUD.
     */
    public static VinylPlayerBlockEntity getHudPlayer() {
        Minecraft minecraft = Minecraft.getInstance();

        VinylPlayerBlockEntity best = null;
        double bestDistance = Double.MAX_VALUE;

        for (VinylPlayerBlockEntity entity : LOCAL_PLAYERS.keySet()) {
            double distance = hudDistanceSquared(minecraft, entity);

            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }

        for (VinylPlayerBlockEntity entity : ACTIVE_SOUNDS.keySet()) {
            double distance = hudDistanceSquared(minecraft, entity);

            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }

        if (best == null) {
            return null;
        }

        if (
                best.isRemoved()
                        || !best.hasVinyl()
                        || best.isStopped()
        ) {
            return null;
        }

        return best;
    }

    private static double hudDistanceSquared(
            Minecraft minecraft,
            VinylPlayerBlockEntity entity) {

        if (
                entity == null
                        || entity.isRemoved()
                        || !entity.hasVinyl()
                        || entity.isStopped()
        ) {
            return Double.MAX_VALUE;
        }

        if (minecraft.player == null) {
            return 0.0D;
        }

        var pos = entity.getBlockPos();

        double dx = minecraft.player.getX() - (pos.getX() + 0.5D);
        double dy = minecraft.player.getY() - (pos.getY() + 0.5D);
        double dz = minecraft.player.getZ() - (pos.getZ() + 0.5D);

        return dx * dx + dy * dy + dz * dz;
    }

    public static void stopAll() {
        for (VinylPlayerBlockEntity entity :
                new java.util.ArrayList<>(ACTIVE_SOUNDS.keySet())) {
            stopMinecraftSound(entity);
        }

        for (VinylPlayerBlockEntity entity :
                new java.util.ArrayList<>(LOCAL_PLAYERS.keySet())) {
            stopLocalSound(entity);
        }
    }
}
