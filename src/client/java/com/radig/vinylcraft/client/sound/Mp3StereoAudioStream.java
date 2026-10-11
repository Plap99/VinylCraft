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

/** MP3 -> PCM estéreo para los audífonos del Discman. */
public final class Mp3StereoAudioStream implements FloatSampleSource {

    private final InputStream input;
    private final Bitstream bitstream;
    private final Decoder decoder;
    private final AudioFormat format;

    private DecodedFrame pending;
    private boolean closed;

    public Mp3StereoAudioStream(Path path, double startSeconds) throws IOException {
        try {
            input = new BufferedInputStream(Files.newInputStream(path));
            bitstream = new Bitstream(input);
            decoder = new Decoder();

            pending = seekTo(Math.max(0.0D, startSeconds));

            if (pending == null) {
                throw new IOException("MP3 sin audio en la posición solicitada: " + path);
            }

            format = new AudioFormat(
                    pending.sampleRate(),
                    16,
                    2,
                    true,
                    false
            );
        } catch (Exception exception) {
            throw exception instanceof IOException io
                    ? io
                    : new IOException("No se pudo abrir MP3 estéreo: " + path, exception);
        }
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public boolean readChunk(FloatConsumer consumer) throws IOException {
        if (closed) {
            return false;
        }

        DecodedFrame frame;

        if (pending != null) {
            frame = pending;
            pending = null;
        } else {
            frame = decodeNextFrame();
        }

        if (frame == null) {
            return false;
        }

        emitStereo(frame, consumer);
        return true;
    }

    private DecodedFrame seekTo(double startSeconds) throws IOException {
        double remaining = startSeconds;

        while (true) {
            DecodedFrame frame = decodeNextFrame();

            if (frame == null) {
                return null;
            }

            int channels = Math.max(1, frame.channels());
            int frames = frame.sampleCount() / channels;
            double seconds = frames / (double) Math.max(1, frame.sampleRate());

            if (remaining >= seconds && seconds > 0.0D) {
                remaining -= seconds;
                continue;
            }

            if (remaining <= 0.0D) {
                return frame;
            }

            int framesToSkip = (int) Math.floor(remaining * frame.sampleRate());
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
                    frame.sampleRate()
            );
        }
    }

    private DecodedFrame decodeNextFrame() throws IOException {
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
            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
            int count = output.getBufferLength();

            return new DecodedFrame(
                    Arrays.copyOf(output.getBuffer(), count),
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

    private static void emitStereo(DecodedFrame frame, FloatConsumer consumer) {
        short[] samples = frame.samples();
        int count = frame.sampleCount();
        int channels = Math.max(1, frame.channels());

        if (channels == 1) {
            for (int index = 0; index < count; index++) {
                float value = samples[index] / 32768.0F;
                consumer.accept(value);
                consumer.accept(value);
            }
            return;
        }

        for (int index = 0; index + 1 < count; index += channels) {
            consumer.accept(samples[index] / 32768.0F);
            consumer.accept(samples[index + 1] / 32768.0F);
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
