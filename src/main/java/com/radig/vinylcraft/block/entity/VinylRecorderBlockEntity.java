package com.radig.vinylcraft.block.entity;

import com.radig.vinylcraft.item.VinylData;
import com.radig.vinylcraft.item.ModItems;

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

    private static final float ARM_REST_POSITION = 0.69F;
    private static final float ARM_START_POSITION = 1.93F;
    private static final float ARM_END_POSITION = 2.52F;
    private static final float ARM_SPEED = 0.035F;

    private final NonNullList<ItemStack> items =
            NonNullList.withSize(1, ItemStack.EMPTY);

    private boolean recording;
    private long recordingTicks;
    private String pendingAlbumId = "";
    private String pendingTrackDurations = "";

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

    public boolean insertVinyl(ItemStack stack) {

        if (
                stack == null
                        || stack.isEmpty()
                        || hasVinyl()
                        || !stack.is(ModItems.BLANK_VINYL)
        ) {
            return false;
        }

        items.set(0, stack.copyWithCount(1));
        recording = false;
        recordingTicks = 0L;
        pendingAlbumId = "";
        pendingTrackDurations = "";
        setChanged();
        return true;
    }

    public ItemStack removeVinyl() {

        if (!hasVinyl() || recording) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = items.get(0);
        items.set(0, ItemStack.EMPTY);

        recordingTicks = 0L;
        pendingAlbumId = "";
        pendingTrackDurations = "";
        setChanged();

        return removed;
    }

    public boolean isRecording() {
        return recording;
    }

    public long getRecordingTicks() {
        return recordingTicks;
    }

    public float getRecordingProgress() {
        return Math.min(
                1.0F,
                recordingTicks / (float) RECORDING_DURATION_TICKS
        );
    }

    public boolean startRecording(
            String albumId,
            String trackDurations) {

        if (
                recording
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
            blockEntity.tickServerRecording();
        }

        if (level.isClientSide()) {
            if (blockEntity.recording) {
                blockEntity.recordingTicks = Math.min(
                        RECORDING_DURATION_TICKS,
                        blockEntity.recordingTicks + 1L
                );
            }

            blockEntity.tickTonearmAnimation();
        }
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
        setChanged();
    }

    private void tickTonearmAnimation() {
        previousTonearmPosition = tonearmPosition;

        float target =
                recording
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

        if (!hasVinyl()) {
            recording = false;
            recordingTicks = 0L;
            pendingAlbumId = "";
            pendingTrackDurations = "";
        }
    }

    @Override
    public void preRemoveSideEffects(
            BlockPos pos,
            BlockState state) {

        if (level != null && !level.isClientSide() && hasVinyl()) {
            recording = false;
            Block.popResource(level, pos, removeVinyl());
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
