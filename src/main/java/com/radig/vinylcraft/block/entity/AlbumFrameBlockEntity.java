package com.radig.vinylcraft.block.entity;

import com.radig.vinylcraft.block.AlbumFrameBlock;
import com.radig.vinylcraft.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class AlbumFrameBlockEntity extends BlockEntity {

    private String albumId = "";
    private int wallSize = 1;
    private int tileX;
    private int tileY;
    private BlockPos originPos = BlockPos.ZERO;
    private ItemStack storedVinyl = ItemStack.EMPTY;

    private boolean suppressCleanup;

    public AlbumFrameBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALBUM_FRAME, pos, state);
    }

    public void setAlbumData(
            String albumId,
            int wallSize,
            int tileX,
            int tileY,
            BlockPos originPos,
            ItemStack storedVinyl) {

        this.albumId = albumId == null ? "" : albumId;
        this.wallSize = Math.max(1, Math.min(10, wallSize));
        this.tileX = Math.max(0, tileX);
        this.tileY = Math.max(0, tileY);
        this.originPos = originPos == null ? worldPosition : originPos.immutable();
        this.storedVinyl = storedVinyl == null || storedVinyl.isEmpty()
                ? ItemStack.EMPTY
                : storedVinyl.copyWithCount(1);
        setChanged();
    }

    public String getAlbumId() {
        return albumId;
    }

    public int getWallSize() {
        return wallSize;
    }

    public int getTileX() {
        return tileX;
    }

    public int getTileY() {
        return tileY;
    }

    public BlockPos getOriginPos() {
        return originPos;
    }

    public ItemStack getStoredVinyl() {
        return storedVinyl;
    }

    private ItemStack takeStoredVinyl() {
        if (storedVinyl == null || storedVinyl.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = storedVinyl.copyWithCount(1);
        storedVinyl = ItemStack.EMPTY;
        setChanged();
        return result;
    }

    public boolean isOrigin() {
        return tileX == 0 && tileY == 0 && worldPosition.equals(originPos);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("AlbumId", albumId);
        output.putInt("WallSize", wallSize);
        output.putInt("TileX", tileX);
        output.putInt("TileY", tileY);
        output.putLong("OriginPos", originPos.asLong());
        output.store("StoredVinyl", ItemStack.OPTIONAL_CODEC, storedVinyl);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        albumId = input.getStringOr("AlbumId", "");
        wallSize = Math.max(1, Math.min(10, input.getIntOr("WallSize", 1)));
        tileX = Math.max(0, input.getIntOr("TileX", 0));
        tileY = Math.max(0, input.getIntOr("TileY", 0));
        originPos = BlockPos.of(input.getLongOr("OriginPos", worldPosition.asLong()));
        storedVinyl = input.read("StoredVinyl", ItemStack.OPTIONAL_CODEC)
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (!suppressCleanup && level != null && !level.isClientSide()) {
            removeWholeMural();
        }

        super.preRemoveSideEffects(pos, state);
    }

    private void removeWholeMural() {
        if (level == null) {
            return;
        }

        BlockPos origin = originPos == null ? worldPosition : originPos;
        Direction facing = getBlockState().hasProperty(AlbumFrameBlock.FACING)
                ? getBlockState().getValue(AlbumFrameBlock.FACING)
                : Direction.NORTH;
        Direction right = facing.getCounterClockWise();
        int size = Math.max(1, Math.min(10, wallSize));

        ItemStack vinylToDrop = ItemStack.EMPTY;
        if (level.getBlockEntity(origin) instanceof AlbumFrameBlockEntity originFrame) {
            vinylToDrop = originFrame.takeStoredVinyl();
        }

        // Primero desactivamos la limpieza recursiva de todas las piezas.
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BlockPos tile = origin.above(y).relative(right, x);
                if (level.getBlockEntity(tile) instanceof AlbumFrameBlockEntity frame) {
                    frame.suppressCleanup = true;
                }
            }
        }

        // Luego retiramos el mural completo.
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BlockPos tile = origin.above(y).relative(right, x);
                if (level.getBlockState(tile).getBlock() == ModBlocks.ALBUM_FRAME) {
                    level.removeBlock(tile, false);
                }
            }
        }

        if (!vinylToDrop.isEmpty()) {
            Block.popResource(level, origin, vinylToDrop);
        }
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
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
