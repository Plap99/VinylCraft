package com.radig.vinylcraft.client.library;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MusicLibraryManager {

    private static final List<Path> ROOT_FOLDERS =
            new ArrayList<>();

    private static final List<MusicLibraryEntry> LIBRARIES =
            new ArrayList<>();


    private MusicLibraryManager() {
    }


    public static void addRootFolder(Path path) {

        if (path == null) {
            return;
        }

        Path normalized =
                path.toAbsolutePath().normalize();

        if (ROOT_FOLDERS.contains(normalized)) {
            return;
        }

        ROOT_FOLDERS.add(normalized);

        refresh();
    }

    public static void addRootFolderWithoutRefresh(
            Path path) {

        if (path == null) {
            return;
        }

        Path normalized =
                path.toAbsolutePath().normalize();

        if (ROOT_FOLDERS.contains(normalized)) {
            return;
        }

        ROOT_FOLDERS.add(normalized);
    }

    public static void removeRootFolder(Path path) {

        if (path == null) {
            return;
        }

        Path normalized =
                path.toAbsolutePath().normalize();

        ROOT_FOLDERS.remove(normalized);

        refresh();
    }


    public static void refresh() {

        LIBRARIES.clear();

        for (Path root : ROOT_FOLDERS) {

            MusicLibraryEntry library =
                    MusicLibraryScanner.scan(root);

            if (library != null) {
                LIBRARIES.add(library);
            }
        }
    }


    public static List<Path> getRootFolders() {

        return Collections.unmodifiableList(
                ROOT_FOLDERS
        );
    }


    public static List<MusicLibraryEntry> getLibraries() {

        return Collections.unmodifiableList(
                LIBRARIES
        );
    }


    public static boolean isEmpty() {
        return ROOT_FOLDERS.isEmpty();
    }


    public static int rootFolderCount() {
        return ROOT_FOLDERS.size();
    }


    public static void clear() {

        ROOT_FOLDERS.clear();
        LIBRARIES.clear();

        AudioMetadataCache.clear();
    }
}