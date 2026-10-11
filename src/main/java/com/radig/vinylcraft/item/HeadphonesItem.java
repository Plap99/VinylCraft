package com.radig.vinylcraft.item;

import net.minecraft.world.item.Item;

/** Audífonos independientes de Becoya's Module. */
public final class HeadphonesItem extends Item {

    private final String colorName;
    private final int colorRgb;

    public HeadphonesItem(Properties properties, String colorName, int colorRgb) {
        super(properties);
        this.colorName = colorName;
        this.colorRgb = colorRgb;
    }

    public String colorName() {
        return colorName;
    }

    public int colorRgb() {
        return colorRgb;
    }
}
