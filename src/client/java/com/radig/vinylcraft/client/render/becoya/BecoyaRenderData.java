package com.radig.vinylcraft.client.render.becoya;

import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

/** Datos extra que VinylCraft adjunta al AvatarRenderState del jugador. */
public final class BecoyaRenderData {

    public static final RenderStateDataKey<Boolean> HAS_DISCMAN =
            RenderStateDataKey.create(() -> "vinylcraft:has_discman");

    public static final RenderStateDataKey<Boolean> HAS_HEADPHONES =
            RenderStateDataKey.create(() -> "vinylcraft:has_headphones");

    public static final RenderStateDataKey<String> HEADPHONES_COLOR =
            RenderStateDataKey.create(() -> "vinylcraft:headphones_color");

    private BecoyaRenderData() {
    }
}
