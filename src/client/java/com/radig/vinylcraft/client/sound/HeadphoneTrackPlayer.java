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

/** Canal streaming estéreo y relativo: música privada de audífonos. */
public final class HeadphoneTrackPlayer {

    private final Path path;
    private final double startSeconds;

    private volatile ChannelAccess.ChannelHandle handle;
    private volatile AudioStream stream;
    private volatile boolean started;
    private volatile boolean stopped;
    private volatile boolean paused;
    private volatile boolean startupFailed;
    private volatile float volume = 0.65F;

    public HeadphoneTrackPlayer(Path path, double startSeconds) {
        this.path = path.toAbsolutePath().normalize();
        this.startSeconds = Math.max(0.0D, startSeconds);
    }

    public Path path() {
        return path;
    }

    public boolean hasStartupFailed() {
        return startupFailed;
    }

    public boolean isFinished() {
        ChannelAccess.ChannelHandle local = handle;
        return started && local != null && local.isStopped();
    }

    public void start(boolean initiallyPaused) {
        if (started || stopped) {
            return;
        }

        started = true;
        paused = initiallyPaused;

        final AudioStream newStream;

        try {
            newStream = HeadphoneAudioStreamFactory.open(path, startSeconds);
            stream = newStream;
        } catch (IOException exception) {
            startupFailed = true;
            closeStreamQuietly();
            System.err.println("[VinylCraft] No se pudo abrir audio de Discman: " + path);
            exception.printStackTrace();
            return;
        }

        SoundManager manager = Minecraft.getInstance().getSoundManager();
        SoundEngine engine = ((SoundManagerAccessor) manager).vinylcraft$getSoundEngine();
        ChannelAccess access = ((SoundEngineAccessor) engine).vinylcraft$getChannelAccess();

        access.createHandle(Library.Pool.STREAMING).thenAccept(channelHandle -> {
            if (channelHandle == null) {
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

                // Relative + sin atenuación = audífonos privados/no posicionales.
                channel.setRelative(true);
                channel.disableAttenuation();
                channel.setPitch(1.0F);
                channel.setVolume(paused ? 0.0F : volume);
                channel.attachBufferStream(newStream);
                channel.play();

                if (paused) {
                    channel.pause();
                }
            });
        });
    }

    public void setPaused(boolean paused) {
        this.paused = paused;

        ChannelAccess.ChannelHandle local = handle;

        if (local == null || local.isStopped()) {
            return;
        }

        local.execute(channel -> {
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

        ChannelAccess.ChannelHandle local = handle;

        if (local == null || local.isStopped()) {
            return;
        }

        float applied = paused ? 0.0F : this.volume;
        local.execute(channel -> channel.setVolume(applied));
    }

    public void stop() {
        if (stopped) {
            return;
        }

        stopped = true;
        paused = false;

        ChannelAccess.ChannelHandle local = handle;

        if (local != null && !local.isStopped()) {
            local.execute(channel -> channel.stop());
        } else {
            closeStreamQuietly();
        }
    }

    private void closeStreamQuietly() {
        AudioStream local = stream;
        stream = null;

        if (local == null) {
            return;
        }

        try {
            local.close();
        } catch (IOException ignored) {
        }
    }
}
