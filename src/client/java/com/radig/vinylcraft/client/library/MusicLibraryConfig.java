package com.radig.vinylcraft.client.library;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

public final class MusicLibraryConfig {

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private static final Path CONFIG_DIRECTORY =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("vinylcraft");

    private static final Path CONFIG_FILE =
            CONFIG_DIRECTORY.resolve(
                    "library.json"
            );


    private MusicLibraryConfig() {
    }


    public static void load() {

        if (!Files.exists(CONFIG_FILE)) {
            return;
        }

        try (
                Reader reader =
                        Files.newBufferedReader(
                                CONFIG_FILE
                        )
        ) {

            ConfigData data =
                    GSON.fromJson(
                            reader,
                            ConfigData.class
                    );

            if (
                    data == null
                    || data.folders == null
            ) {
                return;
            }

            for (String folder : data.folders) {

                if (
                        folder == null
                        || folder.isBlank()
                ) {
                    continue;
                }

                MusicLibraryManager.addRootFolderWithoutRefresh(
                        Path.of(folder)
                );
            }

            MusicLibraryManager.refresh();

        } catch (Exception exception) {

            System.err.println(
                    "[VinylCraft] No se pudo cargar "
                            + "library.json"
            );

            exception.printStackTrace();
        }
    }


    public static void save() {

        try {

            Files.createDirectories(
                    CONFIG_DIRECTORY
            );

            List<String> folders =
                    MusicLibraryManager
                            .getRootFolders()
                            .stream()
                            .map(Path::toString)
                            .toList();

            ConfigData data =
                    new ConfigData();

            data.folders =
                    new ArrayList<>(folders);

            try (
                    Writer writer =
                            Files.newBufferedWriter(
                                    CONFIG_FILE
                            )
            ) {

                GSON.toJson(
                        data,
                        writer
                );
            }

        } catch (IOException exception) {

            System.err.println(
                    "[VinylCraft] No se pudo guardar "
                            + "library.json"
            );

            exception.printStackTrace();
        }
    }


    public static Path getConfigFile() {
        return CONFIG_FILE;
    }


    private static final class ConfigData {

        private List<String> folders =
                new ArrayList<>();
    }
}