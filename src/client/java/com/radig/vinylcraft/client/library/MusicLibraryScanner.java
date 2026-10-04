package com.radig.vinylcraft.client.library;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class MusicLibraryScanner {

    private MusicLibraryScanner() {
    }

    /*
     * Formatos que reconoceremos inicialmente.
     *
     * Que VinylCraft detecte un formato no significa todavía
     * que pueda reproducirlo directamente. La conversión de audio
     * vendrá después.
     */
    private static final List<String> AUDIO_EXTENSIONS =
            List.of(
                    "mp3",
                    "ogg",
                    "wav",
                    "flac",
                    "m4a",
                    "aac"
            );

    private static final List<String> IMAGE_EXTENSIONS =
            List.of(
                    "png",
                    "jpg",
                    "jpeg",
                    "webp"
            );


    public static MusicLibraryEntry scan(Path root) {

        if (
                root == null
                || !Files.exists(root)
                || !Files.isDirectory(root)
        ) {
            return null;
        }

        return scanDirectory(root);
    }


    private static MusicLibraryEntry scanDirectory(Path directory) {

        List<MusicLibraryEntry> children =
                new ArrayList<>();

        try (var stream = Files.list(directory)) {

            stream.forEach(path -> {

                try {

                    if (Files.isDirectory(path)) {

                        MusicLibraryEntry folder =
                                scanDirectory(path);

                        if (folder != null) {
                            children.add(folder);
                        }

                        return;
                    }

                    if (!Files.isRegularFile(path)) {
                        return;
                    }

                    MusicLibraryEntry.Type type =
                            detectType(path);

                    /*
                     * Ignoramos archivos que no nos interesan.
                     *
                     * Por ejemplo:
                     * .txt
                     * .pdf
                     * .cue
                     * .nfo
                     * etc.
                     */
                    if (type == null) {
                        return;
                    }

                    children.add(
                            new MusicLibraryEntry(
                                    getName(path),
                                    path,
                                    type,
                                    List.of()
                            )
                    );

                } catch (Exception exception) {

                    System.err.println(
                            "[VinylCraft] No se pudo analizar: "
                                    + path
                    );

                }
            });

        } catch (IOException exception) {

            System.err.println(
                    "[VinylCraft] No se pudo abrir la carpeta: "
                            + directory
            );
        }

        /*
         * Orden:
         *
         * 1. Carpetas
         * 2. Archivos
         *
         * Dentro de cada grupo, orden alfabético.
         */
        children.sort(
                Comparator
                        .comparing(
                                (MusicLibraryEntry entry) ->
                                        !entry.isFolder()
                        )
                        .thenComparing(
                                MusicLibraryEntry::name,
                                MusicLibraryScanner::compareNatural
                        )
        );

        return new MusicLibraryEntry(
                getName(directory),
                directory,
                MusicLibraryEntry.Type.FOLDER,
                children
        );
    }


    private static MusicLibraryEntry.Type detectType(
            Path path) {

        String extension =
                getExtension(path);

        if (AUDIO_EXTENSIONS.contains(extension)) {
            return MusicLibraryEntry.Type.AUDIO_FILE;
        }

        if (IMAGE_EXTENSIONS.contains(extension)) {
            return MusicLibraryEntry.Type.IMAGE_FILE;
        }

        return null;
    }


    private static String getExtension(Path path) {

        String name =
                getName(path);

        int dot =
                name.lastIndexOf('.');

        if (
                dot < 0
                || dot == name.length() - 1
        ) {
            return "";
        }

        return name
                .substring(dot + 1)
                .toLowerCase(Locale.ROOT);
    }


    private static String getName(Path path) {

        Path fileName =
                path.getFileName();

        if (fileName != null) {
            return fileName.toString();
        }

        return path.toString();
    }

    private static int compareNatural(
            String first,
            String second) {

        int firstIndex = 0;
        int secondIndex = 0;

        while (
                firstIndex < first.length()
                && secondIndex < second.length()
        ) {

            char firstChar =
                    first.charAt(firstIndex);

            char secondChar =
                    second.charAt(secondIndex);

            /*
            * Si ambos caracteres empiezan un número,
            * comparamos el número completo.
            *
            * Ejemplo:
            * 2.mp3 < 10.mp3
            */
            if (
                    Character.isDigit(firstChar)
                    && Character.isDigit(secondChar)
            ) {

                int firstStart = firstIndex;
                int secondStart = secondIndex;

                while (
                        firstIndex < first.length()
                        && Character.isDigit(
                                first.charAt(firstIndex)
                        )
                ) {
                    firstIndex++;
                }

                while (
                        secondIndex < second.length()
                        && Character.isDigit(
                                second.charAt(secondIndex)
                        )
                ) {
                    secondIndex++;
                }

                String firstNumber =
                        first.substring(
                                firstStart,
                                firstIndex
                        );

                String secondNumber =
                        second.substring(
                                secondStart,
                                secondIndex
                        );

                try {

                    long firstValue =
                            Long.parseLong(firstNumber);

                    long secondValue =
                            Long.parseLong(secondNumber);

                    int comparison =
                            Long.compare(
                                    firstValue,
                                    secondValue
                            );

                    if (comparison != 0) {
                        return comparison;
                    }

                } catch (NumberFormatException ignored) {
                    // Si fuera un número absurdamente grande,
                    // continuamos comparándolo como texto.
                }

                continue;
            }

            /*
            * Comparación normal ignorando
            * mayúsculas/minúsculas.
            */
            firstChar =
                    Character.toLowerCase(firstChar);

            secondChar =
                    Character.toLowerCase(secondChar);

            if (firstChar != secondChar) {
                return Character.compare(
                        firstChar,
                        secondChar
                );
            }

            firstIndex++;
            secondIndex++;
        }

        return Integer.compare(
                first.length(),
                second.length()
        );
    }
}