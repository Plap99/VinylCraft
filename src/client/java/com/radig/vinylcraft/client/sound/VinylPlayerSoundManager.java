package com.radig.vinylcraft.client.sound;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.radig.vinylcraft.block.entity.VinylPlayerBlockEntity;
import com.radig.vinylcraft.client.mixin.SoundEngineAccessor;
import com.radig.vinylcraft.client.mixin.SoundManagerAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;

import com.radig.vinylcraft.music.AlbumData;
import com.radig.vinylcraft.music.ModAlbums;
import com.radig.vinylcraft.music.TrackData;
import com.radig.vinylcraft.music.TrackSoundResolver;

import net.minecraft.sounds.SoundEvent;

import com.radig.vinylcraft.item.VinylData;

public final class VinylPlayerSoundManager {

    /*
     * Cada BlockEntity mantiene su propia instancia de sonido.
     *
     * Así podemos tener varios tocadiscos reproduciendo
     * canciones independientemente.
     */
    private static final Map<
            VinylPlayerBlockEntity,
            VinylPlayerSoundInstance
    > ACTIVE_SOUNDS =
            new IdentityHashMap<>();


    /*
     * Tocadiscos cuyo canal de audio está actualmente
     * congelado mediante Channel.pause().
     */
    private static final Set<
            VinylPlayerBlockEntity
    > PAUSED_PLAYERS =
            Collections.newSetFromMap(
                    new IdentityHashMap<>()
            );


    private VinylPlayerSoundManager() {
    }


    public static void tickPlayer(
            VinylPlayerBlockEntity playerEntity) {

        SoundManager soundManager =
                Minecraft.getInstance()
                        .getSoundManager();

        VinylPlayerSoundInstance currentSound =
                ACTIVE_SOUNDS.get(playerEntity);


        /*
         * STOP / quitar vinilo / destruir reproductor.
         *
         * Aquí sí eliminamos completamente el sonido.
         */
        if (
                playerEntity.isRemoved()
                || !playerEntity.hasVinyl()
                || playerEntity.isStopped()
        ) {

            if (currentSound != null) {

                soundManager.stop(currentSound);

                ACTIVE_SOUNDS.remove(
                        playerEntity
                );
            }

            PAUSED_PLAYERS.remove(
                    playerEntity
            );

            return;
        }


        /*
         * PAUSE
         *
         * No destruimos SoundInstance.
         * Solamente congelamos su canal OpenAL.
         */
        if (playerEntity.isPaused()) {

            /*
             * Minecraft puede pausar y reanudar globalmente
             * los canales al abrir/cerrar pantallas.
             *
             * Por eso NO confiamos únicamente en
             * PAUSED_PLAYERS para saber si OpenAL sigue
             * realmente pausado. Mientras el tocadiscos
             * esté en PAUSE, reafirmamos el pause en cada
             * tick del cliente.
             *
             * Channel.pause() es seguro aunque el canal
             * ya esté pausado y evita que ESC/inventario
             * reanuden accidentalmente nuestra música.
             */
            if (
                    currentSound != null
                    && !currentSound.isStopped()
            ) {

                /*
                 * Silenciamos también el SoundInstance, no solamente
                 * el canal OpenAL. Así, si Minecraft recalcula el volumen
                 * durante su resume() global al cerrar una pantalla,
                 * seguirá obteniendo 0 y no puede escaparse un blip.
                 */
                currentSound.setPausedVolume(true);

                if (pauseSound(currentSound)) {

                    PAUSED_PLAYERS.add(
                            playerEntity
                    );
                }
            }

            return;
        }


        /*
         * PLAY inicial.
         *
         * Si todavía no existe una instancia,
         * creamos una nueva canción.
         */
        if (
                currentSound == null
                || currentSound.isStopped()
        ) {

            String albumId =
                VinylData.getAlbumId(
                        playerEntity.getVinyl()
                );

        if (albumId == null) {
        return;
        }

        AlbumData album =
                ModAlbums.get(albumId);

        if (
                album == null
                || album.isEmpty()
        ) {
        return;
        }

        TrackData track =
                album.getTrack(0);

        if (track == null) {
        return;
        }

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

            newSound.setPausedVolume(false);

            ACTIVE_SOUNDS.put(
                    playerEntity,
                    newSound
            );

            PAUSED_PLAYERS.remove(
                    playerEntity
            );

            soundManager.play(
                    newSound
            );

            return;
        }


        /*
         * RESUME
         *
         * Si ya había una instancia y estaba pausada,
         * reanudamos exactamente ese mismo canal.
         */
        if (
                PAUSED_PLAYERS.contains(
                        playerEntity
                )
        ) {

            /*
             * Primero restauramos el volumen lógico de la instancia.
             * Después resumeSound() calcula el volumen final respetando
             * MASTER + RECORDS/JUKEBOX.
             */
            currentSound.setPausedVolume(false);

            if (resumeSound(currentSound)) {

                PAUSED_PLAYERS.remove(
                        playerEntity
                );
            }
        }
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

        /*
         * IMPORTANTE:
         *
         * Primero dejamos el canal a volumen 0 y DESPUÉS lo
         * pausamos. Minecraft puede ejecutar un resume() global
         * al cerrar inventarios o menús. Si eso ocurre, el canal
         * podrá quedar técnicamente reanudado durante una fracción
         * de tiempo, pero seguirá completamente mudo.
         *
         * Esto elimina el pequeño "blip" que se escuchaba al
         * regresar al juego.
         */
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

        /*
         * Restauramos el volumen REAL actual antes de reanudar.
         * Esto respeta tanto el volumen propio de VinylCraft como
         * el deslizador de la categoría RECORDS/JUKEBOX de Minecraft.
         */
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


    public static void stopAll() {

        SoundManager soundManager =
                Minecraft.getInstance()
                        .getSoundManager();

        for (
                VinylPlayerSoundInstance sound
                : ACTIVE_SOUNDS.values()
        ) {

            soundManager.stop(sound);
        }

        ACTIVE_SOUNDS.clear();
        PAUSED_PLAYERS.clear();
    }
}