package com.radig.vinylcraft.client.sound;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import net.minecraft.client.sounds.AudioStream;

public final class LocalAudioStreamFactory {

    private LocalAudioStreamFactory() {
    }

    public static AudioStream open(Path path)
            throws IOException {

        return open(path, 0.0D);
    }

    /**
     * Abre una pista local comenzando aproximadamente en startSeconds.
     *
     * Esto es necesario para que un vinilo que quedó en PAUSE conserve no
     * solamente sus ticks/HUD al volver a entrar al mundo, sino también la
     * posición real del audio.
     */
    public static AudioStream open(
            Path path,
            double startSeconds)
            throws IOException {

        String extension = extension(path);
        double safeStart = Math.max(0.0D, startSeconds);

        return switch (extension) {
            case "mp3" -> new Mp3MonoAudioStream(path, safeStart);
            case "wav" -> new WavMonoAudioStream(path, safeStart);
            default -> throw new IOException(
                    "Formato local no soportado: " + extension
            );
        };
    }

    public static boolean supports(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return false;
        }

        String extension = extension(path);
        return extension.equals("mp3") || extension.equals("wav");
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
