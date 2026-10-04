package com.radig.vinylcraft.client.library;

import java.nio.file.Path;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

public final class AudioMetadataReader {

    private AudioMetadataReader() {
    }

    public static AudioMetadata read(Path path) {

        if (path == null) {
            return null;
        }

        try {

            AudioFile audioFile =
                    AudioFileIO.read(
                            path.toFile()
                    );

            Tag tag =
                    audioFile.getTag();

            String title =
                    getValue(
                            tag,
                            FieldKey.TITLE
                    );

            String artist =
                    getValue(
                            tag,
                            FieldKey.ARTIST
                    );

            String album =
                    getValue(
                            tag,
                            FieldKey.ALBUM
                    );

            String albumArtist =
                    getValue(
                            tag,
                            FieldKey.ALBUM_ARTIST
                    );

            int trackNumber =
                    parseNumber(
                            getValue(
                                    tag,
                                    FieldKey.TRACK
                            )
                    );

            int discNumber =
                    parseNumber(
                            getValue(
                                    tag,
                                    FieldKey.DISC_NO
                            )
                    );

            int year =
                    parseNumber(
                            getValue(
                                    tag,
                                    FieldKey.YEAR
                            )
                    );

            long durationMillis =
                    Math.round(
                            audioFile
                                    .getAudioHeader()
                                    .getPreciseTrackLength()
                                    * 1000.0D
                    );

            byte[] embeddedCover =
                    getEmbeddedCover(tag);

            return new AudioMetadata(
                    path,
                    title,
                    artist,
                    album,
                    albumArtist,
                    trackNumber,
                    discNumber,
                    year,
                    durationMillis,
                    embeddedCover
            );

        } catch (Exception exception) {

            System.err.println(
                    "[VinylCraft] No se pudieron leer "
                            + "los metadatos de: "
                            + path
            );

            exception.printStackTrace();

            return null;
        }
    }


    private static String getValue(
            Tag tag,
            FieldKey fieldKey) {

        if (tag == null) {
            return "";
        }

        try {

            String value =
                    tag.getFirst(fieldKey);

            return value == null
                    ? ""
                    : value.trim();

        } catch (Exception exception) {

            return "";
        }
    }


    private static int parseNumber(
            String value) {

        if (
                value == null
                || value.isBlank()
        ) {
            return 0;
        }

        /*
         * Algunos tags utilizan:
         *
         * 3
         * 03
         * 3/12
         *
         * Nosotros queremos únicamente el 3.
         */
        String number =
                value.trim();

        int slash =
                number.indexOf('/');

        if (slash >= 0) {
            number =
                    number.substring(
                            0,
                            slash
                    );
        }

        try {

            return Integer.parseInt(
                    number.trim()
            );

        } catch (NumberFormatException exception) {

            return 0;
        }
    }


    private static byte[] getEmbeddedCover(
            Tag tag) {

        if (tag == null) {
            return null;
        }

        try {

            Artwork artwork =
                    tag.getFirstArtwork();

            if (artwork == null) {
                return null;
            }

            byte[] data =
                    artwork.getBinaryData();

            if (
                    data == null
                    || data.length == 0
            ) {
                return null;
            }

            return data;

        } catch (Exception exception) {

            return null;
        }
    }
}