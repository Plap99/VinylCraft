package com.radig.vinylcraft.music;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModAlbums {

    /*
     * =====================================================
     * ÁLBUM DE PRUEBA
     * =====================================================
     *
     * Por ahora representa nuestro audio actual.
     *
     * Más adelante estos datos NO estarán escritos aquí:
     * se cargarán desde la biblioteca local de VinylCraft.
     */

    public static final AlbumData TEST_ALBUM =
            new AlbumData(
                    "test_album",
                    "Test Album",
                    "VinylCraft",
                    "",
                    List.of(
                            new TrackData(
                                    "get_your_shine_on",
                                    "Get Your Shine On",
                                    "get_your_shine_on",
                                    20_000L
                            )
                    )
            );


    /*
     * =====================================================
     * REGISTRO TEMPORAL DE ÁLBUMES
     * =====================================================
     */

    private static final Map<String, AlbumData> ALBUMS =
            new LinkedHashMap<>();


    static {

        register(TEST_ALBUM);
    }


    private ModAlbums() {
    }


    private static void register(
            AlbumData album) {

        if (album == null) {
            return;
        }

        ALBUMS.put(
                album.id(),
                album
        );
    }


    public static AlbumData get(
            String albumId) {

        if (
                albumId == null
                || albumId.isBlank()
        ) {
            return null;
        }

        return ALBUMS.get(albumId);
    }


    public static boolean contains(
            String albumId) {

        return get(albumId) != null;
    }


    public static Map<String, AlbumData> getAll() {

        return Map.copyOf(ALBUMS);
    }
}