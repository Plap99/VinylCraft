package com.radig.vinylcraft.client.sound;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import net.minecraft.client.sounds.AudioStream;

/** Streams estéreo/no-posicionales para Becoya's Module. */
public final class HeadphoneAudioStreamFactory {

    private HeadphoneAudioStreamFactory() {
    }

    public static AudioStream open(Path path, double startSeconds) throws IOException {
        String extension = extension(path);
        double safeStart = Math.max(0.0D, startSeconds);

        return switch (extension) {
            case "mp3" -> new Mp3StereoAudioStream(path, safeStart);
            case "wav" -> new WavStereoAudioStream(path, safeStart);
            case "m4a" -> new M4aStereoAudioStream(path, safeStart);
            default -> throw new IOException(
                    "Formato de audífonos no soportado: " + extension
            );
        };
    }

    public static boolean supports(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return false;
        }

        return switch (extension(path)) {
            case "mp3", "wav" -> true;
            case "m4a" -> M4aMonoAudioStream.isAvailable();
            default -> false;
        };
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');

        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }

        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
