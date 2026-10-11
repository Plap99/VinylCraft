package com.radig.vinylcraft.client.sound;

import java.io.IOException;
import java.nio.file.Path;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.sounds.FloatSampleSource;

/** WAV PCM -> estéreo para el Discman. */
public final class WavStereoAudioStream implements FloatSampleSource {

    private static final int FRAMES_PER_CHUNK = 4096;

    private final AudioInputStream source;
    private final AudioInputStream pcmStream;
    private final AudioFormat sourcePcmFormat;
    private final AudioFormat stereoFormat;
    private final byte[] buffer;
    private boolean closed;

    public WavStereoAudioStream(Path path, double startSeconds) throws IOException {
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

            pcmStream = AudioSystem.getAudioInputStream(sourcePcmFormat, source);

            stereoFormat = new AudioFormat(
                    sourcePcmFormat.getSampleRate(),
                    16,
                    2,
                    true,
                    false
            );

            buffer = new byte[
                    FRAMES_PER_CHUNK * sourcePcmFormat.getFrameSize()
            ];

            skipTo(Math.max(0.0D, startSeconds));
        } catch (Exception exception) {
            throw exception instanceof IOException io
                    ? io
                    : new IOException("No se pudo abrir WAV estéreo: " + path, exception);
        }
    }

    private void skipTo(double startSeconds) throws IOException {
        if (startSeconds <= 0.0D) {
            return;
        }

        long framesToSkip = (long) Math.floor(
                startSeconds * Math.max(1.0D, sourcePcmFormat.getFrameRate())
        );
        long remaining = framesToSkip * Math.max(1, sourcePcmFormat.getFrameSize());

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
        return stereoFormat;
    }

    @Override
    public boolean readChunk(FloatConsumer consumer) throws IOException {
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
            int offset = frame * frameSize;
            short left = readSample(offset);
            short right = channels > 1 ? readSample(offset + 2) : left;

            consumer.accept(left / 32768.0F);
            consumer.accept(right / 32768.0F);
        }

        return true;
    }

    private short readSample(int offset) {
        int low = buffer[offset] & 0xFF;
        int high = buffer[offset + 1];
        return (short) ((high << 8) | low);
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
