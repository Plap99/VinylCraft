package com.radig.vinylcraft.client.library;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class AlbumCoverResolver {

    private AlbumCoverResolver() {
    }

    public enum SourceType {
        EMBEDDED,
        FILE
    }

    public record CoverResult(
            SourceType sourceType,
            Path path,
            byte[] data) {

        public boolean isEmbedded() {
            return sourceType == SourceType.EMBEDDED;
        }

        public boolean isFile() {
            return sourceType == SourceType.FILE;
        }
    }

    public static CoverResult resolve(
            Path audioFile,
            AudioMetadata metadata) {

        /*
         * 1. Portada embebida en el archivo de audio.
         */

        if (
                metadata != null
                        && metadata.embeddedCover() != null
                        && metadata.embeddedCover().length > 0
        ) {

            return new CoverResult(
                    SourceType.EMBEDDED,
                    null,
                    metadata.embeddedCover()
            );
        }

        /*
         * 2. Buscar imágenes en la carpeta del audio.
         */

        if (audioFile == null) {
            return null;
        }

        Path folder =
                audioFile
                        .toAbsolutePath()
                        .normalize()
                        .getParent();

        if (
                folder == null
                        || !Files.isDirectory(folder)
        ) {
            return null;
        }

        try (
                Stream<Path> files =
                        Files.list(folder)
        ) {

            List<Path> images =
                    files
                            .filter(Files::isRegularFile)
                            .filter(
                                    AlbumCoverResolver::isImageFile
                            )
                            .sorted(
                                    Comparator
                                            .comparingInt(
                                                    AlbumCoverResolver
                                                            ::coverPriority
                                            )
                                            .reversed()
                                            .thenComparing(
                                                    path ->
                                                            path
                                                                    .getFileName()
                                                                    .toString()
                                                                    .toLowerCase(
                                                                            Locale.ROOT
                                                                    )
                                            )
                            )
                            .toList();

            if (images.isEmpty()) {
                return null;
            }

            return new CoverResult(
                    SourceType.FILE,
                    images.get(0),
                    null
            );

        } catch (IOException exception) {

            System.err.println(
                    "[VinylCraft] No se pudieron buscar portadas en: "
                            + folder
            );

            return null;
        }
    }

    private static boolean isImageFile(
            Path path) {

        if (
                path == null
                        || path.getFileName() == null
        ) {
            return false;
        }

        String name =
                path
                        .getFileName()
                        .toString()
                        .toLowerCase(
                                Locale.ROOT
                        );

        return name.endsWith(".png")
                || name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".webp");
    }

    private static int coverPriority(
            Path path) {

        String name =
                path
                        .getFileName()
                        .toString()
                        .toLowerCase(
                                Locale.ROOT
                        );

        /*
         * Prioridad deliberada:
         *
         * front > cover > folder >
         * AlbumArt Large > AlbumArt >
         * cualquier otra imagen.
         */

        if (
                name.equals("front.jpg")
                        || name.equals("front.jpeg")
                        || name.equals("front.png")
                        || name.equals("front.webp")
        ) {
            return 1000;
        }

        if (name.contains("front")) {
            return 950;
        }

        if (
                name.equals("cover.jpg")
                        || name.equals("cover.jpeg")
                        || name.equals("cover.png")
                        || name.equals("cover.webp")
        ) {
            return 900;
        }

        if (name.contains("cover")) {
            return 850;
        }

        if (
                name.equals("folder.jpg")
                        || name.equals("folder.jpeg")
                        || name.equals("folder.png")
                        || name.equals("folder.webp")
        ) {
            return 800;
        }

        if (
                name.contains("albumart")
                        && name.contains("large")
        ) {
            return 700;
        }

        if (name.contains("albumart")) {
            return 650;
        }

        /*
         * Evitamos escoger "back" o "cd"
         * antes que una imagen genérica útil.
         */

        if (name.contains("back")) {
            return 50;
        }

        if (
                name.contains("cd")
                        || name.contains("disc")
        ) {
            return 40;
        }

        return 100;
    }
}