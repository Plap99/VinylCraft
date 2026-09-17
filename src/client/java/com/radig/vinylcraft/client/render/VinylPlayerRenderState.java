package com.radig.vinylcraft.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

import net.minecraft.core.Direction;

public class VinylPlayerRenderState extends BlockEntityRenderState {

    public final ItemStackRenderState vinyl =
            new ItemStackRenderState();

    public boolean hasVinyl;

    public Direction facing = Direction.NORTH;
}