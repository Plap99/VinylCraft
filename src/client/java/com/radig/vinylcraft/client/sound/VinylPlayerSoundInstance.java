package com.radig.vinylcraft.client.sound;

import com.radig.vinylcraft.block.entity.VinylPlayerBlockEntity;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

import net.minecraft.sounds.SoundEvent;

public class VinylPlayerSoundInstance
        extends AbstractTickableSoundInstance {

    private static final float BASE_VOLUME = 0.65F;

    private final VinylPlayerBlockEntity playerEntity;

    private float configuredVolume = 1.0F;
    private boolean pausedByVinyl = false;

    public VinylPlayerSoundInstance(
            VinylPlayerBlockEntity playerEntity,
            SoundEvent soundEvent) {

        super(
                soundEvent,
                SoundSource.RECORDS,
                SoundInstance.createUnseededRandom()
        );

        this.playerEntity = playerEntity;

        this.volume = BASE_VOLUME;
        this.pitch = 1.0F;

        this.looping = false;

        // El sonido pertenece al tocadiscos,
        // no al jugador/cámara.
        this.relative = false;

        // Atenuación posicional.
        this.attenuation =
                SoundInstance.Attenuation.LINEAR;

        updatePosition();
    }

    @Override
    public void tick() {

        /*
         * El sonido solamente debe destruirse si:
         *
         * - desapareció el reproductor,
         * - se retiró el vinilo,
         * - o se pulsó STOP.
         *
         * PAUSE NO debe detener la instancia.
         */
        if (playerEntity.isRemoved()) {
            stop();
            return;
        }

        if (!playerEntity.hasVinyl()) {
            stop();
            return;
        }

        if (playerEntity.isStopped()) {
            stop();
            return;
        }

        updatePosition();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    private void updatePosition() {

        this.x =
                playerEntity.getBlockPos().getX()
                + 0.5D;

        this.y =
                playerEntity.getBlockPos().getY()
                + 0.5D;

        this.z =
                playerEntity.getBlockPos().getZ()
                + 0.5D;
    }

    /*
     * Mantener también el volumen lógico del SoundInstance en 0 durante
     * PAUSE es importante. Minecraft puede recalcular el volumen del canal
     * al cerrar inventarios/menús antes de que VinylCraft vuelva a ejecutar
     * Channel.pause(). Si el SoundInstance siguiera diciendo 0.65, podría
     * escaparse un pequeño "blip" de audio.
     */
    public void setPausedVolume(boolean paused) {
        this.pausedByVinyl = paused;
        refreshVolume();
    }

    public void setConfiguredVolume(float volume) {
        this.configuredVolume = Math.max(
                0.0F,
                Math.min(1.0F, volume)
        );
        refreshVolume();
    }

    private void refreshVolume() {
        this.volume = pausedByVinyl
                ? 0.0F
                : BASE_VOLUME * configuredVolume;
    }

    public VinylPlayerBlockEntity getPlayerEntity() {
        return playerEntity;
    }
}