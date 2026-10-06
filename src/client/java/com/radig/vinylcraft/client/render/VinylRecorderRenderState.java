package com.radig.vinylcraft.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

public class VinylRecorderRenderState
        extends BlockEntityRenderState {

    public final ItemStackRenderState vinyl =
            new ItemStackRenderState();

    public Direction facing = Direction.NORTH;

    public boolean recording;
    public boolean hasVinyl;

    public float discRotation;
    public float tonearmPosition;
    public float recPulse = 1.0F;
}
