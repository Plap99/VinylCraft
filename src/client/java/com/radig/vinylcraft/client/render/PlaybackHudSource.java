package com.radig.vinylcraft.client.render;

import com.radig.vinylcraft.music.AlbumData;

/** Fuente individual que puede ocupar una tarjeta del HUD de música. */
public record PlaybackHudSource(
        SourceType type,
        AlbumData album,
        long playbackTicks,
        double distanceSquared) {

    public enum SourceType {
        DISCMAN,
        VINYL_PLAYER
    }

    public boolean isDiscman() {
        return type == SourceType.DISCMAN;
    }
}
