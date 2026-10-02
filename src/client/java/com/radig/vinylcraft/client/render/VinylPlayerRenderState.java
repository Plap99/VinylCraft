package com.radig.vinylcraft.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

import net.minecraft.core.Direction;

public class VinylPlayerRenderState
        extends BlockEntityRenderState {

    public final ItemStackRenderState vinyl =
            new ItemStackRenderState();

    public boolean hasVinyl;

    public boolean playing;
    public boolean paused;

    public Direction facing =
            Direction.NORTH;

    public float vinylRotation;


    /*
     * Posición horizontal del brazo.
     *
     * 0.00 = estacionado
     * 1.00 = borde exterior del vinilo
     * ~1.28 = zona interior del vinilo
     */
    public float tonearmPosition =
            0.0F;


    /*
     * Elevación de la aguja.
     *
     * 0.00 = abajo
     * 1.00 = arriba
     */
    public float tonearmLift =
            1.0F;
}