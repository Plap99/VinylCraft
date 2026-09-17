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

public class VinylPlayerBlockEntity extends BlockEntity {

    private final NonNullList<ItemStack> items =
            NonNullList.withSize(1, ItemStack.EMPTY);

    public VinylPlayerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VINYL_PLAYER, pos, state);
    }

    public boolean hasVinyl() {
        return !items.get(0).isEmpty();
    }

    public ItemStack getVinyl() {
        return items.get(0);
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

        setChanged();

        return removed;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        ContainerHelper.saveAllItems(output, items);
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
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
}