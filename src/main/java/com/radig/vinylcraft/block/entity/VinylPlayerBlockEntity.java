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

public class VinylPlayerBlockEntity extends BlockEntity {

    private final NonNullList<ItemStack> items =
            NonNullList.withSize(1, ItemStack.EMPTY);

    private boolean playing = false;
    private long playbackTicks = 0;

    public VinylPlayerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VINYL_PLAYER, pos, state);
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

    public void setPlaying(boolean playing) {

        // No permitimos reproducir si no hay vinilo.
        if (playing && !hasVinyl()) {
            return;
        }

        if (this.playing == playing) {
            return;
        }

        this.playing = playing;
        setChanged();
    }

    public boolean insertVinyl(ItemStack stack) {
        if (hasVinyl()) {
            return false;
        }

        items.set(0, stack.copyWithCount(1));
        setChanged();

        return true;
    }

    public ItemStack removeVinyl() {
        if (!hasVinyl()) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = items.get(0);

        items.set(0, ItemStack.EMPTY);

        // Si quitamos el disco, el reproductor se detiene.
        playing = false;

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

    public static void tick(
        Level level,
        BlockPos pos,
        BlockState state,
        VinylPlayerBlockEntity blockEntity) {

    if (blockEntity.playing && blockEntity.hasVinyl()) {
        blockEntity.playbackTicks++;
    }
}
    @Override
    protected void saveAdditional(ValueOutput output) {
        ContainerHelper.saveAllItems(output, items);

        output.putBoolean("Playing", playing);
        output.putLong("PlaybackTicks", playbackTicks);

        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        // Limpiamos primero el inventario actual para evitar
        // que el cliente conserve un vinilo que ya fue retirado.
        items.clear();

        ContainerHelper.loadAllItems(input, items);

        playing = input.getBooleanOr("Playing", false);
        playbackTicks = input.getLongOr("PlaybackTicks", 0L);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {

        if (level != null && !level.isClientSide() && hasVinyl()) {
            Block.popResource(
                    level,
                    pos,
                    removeVinyl()
            );
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