package com.radig.vinylcraft.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.ContainerHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.block.Block;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

import net.minecraft.world.level.Level;

import com.radig.vinylcraft.sound.VinylPlayerAudioBridge;
import com.radig.vinylcraft.item.VinylData;
import com.radig.vinylcraft.music.AlbumData;
import com.radig.vinylcraft.music.ModAlbums;

public class VinylPlayerBlockEntity extends BlockEntity {

    private final NonNullList<ItemStack> items =
            NonNullList.withSize(1, ItemStack.EMPTY);

    private boolean playing = false;
    private boolean paused = false;

    private long playbackTicks = 0;


    /*
        * =====================================================
        * CONFIGURACIÓN DE LA ANIMACIÓN DEL BRAZO
        * =====================================================
        */

        /*
        * Posición horizontal.
        *
        * REST = brazo completamente recto.
        *
        * 0.804 sale de:
        *
        * -22.5° + (28° × 0.804) ≈ 0°
        */
        private static final float TONEARM_REST_POSITION =
                0.804F;

        /*
        * Inicio del disco.
        *
        * Antes teníamos 1.22:
        *
        * (1.22 - 0.804) × 28 ≈ 11.6°
        *
        * Por eso apenas se movía.
        *
        * Con 1.95 hacemos aproximadamente 32°
        * desde la posición recta.
        */
        private static final float TONEARM_PLAY_POSITION =
                1.75F;

        /*
        * Zona interior del disco.
        *
        * Durante la canción avanzará lentamente
        * desde 1.95 hasta aquí.
        */
        private static final float TONEARM_END_POSITION =
                2.30F;


        /*
        * Altura del brazo.
        *
        * Antes usábamos:
        *
        * 1.0 = demasiado levantado
        * 0.0 = ligeramente demasiado bajo
        *
        * Ahora usamos un recorrido mucho menor.
        */
        private static final float TONEARM_LIFTED =
                0.45F;

        private static final float TONEARM_DOWN =
                0.15F;


        /*
        * Velocidades.
        */
        private static final float TONEARM_MOVE_SPEED =
                0.06F;

        private static final float TONEARM_LIFT_SPEED =
                0.04F;
                
    /*
     * =====================================================
     * ANIMACIÓN DEL BRAZO
     * =====================================================
     *
     * Estos valores solamente sirven para animación visual.
     * No necesitan guardarse en NBT.
     *
     * tonearmPosition:
     *
     * 0.00 = brazo estacionado
     * 1.00 = comienzo del disco
     * 1.28 = zona interior del disco
     *
     * tonearmLift:
     *
     * 0.00 = aguja abajo
     * 1.00 = aguja levantada
     */

    private float tonearmPosition = TONEARM_REST_POSITION;
    private float previousTonearmPosition = TONEARM_REST_POSITION;

    private float tonearmLift = 1.0F;
    private float previousTonearmLift = 1.0F;

    /*
     * Duración visual del recorrido completo.
     *
     * Por ahora usamos 4 minutos.
     *
     * Más adelante, cuando VinylCraft conozca la
     * duración real de cada canción, podemos sustituir
     * esta constante por la duración del track.
     */
private static final long FALLBACK_ALBUM_TICKS =
        20L * 60L * 4L;

    public VinylPlayerBlockEntity(
            BlockPos pos,
            BlockState state) {

        super(
                ModBlockEntities.VINYL_PLAYER,
                pos,
                state
        );
    }


    public boolean hasVinyl() {
        return !items.get(0).isEmpty();
    }


    public ItemStack getVinyl() {
        return items.get(0);
    }


    public boolean isPlaying() {
        return playing;
    }


    public boolean isPaused() {
        return paused;
    }


    public boolean isStopped() {
        return !playing && !paused;
    }


    public void setPlaying(boolean playing) {

        /*
         * No permitimos reproducir sin vinilo.
         */
        if (playing && !hasVinyl()) {
            return;
        }

        if (playing) {

            /*
             * PLAY o RESUME.
             */
            this.playing = true;
            this.paused = false;

        } else {

            /*
             * Si actualmente estaba reproduciendo,
             * pasar a false significa PAUSA.
             */
            if (this.playing) {

                this.playing = false;
                this.paused = true;
            }
        }

        setChanged();
    }


    public void stopPlayback() {

        playing = false;
        paused = false;

        playbackTicks = 0;

        setChanged();
    }


    public boolean insertVinyl(ItemStack stack) {

        if (hasVinyl()) {
            return false;
        }

        items.set(
                0,
                stack.copyWithCount(1)
        );

        setChanged();

        return true;
    }


    public ItemStack removeVinyl() {

        if (!hasVinyl()) {
            return ItemStack.EMPTY;
        }

        ItemStack removed =
                items.get(0);

        items.set(
                0,
                ItemStack.EMPTY
        );

        /*
         * Retirar el vinilo equivale a STOP.
         *
         * La animación del cliente se encargará de:
         *
         * 1. levantar la aguja
         * 2. regresar el brazo
         */
        playing = false;
        paused = false;

        playbackTicks = 0;

        setChanged();

        return removed;
    }


    public long getPlaybackTicks() {
        return playbackTicks;
    }


    public void resetPlayback() {

        playbackTicks = 0;

        setChanged();
    }


    public void previousTrack() {

        if (!hasVinyl()) {
            return;
        }

        long[] durations = getTrackDurationsTicks();

        if (durations.length == 0) {
            playbackTicks = 0L;
            setChanged();
            return;
        }

        int currentIndex = getTrackIndexAt(playbackTicks, durations);
        long currentStart = getTrackStartTick(currentIndex, durations);
        long elapsedInTrack = Math.max(0L, playbackTicks - currentStart);

        /*
         * Comportamiento típico de reproductor:
         * - si ya pasaron más de 3 s, reinicia la pista actual;
         * - al principio de la pista, salta a la anterior.
         */
        if (elapsedInTrack > 60L) {
            playbackTicks = currentStart;
        } else {
            int previousIndex = Math.max(0, currentIndex - 1);
            playbackTicks = getTrackStartTick(previousIndex, durations);
        }

        setChanged();
    }


    public void nextTrack() {

        if (!hasVinyl()) {
            return;
        }

        long[] durations = getTrackDurationsTicks();

        if (durations.length == 0) {
            return;
        }

        int currentIndex = getTrackIndexAt(playbackTicks, durations);

        if (currentIndex >= durations.length - 1) {
            playbackTicks = getTrackStartTick(
                    durations.length - 1,
                    durations
            );
        } else {
            playbackTicks = getTrackStartTick(
                    currentIndex + 1,
                    durations
            );
        }

        setChanged();
    }


    private int getTrackIndexAt(
            long tick,
            long[] durations) {

        long accumulated = 0L;

        for (int index = 0; index < durations.length; index++) {
            long duration = Math.max(1L, durations[index]);

            if (tick < accumulated + duration) {
                return index;
            }

            accumulated += duration;
        }

        return Math.max(0, durations.length - 1);
    }


    private long getTrackStartTick(
            int trackIndex,
            long[] durations) {

        long start = 0L;

        for (int index = 0; index < trackIndex && index < durations.length; index++) {
            start += Math.max(1L, durations[index]);
        }

        return start;
    }


    /*
     * =====================================================
     * GETTERS INTERPOLADOS
     * =====================================================
     *
     * El renderer los llama usando tickProgress.
     * Esto evita que el movimiento avance a saltitos
     * de 20 FPS lógicos.
     */

    public float getTonearmPosition(float tickProgress) {

        float partial =
                clamp01(tickProgress);

        return previousTonearmPosition
                + (
                    tonearmPosition
                    - previousTonearmPosition
                )
                * partial;
    }


    public float getTonearmLift(float tickProgress) {

        float partial =
                clamp01(tickProgress);

        return previousTonearmLift
                + (
                    tonearmLift
                    - previousTonearmLift
                )
                * partial;
    }


    /*
     * =====================================================
     * TICK
     * =====================================================
     */

    public static void tick(
            Level level,
            BlockPos pos,
            BlockState state,
            VinylPlayerBlockEntity blockEntity) {

        /*
         * El contador avanza tanto en cliente como servidor.
         *
         * En cliente permite que el vinilo gire fluidamente.
         * En servidor mantiene el tiempo de reproducción.
         */
        if (
                blockEntity.playing
                && blockEntity.hasVinyl()
        ) {

            blockEntity.playbackTicks++;

            if (!level.isClientSide()) {
                long albumDuration =
                        blockEntity.getAlbumDurationTicks();

                if (
                        albumDuration > 0L
                                && blockEntity.playbackTicks >= albumDuration
                ) {
                    blockEntity.stopPlayback();
                }
            }
        }


        /*
         * La animación del brazo es puramente visual.
         *
         * No necesitamos sincronizar cada pequeño movimiento
         * por red.
         */
        if (level.isClientSide()) {

        blockEntity.tickTonearmAnimation();

        VinylPlayerAudioBridge.tickClient(
                blockEntity
        );
        }
    }


    /*
     * =====================================================
     * ANIMACIÓN DEL BRAZO
     * =====================================================
     */

    private void tickTonearmAnimation() {

        /*
        * Guardamos el estado anterior para interpolar
        * suavemente entre ticks.
        */
        previousTonearmPosition =
                tonearmPosition;

        previousTonearmLift =
                tonearmLift;


        /*
        * =================================================
        * STOP / SIN VINILO
        * =================================================
        *
        * Primero levantamos ligeramente la aguja.
        * Después regresamos el brazo al soporte.
        */
        if (!hasVinyl() || isStopped()) {

                tonearmLift =
                        approach(
                                tonearmLift,
                                TONEARM_LIFTED,
                                TONEARM_LIFT_SPEED
                        );

                /*
                * Esperamos a que esté prácticamente levantado
                * antes de comenzar el regreso.
                */
                if (
                        tonearmLift
                        >= TONEARM_LIFTED - 0.02F
                ) {

                tonearmPosition =
                        approach(
                                tonearmPosition,
                                TONEARM_REST_POSITION,
                                TONEARM_MOVE_SPEED
                        );
                }

                return;
        }


        /*
        * Posición horizontal correspondiente
        * al tiempo actual del disco.
        */
        float playbackPosition =
                getPlaybackTonearmPosition();


        /*
        * =================================================
        * PAUSE
        * =================================================
        *
        * Conservamos la posición horizontal,
        * pero levantamos ligeramente la aguja.
        */
        if (paused) {

                tonearmPosition =
                        approach(
                                tonearmPosition,
                                playbackPosition,
                                TONEARM_MOVE_SPEED
                        );

                tonearmLift =
                        approach(
                                tonearmLift,
                                TONEARM_LIFTED,
                        TONEARM_LIFT_SPEED
                );

        return;
    }


    /*
     * =================================================
     * PLAY
     * =================================================
     */
    if (playing) {

        /*
         * Primero desplazamos el brazo hacia
         * la posición correspondiente del disco.
         */
        tonearmPosition =
                approach(
                        tonearmPosition,
                        playbackPosition,
                        TONEARM_MOVE_SPEED
                );


        /*
         * Mientras todavía viaja hacia el disco,
         * permanece levantado.
         */
        if (
                tonearmPosition
                < TONEARM_PLAY_POSITION - 0.02F
        ) {

            tonearmLift =
                    approach(
                            tonearmLift,
                            TONEARM_LIFTED,
                            TONEARM_LIFT_SPEED
                    );

        } else {

            /*
             * Ya llegó al disco:
             * bajamos suavemente la aguja.
             *
             * No bajamos hasta 0.0 porque visualmente
             * quedaba un poco hundida en el vinilo.
             */
            tonearmLift =
                    approach(
                            tonearmLift,
                            TONEARM_DOWN,
                            TONEARM_LIFT_SPEED
                    );
        }
    }
}


    /*
     * Calcula la posición horizontal del brazo según
     * cuánto tiempo lleva reproduciéndose el vinilo.
     */
    private float getPlaybackTonearmPosition() {

        long durationTicks =
                getAlbumDurationTicks();

        if (durationTicks <= 0L) {
            durationTicks = FALLBACK_ALBUM_TICKS;
        }

        float progress =
                playbackTicks
                / (float) durationTicks;

        progress =
                clamp01(progress);

        return TONEARM_PLAY_POSITION
                + (
                    TONEARM_END_POSITION
                    - TONEARM_PLAY_POSITION
                )
                * progress;
    }


    private long getAlbumDurationTicks() {

        long[] durations = getTrackDurationsTicks();

        if (durations.length > 0) {
            long total = 0L;

            for (long duration : durations) {
                total += Math.max(1L, duration);
            }

            return Math.max(1L, total);
        }

        if (!hasVinyl()) {
            return 0L;
        }

        String albumId =
                VinylData.getAlbumId(getVinyl());

        AlbumData album =
                ModAlbums.get(albumId);

        if (album == null || album.isEmpty()) {
            return 0L;
        }

        return Math.max(1L, album.totalDurationTicks());
    }


    private long[] getTrackDurationsTicks() {

        if (!hasVinyl()) {
            return new long[0];
        }

        long[] fromVinyl =
                VinylData.getTrackDurationsTicks(
                        getVinyl()
                );

        if (fromVinyl.length > 0) {
            return fromVinyl;
        }

        String albumId =
                VinylData.getAlbumId(getVinyl());

        AlbumData album =
                ModAlbums.get(albumId);

        if (album == null || album.isEmpty()) {
            return new long[0];
        }

        long[] result = new long[album.trackCount()];

        for (int index = 0; index < album.trackCount(); index++) {
            var track = album.getTrack(index);
            result[index] = track == null
                    ? 1L
                    : Math.max(1L, track.durationTicks());
        }

        return result;
    }


    /*
     * Acerca gradualmente un valor hacia su objetivo.
     */
    private static float approach(
            float current,
            float target,
            float speed) {

        if (current < target) {

            return Math.min(
                    current + speed,
                    target
            );

        } else if (current > target) {

            return Math.max(
                    current - speed,
                    target
            );
        }

        return target;
    }


    private static float clamp01(float value) {

        if (value < 0.0F) {
            return 0.0F;
        }

        if (value > 1.0F) {
            return 1.0F;
        }

        return value;
    }


    /*
     * =====================================================
     * GUARDADO
     * =====================================================
     */

    @Override
    protected void saveAdditional(
            ValueOutput output) {

        ContainerHelper.saveAllItems(
                output,
                items
        );

        output.putBoolean(
                "Playing",
                playing
        );

        output.putBoolean(
                "Paused",
                paused
        );

        output.putLong(
                "PlaybackTicks",
                playbackTicks
        );

        super.saveAdditional(output);
    }


    @Override
    protected void loadAdditional(
            ValueInput input) {

        super.loadAdditional(input);

        /*
         * Limpiamos primero para evitar que el cliente
         * conserve visualmente un vinilo ya retirado.
         */
        items.clear();

        ContainerHelper.loadAllItems(
                input,
                items
        );

        playing =
                input.getBooleanOr(
                        "Playing",
                        false
                );

        paused =
                input.getBooleanOr(
                        "Paused",
                        false
                );

        playbackTicks =
                input.getLongOr(
                        "PlaybackTicks",
                        0L
                );
    }


    @Override
    public void preRemoveSideEffects(
            BlockPos pos,
            BlockState state) {

        if (
                level != null
                && !level.isClientSide()
                && hasVinyl()
        ) {

            Block.popResource(
                    level,
                    pos,
                    removeVinyl()
            );
        }

        super.preRemoveSideEffects(
                pos,
                state
        );
    }


    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registryLookup) {

        return saveWithoutMetadata(
                registryLookup
        );
    }


    @Override
    public Packet<ClientGamePacketListener>
            getUpdatePacket() {

        return ClientboundBlockEntityDataPacket
                .create(this);
    }


    @Override
    public void setChanged() {

        super.setChanged();

        if (
                level != null
                && !level.isClientSide()
        ) {

            BlockState state =
                    getBlockState();

            level.sendBlockUpdated(
                    worldPosition,
                    state,
                    state,
                    Block.UPDATE_CLIENTS
            );
        }
    }
}