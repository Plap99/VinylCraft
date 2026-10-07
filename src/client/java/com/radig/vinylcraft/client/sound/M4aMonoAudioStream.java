package com.radig.vinylcraft.client.sound;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioFormat;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.sounds.FloatSampleSource;

/**
 * M4A/AAC/ALAC opcional: FFmpeg decodifica a PCM 16-bit mono 44.1 kHz
 * directamente hacia OpenAL; no crea archivos temporales ni modifica el M4A.
 * Si FFmpeg no está instalado, VinylCraft lo deja gris en la biblioteca.
 */
public final class M4aMonoAudioStream implements FloatSampleSource {
    private static final AudioFormat FORMAT = new AudioFormat(44100.0F, 16, 1, true, false);
    private static volatile Boolean ffmpegAvailable;
    private static final int CHUNK_SIZE = 8192;

    private final Process process;
    private final InputStream source;
    private final byte[] buffer = new byte[CHUNK_SIZE];
    private boolean closed;

    public M4aMonoAudioStream(Path path, double startSeconds) throws IOException {
        if (!isAvailable() || !Files.isRegularFile(path)) {
            throw new IOException("FFmpeg no disponible para M4A: " + path);
        }
        List<String> args = new ArrayList<>();
        args.add(executable());
        args.add("-nostdin");
        args.add("-loglevel"); args.add("error");
        if (startSeconds > 0.0D) {
            args.add("-ss"); args.add(Double.toString(startSeconds));
        }
        args.add("-i"); args.add(path.toAbsolutePath().normalize().toString());
        args.add("-vn");
        args.add("-f"); args.add("s16le");
        args.add("-acodec"); args.add("pcm_s16le");
        args.add("-ac"); args.add("1");
        args.add("-ar"); args.add("44100");
        args.add("pipe:1");
        process = new ProcessBuilder(args).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        source = process.getInputStream();
    }

    public static boolean isAvailable() {
        Boolean previous = ffmpegAvailable;
        if (previous != null) return previous;
        synchronized (M4aMonoAudioStream.class) {
            if (ffmpegAvailable != null) return ffmpegAvailable;
            boolean available = false;
            Process probe = null;
            try {
                probe = new ProcessBuilder(executable(), "-version")
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
                available = probe.waitFor(3, TimeUnit.SECONDS) && probe.exitValue() == 0;
            } catch (Exception ignored) {
                if (ignored instanceof InterruptedException) Thread.currentThread().interrupt();
            } finally {
                if (probe != null && probe.isAlive()) probe.destroyForcibly();
            }
            ffmpegAvailable = available;
            return available;
        }
    }

    private static String executable() {
        return System.getProperty("vinylcraft.ffmpeg.path", "ffmpeg");
    }

    @Override public AudioFormat getFormat() { return FORMAT; }

    @Override public boolean readChunk(FloatConsumer consumer) throws IOException {
        if (closed) return false;
        int length = 0;
        while (length < buffer.length) {
            int n = source.read(buffer, length, buffer.length - length);
            if (n < 0) break;
            if (n == 0) break;
            length += n;
        }
        length -= length % 2;
        if (length == 0) return false;
        for (int i = 0; i < length; i += 2) {
            short pcm = (short) (((buffer[i + 1] & 255) << 8) | (buffer[i] & 255));
            consumer.accept(pcm / 32768.0F);
        }
        return true;
    }

    @Override public void close() throws IOException {
        if (closed) return;
        closed = true;
        try { source.close(); }
        finally {
            process.destroy();
            try {
                if (!process.waitFor(200, TimeUnit.MILLISECONDS)) process.destroyForcibly();
            } catch (InterruptedException interrupted) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }
}
