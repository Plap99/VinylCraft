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

/** M4A/AAC/ALAC -> PCM estéreo usando FFmpeg. */
public final class M4aStereoAudioStream implements FloatSampleSource {

    private static final AudioFormat FORMAT =
            new AudioFormat(44100.0F, 16, 2, true, false);

    private static final int CHUNK_SIZE = 8192;
    private final Process process;
    private final InputStream source;
    private final byte[] buffer = new byte[CHUNK_SIZE];
    private boolean closed;

    public M4aStereoAudioStream(Path path, double startSeconds) throws IOException {
        if (!M4aMonoAudioStream.isAvailable() || !Files.isRegularFile(path)) {
            throw new IOException("FFmpeg no disponible para M4A: " + path);
        }

        List<String> args = new ArrayList<>();
        args.add(executable());
        args.add("-nostdin");
        args.add("-loglevel");
        args.add("error");

        if (startSeconds > 0.0D) {
            args.add("-ss");
            args.add(Double.toString(startSeconds));
        }

        args.add("-i");
        args.add(path.toAbsolutePath().normalize().toString());
        args.add("-vn");
        args.add("-f");
        args.add("s16le");
        args.add("-acodec");
        args.add("pcm_s16le");
        args.add("-ac");
        args.add("2");
        args.add("-ar");
        args.add("44100");
        args.add("pipe:1");

        process = new ProcessBuilder(args)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        source = process.getInputStream();
    }

    private static String executable() {
        return System.getProperty("vinylcraft.ffmpeg.path", "ffmpeg");
    }

    @Override
    public AudioFormat getFormat() {
        return FORMAT;
    }

    @Override
    public boolean readChunk(FloatConsumer consumer) throws IOException {
        if (closed) {
            return false;
        }

        int length = 0;

        while (length < buffer.length) {
            int read = source.read(buffer, length, buffer.length - length);

            if (read < 0) {
                break;
            }

            if (read == 0) {
                break;
            }

            length += read;
        }

        length -= length % 2;

        if (length == 0) {
            return false;
        }

        for (int index = 0; index < length; index += 2) {
            short sample = (short) (
                    ((buffer[index + 1] & 255) << 8)
                            | (buffer[index] & 255)
            );
            consumer.accept(sample / 32768.0F);
        }

        return true;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }

        closed = true;

        try {
            source.close();
        } finally {
            process.destroy();

            try {
                if (!process.waitFor(200, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException interrupted) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }
}
