package com.radig.vinylcraft.client.library;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;

public final class AudioMetadataCache {

    private static final Map<Path, CacheEntry> CACHE =
            new HashMap<>();

    private AudioMetadataCache() {
    }


    public static AudioMetadata get(Path path) {

        if (
                path == null
                || !Files.isRegularFile(path)
        ) {
            return null;
        }

        Path normalizedPath =
                path.toAbsolutePath().normalize();

        try {

            long size =
                    Files.size(normalizedPath);

            FileTime modifiedTime =
                    Files.getLastModifiedTime(
                            normalizedPath
                    );

            CacheEntry cached =
                    CACHE.get(normalizedPath);

            /*
             * Si el archivo no ha cambiado desde
             * la última lectura, usamos el metadata
             * que ya tenemos.
             */
            if (
                    cached != null
                    && cached.fileSize() == size
                    && cached.modifiedTime()
                            .equals(modifiedTime)
            ) {
                return cached.metadata();
            }

            /*
             * Archivo nuevo o modificado:
             * volvemos a leer sus tags.
             */
            AudioMetadata metadata =
                    AudioMetadataReader.read(
                            normalizedPath
                    );

            if (metadata == null) {
                CACHE.remove(normalizedPath);
                return null;
            }

            CACHE.put(
                    normalizedPath,
                    new CacheEntry(
                            metadata,
                            size,
                            modifiedTime
                    )
            );

            return metadata;

        } catch (IOException exception) {

            CACHE.remove(normalizedPath);

            System.err.println(
                    "[VinylCraft] No se pudo revisar "
                            + "el archivo: "
                            + normalizedPath
            );

            return null;
        }
    }


    public static void invalidate(Path path) {

        if (path == null) {
            return;
        }

        CACHE.remove(
                path.toAbsolutePath().normalize()
        );
    }


    public static void clear() {
        CACHE.clear();
    }


    public static int size() {
        return CACHE.size();
    }


    private record CacheEntry(
            AudioMetadata metadata,
            long fileSize,
            FileTime modifiedTime
    ) {
    }
}