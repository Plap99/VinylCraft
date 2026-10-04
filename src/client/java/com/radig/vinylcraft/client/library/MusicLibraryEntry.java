package com.radig.vinylcraft.client.library;

import java.nio.file.Path;
import java.util.List;

public record MusicLibraryEntry(
        String name,
        Path path,
        Type type,
        List<MusicLibraryEntry> children
) {

    public enum Type {
        FOLDER,
        AUDIO_FILE,
        IMAGE_FILE
    }

    public MusicLibraryEntry {
        children = children == null
                ? List.of()
                : List.copyOf(children);
    }

    public boolean isFolder() {
        return type == Type.FOLDER;
    }

    public boolean isAudioFile() {
        return type == Type.AUDIO_FILE;
    }

    public boolean isImageFile() {
        return type == Type.IMAGE_FILE;
    }
}