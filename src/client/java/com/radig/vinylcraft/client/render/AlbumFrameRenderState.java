package com.radig.vinylcraft.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

public final class AlbumFrameRenderState extends BlockEntityRenderState {
    public boolean origin;
    public int wallSize = 1;
    public Direction facing = Direction.NORTH;
    public Identifier coverTexture;
}
