package com.radig.vinylcraft.client.sound;

import java.io.IOException;
import java.nio.file.Path;

import com.mojang.blaze3d.audio.Library;
import com.radig.vinylcraft.client.mixin.SoundEngineAccessor;
import com.radig.vinylcraft.client.mixin.SoundManagerAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.world.phys.Vec3;

/**
 * Reproductor local posicional para VinylCraft.
 *
 * El MP3/WAV original se decodifica a PCM mono y se entrega a un canal
 * streaming de Minecraft/OpenAL. El alcance puede modificarse en caliente
 * desde la configuración F6.
 */
public final class PositionalLocalTrackPlayer {

    private final Path path;
    private final Vec3 position;
    private final double startSeconds;

    private volatile ChannelAccess.ChannelHandle handle;
    private volatile AudioStream stream;

    private volatile boolean started;
    private volatile boolean stopped;
    private volatile boolean paused;
    private volatile boolean finished;
    private volatile boolean startupFailed;

    private volatile float volume = 0.65F;
    private volatile float attenuationDistance = 32.0F;

    public PositionalLocalTrackPlayer(
            Path path,
            Vec3 position) {

        this(path, position, 0.0D);
    }

    public PositionalLocalTrackPlayer(
            Path path,
            Vec3 position,
            double startSeconds) {

        this.path = path.toAbsolutePath().normalize();
        this.position = position;
        this.startSeconds = Math.max(0.0D, startSeconds);
    }

    public Path path() {
        return path;
    }

    public boolean isFinished() {
        ChannelAccess.ChannelHandle localHandle = handle;

        if (
                started
                        && localHandle != null
                        && localHandle.isStopped()
        ) {
            finished = true;
        }

        return finished;
    }

    public boolean hasStartupFailed() {
        return startupFailed;
    }

    public void start(boolean initiallyPaused) {
        if (started || stopped) {
            return;
        }

        started = true;
        paused = initiallyPaused;

        final AudioStream newStream;

        try {
            newStream = LocalAudioStreamFactory.open(
                    path,
                    startSeconds
            );
            stream = newStream;
        } catch (IOException exception) {
            finished = true;
            startupFailed = true;
            System.err.println(
                    "[VinylCraft] No se pudo abrir audio posicional: "
                            + path
            );
            exception.printStackTrace();
            return;
        }

        SoundManager soundManager =
                Minecraft.getInstance().getSoundManager();

        SoundEngine soundEngine =
                ((SoundManagerAccessor) soundManager)
                        .vinylcraft$getSoundEngine();

        ChannelAccess channelAccess =
                ((SoundEngineAccessor) soundEngine)
                        .vinylcraft$getChannelAccess();

        channelAccess
                .createHandle(Library.Pool.STREAMING)
                .thenAccept(channelHandle -> {

                    if (channelHandle == null) {
                        finished = true;
                        startupFailed = true;
                        closeStreamQuietly();
                        return;
                    }

                    handle = channelHandle;

                    channelHandle.execute(channel -> {
                        if (stopped) {
                            channel.stop();
                            return;
                        }

                        channel.setRelative(false);
                        channel.setSelfPosition(position);
                        channel.setPitch(1.0F);
                        channel.setVolume(paused ? 0.0F : volume);
                        channel.linearAttenuation(attenuationDistance);
                        channel.attachBufferStream(newStream);
                        channel.play();

                        if (paused) {
                            channel.pause();
                        }
                    });
                });
    }

    public void setPaused(boolean paused) {
        /*
         * No hacemos early-return aunque el valor lógico no cambie.
         * Minecraft puede reanudar globalmente canales al cerrar GUIs;
         * mientras el vinilo esté pausado reafirmamos mute + pause.
         */
        this.paused = paused;

        ChannelAccess.ChannelHandle localHandle = handle;

        if (
                localHandle == null
                        || localHandle.isStopped()
        ) {
            return;
        }

        localHandle.execute(channel -> {
            if (paused) {
                channel.setVolume(0.0F);
                channel.pause();
            } else if (!stopped) {
                channel.setVolume(volume);
                channel.unpause();
            }
        });
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0F, Math.min(1.0F, volume));

        ChannelAccess.ChannelHandle localHandle = handle;

        if (
                localHandle == null
                        || localHandle.isStopped()
        ) {
            return;
        }

        final float appliedVolume = paused ? 0.0F : this.volume;

        localHandle.execute(
                channel -> channel.setVolume(appliedVolume)
        );
    }

    /** Cambia en caliente el radio máximo de atenuación OpenAL. */
    public void setAttenuationDistance(float distance) {
        this.attenuationDistance = Math.max(1.0F, distance);

        ChannelAccess.ChannelHandle localHandle = handle;

        if (
                localHandle == null
                        || localHandle.isStopped()
        ) {
            return;
        }

        final float appliedDistance = this.attenuationDistance;

        localHandle.execute(
                channel -> channel.linearAttenuation(appliedDistance)
        );
    }

    public void stop() {
        if (stopped) {
            return;
        }

        stopped = true;
        paused = false;

        ChannelAccess.ChannelHandle localHandle = handle;

        if (
                localHandle != null
                        && !localHandle.isStopped()
        ) {
            localHandle.execute(channel -> channel.stop());
        } else {
            closeStreamQuietly();
        }
    }

    private void closeStreamQuietly() {
        AudioStream localStream = stream;
        stream = null;

        if (localStream == null) {
            return;
        }

        try {
            localStream.close();
        } catch (IOException ignored) {
        }
    }
}
