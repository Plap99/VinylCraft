package com.radig.vinylcraft.block.entity;

import java.util.UUID;

import com.radig.vinylcraft.item.ModItems;
import com.radig.vinylcraft.item.VinylData;
import com.radig.vinylcraft.recorder.VinylRecorderClientBridge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class VinylRecorderBlockEntity
        extends net.minecraft.world.level.block.entity.BlockEntity {

    public static final long RECORDING_DURATION_TICKS =
            20L * 30L;

    public enum CloneStage {
        IDLE,
        READING_SOURCE,
        SOURCE_READY,
        WAITING_BLANK,
        WRITING_COPY,
        COMPLETE
    }

    private static final float ARM_REST_POSITION = 0.69F;
    private static final float ARM_START_POSITION = 1.93F;
    private static final float ARM_END_POSITION = 2.52F;
    private static final float ARM_SPEED = 0.035F;

    private final NonNullList<ItemStack> items =
            NonNullList.withSize(1, ItemStack.EMPTY);

    /*
     * Grabación normal desde la GUI.
     */
    private boolean recording;
    private long recordingTicks;
    private String pendingAlbumId = "";
    private String pendingTrackDurations = "";

    /*
     * Luz verde: únicamente significa "proceso terminado, retira el disco".
     * No se enciende por el simple hecho de insertar un álbum grabado.
     */
    private boolean completedLight;

    /*
     * Estado de clonación.
     *
     * READING_SOURCE  -> lee el original durante 30 s.
     * SOURCE_READY    -> terminó de leer; original aún está dentro.
     * WAITING_BLANK   -> original retirado; espera un vinilo virgen.
     * WRITING_COPY    -> graba el clon durante 30 s.
     * COMPLETE        -> clon terminado; luz verde hasta retirar el disco.
     */
    private CloneStage cloneStage = CloneStage.IDLE;
    private long cloneTicks;

    /*
     * Snapshot de los datos necesarios para crear la copia.
     * No guardamos un ItemStack duplicado: sólo los componentes del álbum.
     */
    /** ID del álbum fuente leído por el clonador. */
    private String cloneAlbumId = "";

    /**
     * ID NUEVO que recibirá la copia. Nunca debe coincidir con cloneAlbumId:
     * dos discos clonados empiezan iguales, pero sus metadatos se editan de
     * forma completamente independiente.
     */
    private String cloneResultAlbumId = "";

    private String cloneTrackDurations = "";
    private int cloneWallSize = 1;

    private float tonearmPosition = ARM_REST_POSITION;
    private float previousTonearmPosition = ARM_REST_POSITION;

    public VinylRecorderBlockEntity(
            BlockPos pos,
            BlockState state) {

        super(ModBlockEntities.VINYL_RECORDER, pos, state);
    }

    public boolean hasVinyl() {
        return !items.get(0).isEmpty();
    }

    public ItemStack getVinyl() {
        return items.get(0);
    }

    public boolean hasBlankVinyl() {
        return hasVinyl() && !VinylData.hasAlbum(getVinyl());
    }

    public boolean hasRecordedVinyl() {
        return hasVinyl() && VinylData.hasAlbum(getVinyl());
    }

    public CloneStage getCloneStage() {
        return cloneStage;
    }

    public boolean isCloneReading() {
        return cloneStage == CloneStage.READING_SOURCE;
    }

    public boolean isCloneSourceReady() {
        return cloneStage == CloneStage.SOURCE_READY;
    }

    public boolean isWaitingForCloneBlank() {
        return cloneStage == CloneStage.WAITING_BLANK;
    }

    public boolean isCloneWriting() {
        return cloneStage == CloneStage.WRITING_COPY;
    }

    public boolean isCloneComplete() {
        return cloneStage == CloneStage.COMPLETE;
    }

    public boolean isCloneSessionActive() {
        return cloneStage != CloneStage.IDLE;
    }

    public boolean isBusy() {
        return recording
                || cloneStage == CloneStage.READING_SOURCE
                || cloneStage == CloneStage.WRITING_COPY;
    }

    public boolean isMechanicalProcessActive() {
        return isBusy();
    }

    public boolean isCompletedLightOn() {
        return completedLight;
    }

    public boolean isWrongCloneTargetInserted() {
        return cloneStage == CloneStage.WAITING_BLANK
                && hasRecordedVinyl();
    }

    public boolean shouldShowYellowLight() {
        return cloneStage == CloneStage.READING_SOURCE
                || cloneStage == CloneStage.SOURCE_READY
                || cloneStage == CloneStage.WAITING_BLANK;
    }

    public boolean shouldHardBlinkYellow() {
        return cloneStage == CloneStage.SOURCE_READY
                || cloneStage == CloneStage.WAITING_BLANK;
    }

    public boolean shouldShowRedProcessLight() {
        return recording || cloneStage == CloneStage.WRITING_COPY;
    }

    public boolean insertVinyl(ItemStack stack) {

        if (
                stack == null
                        || stack.isEmpty()
                        || hasVinyl()
                        || !stack.is(ModItems.BLANK_VINYL)
                        || isBusy()
                        || cloneStage == CloneStage.SOURCE_READY
                        || cloneStage == CloneStage.COMPLETE
        ) {
            return false;
        }

        items.set(0, stack.copyWithCount(1));
        completedLight = false;

        /*
         * Durante una clonación ya leída, insertar un disco decide el paso:
         * - virgen  -> comienza automáticamente la escritura.
         * - grabado -> se queda esperando; amarillo parpadea y rojo marca error.
         */
        if (cloneStage == CloneStage.WAITING_BLANK) {
            if (hasBlankVinyl()) {
                cloneStage = CloneStage.WRITING_COPY;
                cloneTicks = 0L;
            }

            setChanged();
            return true;
        }

        /*
         * Inserción normal fuera de una clonación.
         */
        recording = false;
        recordingTicks = 0L;
        pendingAlbumId = "";
        pendingTrackDurations = "";
        cloneStage = CloneStage.IDLE;
        clearCloneSnapshot();
        setChanged();
        return true;
    }

    public ItemStack removeVinyl() {

        if (!hasVinyl() || isBusy()) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = items.get(0);
        items.set(0, ItemStack.EMPTY);

        recordingTicks = 0L;
        pendingAlbumId = "";
        pendingTrackDurations = "";
        completedLight = false;

        if (cloneStage == CloneStage.SOURCE_READY) {
            /*
             * El original ya fue leído: al retirarlo conservamos el snapshot
             * y pasamos a esperar el vinilo virgen.
             */
            cloneStage = CloneStage.WAITING_BLANK;
            cloneTicks = RECORDING_DURATION_TICKS;
        } else if (cloneStage == CloneStage.WAITING_BLANK) {
            /*
             * Se retiró un disco incorrecto. Continuamos esperando uno virgen.
             */
            cloneTicks = RECORDING_DURATION_TICKS;
        } else if (cloneStage == CloneStage.COMPLETE) {
            resetCloneSession();
        } else {
            cloneTicks = 0L;
            cloneStage = CloneStage.IDLE;
            clearCloneSnapshot();
        }

        setChanged();
        return removed;
    }

    public boolean isRecording() {
        return recording;
    }

    public long getRecordingTicks() {
        return recordingTicks;
    }

    public long getCloneTicks() {
        return cloneTicks;
    }

    public String getCloneSourceAlbumId() {
        return cloneAlbumId;
    }

    public String getCloneResultAlbumId() {
        return cloneResultAlbumId;
    }

    public long getMechanicalProcessTicks() {
        if (recording) {
            return recordingTicks;
        }

        if (
                cloneStage == CloneStage.READING_SOURCE
                        || cloneStage == CloneStage.WRITING_COPY
        ) {
            return cloneTicks;
        }

        return 0L;
    }

    public float getRecordingProgress() {
        return Math.min(
                1.0F,
                getMechanicalProcessTicks()
                        / (float) RECORDING_DURATION_TICKS
        );
    }

    public boolean startRecording(
            String albumId,
            String trackDurations) {

        if (
                recording
                        || cloneStage != CloneStage.IDLE
                        || !hasBlankVinyl()
                        || albumId == null
                        || albumId.isBlank()
                        || trackDurations == null
                        || trackDurations.isBlank()
        ) {
            return false;
        }

        pendingAlbumId = albumId;
        pendingTrackDurations = trackDurations;
        recordingTicks = 0L;
        recording = true;
        completedLight = false;
        setChanged();
        return true;
    }

    public boolean startCloneReading() {

        if (
                recording
                        || cloneStage != CloneStage.IDLE
                        || !hasRecordedVinyl()
        ) {
            return false;
        }

        String albumId = VinylData.getAlbumId(getVinyl());

        if (albumId == null || albumId.isBlank()) {
            return false;
        }

        cloneAlbumId = albumId;
        cloneResultAlbumId = "recorded_clone_" + UUID.randomUUID();

        String durations =
                VinylData.getTrackDurationsEncoded(getVinyl());

        cloneTrackDurations =
                durations == null ? "" : durations;

        cloneWallSize = VinylData.getWallSize(getVinyl());
        cloneTicks = 0L;
        cloneStage = CloneStage.READING_SOURCE;
        completedLight = false;
        setChanged();
        return true;
    }

    public float getTonearmPosition(float tickProgress) {
        float partial = clamp01(tickProgress);

        return previousTonearmPosition
                + (tonearmPosition - previousTonearmPosition) * partial;
    }

    public static void tick(
            Level level,
            BlockPos pos,
            BlockState state,
            VinylRecorderBlockEntity blockEntity) {

        if (!level.isClientSide()) {
            blockEntity.tickServerProcesses();
        } else {
            blockEntity.tickClientProcesses();
        }

        if (level.isClientSide()) {
            blockEntity.tickTonearmAnimation();
        }
    }

    private void tickClientProcesses() {
        if (
                cloneStage == CloneStage.COMPLETE
                        && !cloneAlbumId.isBlank()
                        && !cloneResultAlbumId.isBlank()
        ) {
            VinylRecorderClientBridge.ensureIndependentCloneAlbum(
                    cloneAlbumId,
                    cloneResultAlbumId
            );
        }

        if (recording) {
            recordingTicks = Math.min(
                    RECORDING_DURATION_TICKS,
                    recordingTicks + 1L
            );
        }

        if (
                cloneStage == CloneStage.READING_SOURCE
                        || cloneStage == CloneStage.WRITING_COPY
        ) {
            cloneTicks = Math.min(
                    RECORDING_DURATION_TICKS,
                    cloneTicks + 1L
            );
        }
    }

    private void tickServerProcesses() {
        tickServerRecording();
        tickServerCloning();
    }

    private void tickServerRecording() {

        if (!recording) {
            return;
        }

        if (!hasBlankVinyl() || pendingAlbumId.isBlank()) {
            recording = false;
            recordingTicks = 0L;
            pendingAlbumId = "";
            pendingTrackDurations = "";
            completedLight = false;
            setChanged();
            return;
        }

        recordingTicks++;

        if (recordingTicks < RECORDING_DURATION_TICKS) {
            return;
        }

        VinylData.setAlbumId(
                getVinyl(),
                pendingAlbumId
        );

        VinylData.setTrackDurationsEncoded(
                getVinyl(),
                pendingTrackDurations
        );

        recording = false;
        recordingTicks = RECORDING_DURATION_TICKS;
        pendingAlbumId = "";
        pendingTrackDurations = "";
        completedLight = true;
        setChanged();
    }

    private void tickServerCloning() {

        if (cloneStage == CloneStage.READING_SOURCE) {

            if (!hasRecordedVinyl()) {
                resetCloneSession();
                setChanged();
                return;
            }

            String currentAlbumId = VinylData.getAlbumId(getVinyl());

            if (
                    currentAlbumId == null
                            || !currentAlbumId.equals(cloneAlbumId)
            ) {
                resetCloneSession();
                setChanged();
                return;
            }

            cloneTicks++;

            if (cloneTicks >= RECORDING_DURATION_TICKS) {
                cloneTicks = RECORDING_DURATION_TICKS;
                cloneStage = CloneStage.SOURCE_READY;
                setChanged();
            }

            return;
        }

        if (cloneStage != CloneStage.WRITING_COPY) {
            return;
        }

        if (!hasBlankVinyl() || cloneAlbumId.isBlank()) {
            /*
             * Si algo externo retira/cambia el disco durante escritura,
             * volvemos a esperar un vinilo virgen sin perder el snapshot.
             */
            cloneStage = CloneStage.WAITING_BLANK;
            cloneTicks = RECORDING_DURATION_TICKS;
            completedLight = false;
            setChanged();
            return;
        }

        cloneTicks++;

        if (cloneTicks < RECORDING_DURATION_TICKS) {
            return;
        }

        if (cloneResultAlbumId.isBlank()) {
            // Sesiones antiguas o dañadas: genera un ID independiente antes
            // de terminar, en vez de enlazar accidentalmente al original.
            cloneResultAlbumId = "recorded_clone_" + UUID.randomUUID();
        }

        VinylData.setAlbumId(
                getVinyl(),
                cloneResultAlbumId
        );

        if (!cloneTrackDurations.isBlank()) {
            VinylData.setTrackDurationsEncoded(
                    getVinyl(),
                    cloneTrackDurations
            );
        }

        VinylData.setWallSize(
                getVinyl(),
                cloneWallSize
        );

        cloneTicks = RECORDING_DURATION_TICKS;
        cloneStage = CloneStage.COMPLETE;
        completedLight = true;
        setChanged();
    }

    private void tickTonearmAnimation() {
        previousTonearmPosition = tonearmPosition;

        float target =
                isMechanicalProcessActive()
                        ? getRecordingTonearmPosition()
                        : ARM_REST_POSITION;

        tonearmPosition = approach(
                tonearmPosition,
                target,
                ARM_SPEED
        );
    }

    private float getRecordingTonearmPosition() {
        float progress = getRecordingProgress();

        return ARM_START_POSITION
                + (ARM_END_POSITION - ARM_START_POSITION) * progress;
    }

    private void resetCloneSession() {
        cloneStage = CloneStage.IDLE;
        cloneTicks = 0L;
        completedLight = false;
        clearCloneSnapshot();
    }

    private void clearCloneSnapshot() {
        cloneAlbumId = "";
        cloneResultAlbumId = "";
        cloneTrackDurations = "";
        cloneWallSize = 1;
    }

    private static float approach(
            float current,
            float target,
            float speed) {

        if (current < target) {
            return Math.min(current + speed, target);
        }

        if (current > target) {
            return Math.max(current - speed, target);
        }

        return target;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        ContainerHelper.saveAllItems(output, items);

        output.putBoolean("Recording", recording);
        output.putLong("RecordingTicks", recordingTicks);
        output.putString("PendingAlbumId", pendingAlbumId);
        output.putString("PendingTrackDurations", pendingTrackDurations);
        output.putBoolean("CompletedLight", completedLight);

        output.putString("CloneStage", cloneStage.name());
        output.putLong("CloneTicks", cloneTicks);
        output.putString("CloneAlbumId", cloneAlbumId);
        output.putString("CloneResultAlbumId", cloneResultAlbumId);
        output.putString("CloneTrackDurations", cloneTrackDurations);
        output.putInt("CloneWallSize", cloneWallSize);

        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        items.clear();
        ContainerHelper.loadAllItems(input, items);

        recording = input.getBooleanOr("Recording", false);
        recordingTicks = input.getLongOr("RecordingTicks", 0L);
        pendingAlbumId = input.getStringOr("PendingAlbumId", "");
        pendingTrackDurations = input.getStringOr(
                "PendingTrackDurations",
                ""
        );
        completedLight = input.getBooleanOr("CompletedLight", false);

        try {
            cloneStage = CloneStage.valueOf(
                    input.getStringOr("CloneStage", CloneStage.IDLE.name())
            );
        } catch (IllegalArgumentException ignored) {
            cloneStage = CloneStage.IDLE;
        }

        cloneTicks = input.getLongOr("CloneTicks", 0L);
        cloneAlbumId = input.getStringOr("CloneAlbumId", "");
        cloneResultAlbumId = input.getStringOr("CloneResultAlbumId", "");
        cloneTrackDurations = input.getStringOr("CloneTrackDurations", "");
        cloneWallSize = Math.max(
                1,
                Math.min(
                        10,
                        input.getIntOr("CloneWallSize", 1)
                )
        );

        if (!hasVinyl()) {
            recording = false;
            recordingTicks = 0L;
            pendingAlbumId = "";
            pendingTrackDurations = "";
            completedLight = false;

            /*
             * WAITING_BLANK es el único estado válido sin disco insertado.
             */
            if (cloneStage != CloneStage.WAITING_BLANK) {
                resetCloneSession();
            }
        }
    }

    @Override
    public void preRemoveSideEffects(
            BlockPos pos,
            BlockState state) {

        if (level != null && !level.isClientSide() && hasVinyl()) {
            recording = false;

            /*
             * Al romper la máquina durante cualquier proceso, el disco físico
             * actual se recupera y la sesión de clonación se descarta.
             */
            cloneStage = CloneStage.IDLE;
            clearCloneSnapshot();

            ItemStack dropped = items.get(0);
            items.set(0, ItemStack.EMPTY);

            if (!dropped.isEmpty()) {
                Block.popResource(level, pos, dropped);
            }
        }

        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        return saveWithoutMetadata(registryLookup);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setChanged() {
        super.setChanged();

        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();

            level.sendBlockUpdated(
                    worldPosition,
                    state,
                    state,
                    Block.UPDATE_CLIENTS
            );
        }
    }
}
