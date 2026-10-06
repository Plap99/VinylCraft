package com.radig.vinylcraft.client.sound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.sounds.FloatSampleSource;

/**
 * MP3 -> PCM mono en streaming.
 *
 * JLayer decodifica el MP3 original. Si la fuente es estéreo,
 * se mezclan sus canales a mono antes de entregarlos al Channel
 * de Minecraft/OpenAL, permitiendo audio posicional real.
 *
 * También permite comenzar desde un desplazamiento temporal. Para MP3
 * hacemos seek seguro decodificando y descartando frames hasta alcanzar
 * la posición requerida; solo ocurre al crear/recrear el stream.
 */
public final class Mp3MonoAudioStream implements FloatSampleSource {

    private final InputStream input;
    private final Bitstream bitstream;
    private final Decoder decoder;
    private final AudioFormat format;

    private short[] pendingSamples;
    private int pendingSampleCount;
    private int pendingChannels;
    private boolean closed;

    public Mp3MonoAudioStream(Path path) throws IOException {
        this(path, 0.0D);
    }

    public Mp3MonoAudioStream(
            Path path,
            double startSeconds)
            throws IOException {

        try {
            this.input = new BufferedInputStream(
                    Files.newInputStream(path)
            );
            this.bitstream = new Bitstream(input);
            this.decoder = new Decoder();

            DecodedFrame first = seekTo(
                    Math.max(0.0D, startSeconds)
            );

            if (first == null) {
                throw new IOException(
                        "MP3 sin audio en la posición solicitada: " + path
                );
            }

            this.pendingSamples = first.samples();
            this.pendingSampleCount = first.sampleCount();
            this.pendingChannels = first.channels();

            this.format = new AudioFormat(
                    first.sampleRate(),
                    16,
                    1,
                    true,
                    false
            );
        } catch (Exception exception) {
            throw exception instanceof IOException io
                    ? io
                    : new IOException(
                            "No se pudo abrir MP3: " + path,
                            exception
                    );
        }
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public boolean readChunk(FloatConsumer consumer)
            throws IOException {

        if (closed) {
            return false;
        }

        DecodedFrame frame;

        if (pendingSamples != null) {
            frame = new DecodedFrame(
                    pendingSamples,
                    pendingSampleCount,
                    pendingChannels,
                    Math.round(format.getSampleRate())
            );

            pendingSamples = null;
            pendingSampleCount = 0;
            pendingChannels = 0;
        } else {
            frame = decodeNextFrame();
        }

        if (frame == null) {
            return false;
        }

        emitMono(frame, consumer);
        return true;
    }

    private DecodedFrame seekTo(double startSeconds)
            throws IOException {

        double remaining = Math.max(0.0D, startSeconds);

        while (true) {
            DecodedFrame frame = decodeNextFrame();

            if (frame == null) {
                return null;
            }

            int channels = Math.max(1, frame.channels());
            int sampleRate = Math.max(1, frame.sampleRate());
            int pcmFrames = frame.sampleCount() / channels;

            double frameSeconds =
                    pcmFrames / (double) sampleRate;

            if (remaining >= frameSeconds && frameSeconds > 0.0D) {
                remaining -= frameSeconds;
                continue;
            }

            if (remaining <= 0.0D) {
                return frame;
            }

            int framesToSkip = (int) Math.floor(
                    remaining * sampleRate
            );

            int samplesToSkip = Math.min(
                    frame.sampleCount(),
                    Math.max(0, framesToSkip * channels)
            );

            if (samplesToSkip >= frame.sampleCount()) {
                remaining = 0.0D;
                continue;
            }

            short[] trimmed = Arrays.copyOfRange(
                    frame.samples(),
                    samplesToSkip,
                    frame.sampleCount()
            );

            return new DecodedFrame(
                    trimmed,
                    trimmed.length,
                    channels,
                    sampleRate
            );
        }
    }

    private DecodedFrame decodeNextFrame()
            throws IOException {

        Header header;

        try {
            header = bitstream.readFrame();
        } catch (Exception exception) {
            throw new IOException("Error leyendo cuadro MP3", exception);
        }

        if (header == null) {
            return null;
        }

        try {
            SampleBuffer output =
                    (SampleBuffer) decoder.decodeFrame(
                            header,
                            bitstream
                    );

            int count = output.getBufferLength();

            short[] samples = Arrays.copyOf(
                    output.getBuffer(),
                    count
            );

            return new DecodedFrame(
                    samples,
                    count,
                    Math.max(1, output.getChannelCount()),
                    output.getSampleFrequency()
            );

        } catch (Exception exception) {
            throw new IOException("Error decodificando cuadro MP3", exception);
        } finally {
            try {
                bitstream.closeFrame();
            } catch (Exception ignored) {
            }
        }
    }

    private static void emitMono(
            DecodedFrame frame,
            FloatConsumer consumer) {

        short[] samples = frame.samples();
        int count = frame.sampleCount();
        int channels = Math.max(1, frame.channels());

        if (channels == 1) {
            for (int index = 0; index < count; index++) {
                consumer.accept(samples[index] / 32768.0F);
            }
            return;
        }

        for (int index = 0; index < count; index += channels) {
            int sum = 0;
            int used = 0;

            for (int channel = 0; channel < channels; channel++) {
                int sampleIndex = index + channel;

                if (sampleIndex >= count) {
                    break;
                }

                sum += samples[sampleIndex];
                used++;
            }

            if (used > 0) {
                consumer.accept(
                        (sum / (float) used) / 32768.0F
                );
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }

        closed = true;

        try {
            bitstream.close();
        } catch (Exception ignored) {
            input.close();
        }
    }

    private record DecodedFrame(
            short[] samples,
            int sampleCount,
            int channels,
            int sampleRate) {
    }
}
