package com.radig.vinylcraft.client.library;

import java.nio.file.Path;

public record AudioMetadata(
        Path file,
        String title,
        String artist,
        String album,
        String albumArtist,
        int trackNumber,
        int discNumber,
        int year,
        long durationMillis,
        byte[] embeddedCover
) {

    public boolean hasTitle() {
        return title != null && !title.isBlank();
    }

    public boolean hasArtist() {
        return artist != null && !artist.isBlank();
    }

    public boolean hasAlbum() {
        return album != null && !album.isBlank();
    }

    public boolean hasAlbumArtist() {
        return albumArtist != null
                && !albumArtist.isBlank();
    }

    public boolean hasEmbeddedCover() {
        return embeddedCover != null
                && embeddedCover.length > 0;
    }

    public String formattedDuration() {

        long totalSeconds =
                durationMillis / 1000L;

        long minutes =
                totalSeconds / 60L;

        long seconds =
                totalSeconds % 60L;

        return String.format(
                "%02d:%02d",
                minutes,
                seconds
        );
    }
}