package com.radig.vinylcraft.music;

import java.util.List;

public record AlbumData(
        String id,
        String title,
        String artist,
        String coverFile,
        List<TrackData> tracks
) {

    public AlbumData {
        tracks = List.copyOf(tracks);
    }

    public int trackCount() {
        return tracks.size();
    }

    public boolean isEmpty() {
        return tracks.isEmpty();
    }

    public TrackData getTrack(int index) {

        if (
                index < 0
                || index >= tracks.size()
        ) {
            return null;
        }

        return tracks.get(index);
    }

    public long totalDurationMillis() {

        long total = 0L;

        for (TrackData track : tracks) {
            total += track.durationMillis();
        }

        return total;
    }

    public long totalDurationTicks() {

        long total = 0L;

        for (TrackData track : tracks) {
            total += track.durationTicks();
        }

        return total;
    }
}