package com.radig.vinylcraft.player;

import net.minecraft.world.SimpleContainer;

/** Expone los slots reales de Becoya's Module almacenados en el jugador. */
public interface DiscmanEquipmentHolder {

    int HEADPHONES_SLOT = 0;
    int DISCMAN_SLOT = 1;
    int VINYL_SLOT = 2;
    int SIZE = 3;

    SimpleContainer vinylcraft$getDiscmanEquipment();
}
