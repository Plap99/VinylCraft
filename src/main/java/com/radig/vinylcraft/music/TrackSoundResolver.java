package com.radig.vinylcraft.music;

import com.radig.vinylcraft.sound.ModSounds;

import net.minecraft.sounds.SoundEvent;

public final class TrackSoundResolver {

    private TrackSoundResolver() {
    }

    /*
     * Convierte una pista de VinylCraft en un SoundEvent
     * reproducible por el sistema actual de Minecraft.
     *
     * IMPORTANTE:
     * Esta implementación es solamente el puente temporal
     * para las pistas incluidas dentro del mod.
     *
     * Las pistas importadas por el usuario utilizarán
     * posteriormente un backend de audio dinámico.
     */
    public static SoundEvent resolve(TrackData track) {

        if (track == null) {
            return null;
        }

        return switch (track.audioFile()) {

            case "get_your_shine_on" ->
                    ModSounds.GET_YOUR_SHINE_ON;

            default ->
                    null;
        };
    }
}