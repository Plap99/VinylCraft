package com.radig.vinylcraft.client.music;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.radig.vinylcraft.VinylCraft;
import com.radig.vinylcraft.music.AlbumData;
import com.radig.vinylcraft.music.ModAlbums;
import com.radig.vinylcraft.music.TrackData;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Catálogo local persistente de los álbumes creados por el usuario.
 *
 * El ItemStack solamente guarda album_id y duraciones. Las rutas de los
 * archivos de audio y de la portada permanecen en el cliente, de modo que
 * no enviamos rutas locales del PC dentro del ItemStack ni al servidor.
 */
public final class RecordedAlbumStore {

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private static final int FORMAT_VERSION = 1;

    private RecordedAlbumStore() {
    }

    private static Path getFile() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("vinylcraft")
                .resolve("albums.json");
    }

    public static void load() {

        Path file = getFile();

        if (!Files.isRegularFile(file)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(
                file,
                StandardCharsets.UTF_8
        )) {
            JsonElement rootElement = JsonParser.parseReader(reader);

            if (!rootElement.isJsonObject()) {
                return;
            }

            JsonObject root = rootElement.getAsJsonObject();
            JsonArray albums = root.getAsJsonArray("albums");

            if (albums == null) {
                return;
            }

            int loaded = 0;

            for (JsonElement element : albums) {
                if (!element.isJsonObject()) {
                    continue;
                }

                AlbumData album = parseAlbum(element.getAsJsonObject());

                if (album == null || album.isEmpty()) {
                    continue;
                }

                ModAlbums.register(album);
                loaded++;
            }

            VinylCraft.LOGGER.info(
                    "Loaded {} recorded VinylCraft albums from {}",
                    loaded,
                    file
            );

        } catch (Exception exception) {
            VinylCraft.LOGGER.error(
                    "Could not load recorded VinylCraft albums from {}",
                    file,
                    exception
            );
        }
    }

    public static void registerAndSave(AlbumData album) {

        if (album == null || album.isEmpty()) {
            return;
        }

        ModAlbums.register(album);
        saveAll();
    }

    public static void saveAll() {

        Path file = getFile();

        try {
            Files.createDirectories(file.getParent());

            List<AlbumData> recordedAlbums =
                    ModAlbums.getAll()
                            .values()
                            .stream()
                            .filter(RecordedAlbumStore::isUserAlbum)
                            .sorted(Comparator.comparing(AlbumData::id))
                            .toList();

            JsonObject root = new JsonObject();
            root.addProperty("version", FORMAT_VERSION);

            JsonArray albums = new JsonArray();

            for (AlbumData album : recordedAlbums) {
                albums.add(serializeAlbum(album));
            }

            root.add("albums", albums);

            try (Writer writer = Files.newBufferedWriter(
                    file,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )) {
                GSON.toJson(root, writer);
            }

        } catch (IOException exception) {
            VinylCraft.LOGGER.error(
                    "Could not save recorded VinylCraft albums to {}",
                    file,
                    exception
            );
        }
    }

    private static boolean isUserAlbum(AlbumData album) {
        return album != null
                && album.id() != null
                && !album.id().isBlank()
                && !"test_album".equals(album.id());
    }

    private static JsonObject serializeAlbum(AlbumData album) {

        JsonObject object = new JsonObject();
        object.addProperty("id", album.id());
        object.addProperty("title", album.title());
        object.addProperty("artist", album.artist());
        object.addProperty("coverFile", album.coverFile());

        JsonArray tracks = new JsonArray();

        for (TrackData track : album.tracks()) {
            JsonObject trackObject = new JsonObject();
            trackObject.addProperty("id", track.id());
            trackObject.addProperty("title", track.title());
            trackObject.addProperty("audioFile", track.audioFile());
            trackObject.addProperty("durationMillis", track.durationMillis());
            tracks.add(trackObject);
        }

        object.add("tracks", tracks);
        return object;
    }

    private static AlbumData parseAlbum(JsonObject object) {

        String id = getString(object, "id");
        String title = getString(object, "title");
        String artist = getString(object, "artist");
        String coverFile = getString(object, "coverFile");

        if (id.isBlank()) {
            return null;
        }

        JsonArray tracksArray = object.getAsJsonArray("tracks");
        List<TrackData> tracks = new ArrayList<>();

        if (tracksArray != null) {
            for (JsonElement element : tracksArray) {
                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject trackObject = element.getAsJsonObject();

                String trackId = getString(trackObject, "id");
                String trackTitle = getString(trackObject, "title");
                String audioFile = getString(trackObject, "audioFile");
                long durationMillis = getLong(trackObject, "durationMillis");

                if (audioFile.isBlank()) {
                    continue;
                }

                tracks.add(
                        new TrackData(
                                trackId,
                                trackTitle,
                                audioFile,
                                Math.max(0L, durationMillis)
                        )
                );
            }
        }

        return new AlbumData(
                id,
                title,
                artist,
                coverFile,
                tracks
        );
    }

    private static String getString(JsonObject object, String key) {
        try {
            JsonElement value = object.get(key);
            return value == null || value.isJsonNull()
                    ? ""
                    : value.getAsString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static long getLong(JsonObject object, String key) {
        try {
            JsonElement value = object.get(key);
            return value == null || value.isJsonNull()
                    ? 0L
                    : value.getAsLong();
        } catch (Exception ignored) {
            return 0L;
        }
    }
}
