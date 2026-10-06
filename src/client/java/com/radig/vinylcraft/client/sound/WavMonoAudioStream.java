package com.radig.vinylcraft.client.sound;

import java.io.IOException;
import java.nio.file.Path;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.sounds.FloatSampleSource;

/** WAV PCM -> PCM mono en streaming para OpenAL. */
public final class WavMonoAudioStream implements FloatSampleSource {

    private static final int FRAMES_PER_CHUNK = 4096;

    private final AudioInputStream source;
    private final AudioInputStream pcmStream;
    private final AudioFormat sourcePcmFormat;
    private final AudioFormat monoFormat;
    private final byte[] buffer;

    private boolean closed;

    public WavMonoAudioStream(Path path) throws IOException {
        this(path, 0.0D);
    }

    public WavMonoAudioStream(
            Path path,
            double startSeconds)
            throws IOException {

        try {
            source = AudioSystem.getAudioInputStream(path.toFile());

            AudioFormat original = source.getFormat();
            int channels = Math.max(1, original.getChannels());

            sourcePcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    original.getSampleRate(),
                    16,
                    channels,
                    channels * 2,
                    original.getSampleRate(),
                    false
            );

            pcmStream = AudioSystem.getAudioInputStream(
                    sourcePcmFormat,
                    source
            );

            monoFormat = new AudioFormat(
                    sourcePcmFormat.getSampleRate(),
                    16,
                    1,
                    true,
                    false
            );

            buffer = new byte[
                    FRAMES_PER_CHUNK
                            * sourcePcmFormat.getFrameSize()
            ];

            skipTo(Math.max(0.0D, startSeconds));

        } catch (Exception exception) {
            throw exception instanceof IOException io
                    ? io
                    : new IOException(
                            "No se pudo abrir WAV: " + path,
                            exception
                    );
        }
    }

    private void skipTo(double startSeconds) throws IOException {
        if (startSeconds <= 0.0D) {
            return;
        }

        int frameSize = Math.max(1, sourcePcmFormat.getFrameSize());
        double frameRate = Math.max(1.0D, sourcePcmFormat.getFrameRate());

        long framesToSkip = (long) Math.floor(startSeconds * frameRate);
        long remaining = framesToSkip * frameSize;

        while (remaining > 0L) {
            long skipped = pcmStream.skip(remaining);

            if (skipped > 0L) {
                remaining -= skipped;
                continue;
            }

            int read = pcmStream.read(
                    buffer,
                    0,
                    (int) Math.min(buffer.length, remaining)
            );

            if (read < 0) {
                break;
            }

            remaining -= read;
        }
    }

    @Override
    public AudioFormat getFormat() {
        return monoFormat;
    }

    @Override
    public boolean readChunk(FloatConsumer consumer)
            throws IOException {

        if (closed) {
            return false;
        }

        int read = pcmStream.read(buffer);

        if (read <= 0) {
            return false;
        }

        int frameSize = sourcePcmFormat.getFrameSize();
        int channels = sourcePcmFormat.getChannels();
        int frameCount = read / frameSize;

        for (int frame = 0; frame < frameCount; frame++) {
            int frameOffset = frame * frameSize;
            int sum = 0;

            for (int channel = 0; channel < channels; channel++) {
                int offset = frameOffset + channel * 2;

                int low = buffer[offset] & 0xFF;
                int high = buffer[offset + 1];
                short sample = (short) ((high << 8) | low);

                sum += sample;
            }

            consumer.accept(
                    (sum / (float) channels) / 32768.0F
            );
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
            pcmStream.close();
        } finally {
            source.close();
        }
    }
}
