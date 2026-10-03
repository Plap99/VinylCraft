package com.radig.vinylcraft.music;

public record TrackData(
        String id,
        String title,
        String audioFile,
        long durationMillis
) {

    public long durationTicks() {
        return Math.max(
                1L,
                Math.round(durationMillis / 50.0D)
        );
    }
}