package com.radig.vinylcraft.client.sound;

import com.radig.vinylcraft.block.entity.VinylPlayerBlockEntity;
import com.radig.vinylcraft.sound.ModSounds;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

public class VinylPlayerSoundInstance
        extends AbstractTickableSoundInstance {

    private static final float BASE_VOLUME = 0.65F;

    private final VinylPlayerBlockEntity playerEntity;

    public VinylPlayerSoundInstance(
            VinylPlayerBlockEntity playerEntity) {

        super(
                ModSounds.GET_YOUR_SHINE_ON,
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
        this.volume = paused
                ? 0.0F
                : BASE_VOLUME;
    }

    public VinylPlayerBlockEntity getPlayerEntity() {
        return playerEntity;
    }
}