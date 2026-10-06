package com.radig.vinylcraft.client.sound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

/**
 * Reproductor de archivos locales usado por VinylCraft.
 *
 * MP3 se decodifica con JLayer. WAV se reproduce mediante Java Sound.
 * El objetivo es permitir que los álbumes grabados desde la biblioteca
 * reproduzcan los archivos reales del usuario, sin copiarlos al mundo.
 */
public final class LocalTrackAudioPlayer {

    private final Path path;

    private final Object pauseLock = new Object();

    private volatile boolean paused;
    private volatile boolean stopped;
    private volatile boolean finished;

    private volatile float volume = 0.65F;

    private volatile SourceDataLine line;
    private volatile Thread thread;

    public LocalTrackAudioPlayer(Path path) {
        this.path = path.toAbsolutePath().normalize();
    }

    public Path path() {
        return path;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void start() {
        if (thread != null) {
            return;
        }

        thread = new Thread(
                this::runPlayback,
                "VinylCraft Local Audio - " + path.getFileName()
        );

        thread.setDaemon(true);
        thread.start();
    }

    public void setPaused(boolean paused) {
        this.paused = paused;

        SourceDataLine localLine = line;

        if (localLine != null) {
            if (paused) {
                localLine.stop();
            } else if (!stopped && !finished) {
                localLine.start();
            }
        }

        if (!paused) {
            synchronized (pauseLock) {
                pauseLock.notifyAll();
            }
        }
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0.0F, Math.min(1.0F, volume));
        applyVolume();
    }

    public void stop() {
        stopped = true;
        paused = false;

        synchronized (pauseLock) {
            pauseLock.notifyAll();
        }

        SourceDataLine localLine = line;

        if (localLine != null) {
            try {
                localLine.stop();
                localLine.flush();
                localLine.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void runPlayback() {
        try {
            String extension = extension(path);

            if (extension.equals("mp3")) {
                playMp3();
            } else if (extension.equals("wav")) {
                playWav();
            } else {
                System.err.println(
                        "[VinylCraft] Formato de reproducción local todavía no soportado: "
                                + extension
                                + " ("
                                + path
                                + ")"
                );
            }
        } catch (Exception exception) {
            if (!stopped) {
                System.err.println(
                        "[VinylCraft] No se pudo reproducir el archivo local: "
                                + path
                );
                exception.printStackTrace();
            }
        } finally {
            finished = true;

            SourceDataLine localLine = line;
            if (localLine != null) {
                try {
                    localLine.drain();
                    localLine.stop();
                    localLine.close();
                } catch (Exception ignored) {
                }
            }

            line = null;
        }
    }

    private void playMp3() throws Exception {
        try (
                InputStream input =
                        new BufferedInputStream(Files.newInputStream(path))
        ) {
            Bitstream bitstream = new Bitstream(input);
            Decoder decoder = new Decoder();

            try {
                while (!stopped) {
                    waitIfPaused();

                    if (stopped) {
                        break;
                    }

                    Header header = bitstream.readFrame();
                    if (header == null) {
                        break;
                    }

                    SampleBuffer output =
                            (SampleBuffer) decoder.decodeFrame(
                                    header,
                                    bitstream
                            );

                    ensureLine(
                            output.getSampleFrequency(),
                            output.getChannelCount()
                    );

                    short[] samples = output.getBuffer();
                    int sampleCount = output.getBufferLength();
                    byte[] bytes = new byte[sampleCount * 2];

                    for (int i = 0; i < sampleCount; i++) {
                        short sample = samples[i];
                        bytes[i * 2] = (byte) (sample & 0xFF);
                        bytes[i * 2 + 1] = (byte) ((sample >>> 8) & 0xFF);
                    }

                    SourceDataLine localLine = line;
                    if (localLine != null && !stopped) {
                        localLine.write(bytes, 0, bytes.length);
                    }

                    bitstream.closeFrame();
                }
            } finally {
                try {
                    bitstream.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void playWav() throws Exception {
        try (
                AudioInputStream source =
                        AudioSystem.getAudioInputStream(path.toFile())
        ) {
            AudioFormat sourceFormat = source.getFormat();

            AudioFormat pcm = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sourceFormat.getSampleRate(),
                    16,
                    sourceFormat.getChannels(),
                    sourceFormat.getChannels() * 2,
                    sourceFormat.getSampleRate(),
                    false
            );

            try (
                    AudioInputStream decoded =
                            AudioSystem.getAudioInputStream(pcm, source)
            ) {
                ensureLine(
                        Math.round(pcm.getSampleRate()),
                        pcm.getChannels()
                );

                byte[] buffer = new byte[32 * 1024];

                while (!stopped) {
                    waitIfPaused();

                    if (stopped) {
                        break;
                    }

                    int read = decoded.read(buffer);
                    if (read < 0) {
                        break;
                    }

                    SourceDataLine localLine = line;
                    if (localLine != null) {
                        localLine.write(buffer, 0, read);
                    }
                }
            }
        }
    }

    private void ensureLine(
            int sampleRate,
            int channels)
            throws Exception {

        if (line != null) {
            return;
        }

        AudioFormat format = new AudioFormat(
                sampleRate,
                16,
                channels,
                true,
                false
        );

        DataLine.Info info =
                new DataLine.Info(
                        SourceDataLine.class,
                        format
                );

        SourceDataLine newLine =
                (SourceDataLine) AudioSystem.getLine(info);

        newLine.open(format, 64 * 1024);
        line = newLine;
        applyVolume();

        if (!paused) {
            newLine.start();
        }
    }

    private void waitIfPaused()
            throws InterruptedException {

        synchronized (pauseLock) {
            while (paused && !stopped) {
                pauseLock.wait(100L);
            }
        }
    }

    private void applyVolume() {
        SourceDataLine localLine = line;

        if (
                localLine == null
                        || !localLine.isControlSupported(
                                FloatControl.Type.MASTER_GAIN
                        )
        ) {
            return;
        }

        FloatControl gain =
                (FloatControl) localLine.getControl(
                        FloatControl.Type.MASTER_GAIN
                );

        float value;

        if (volume <= 0.0001F) {
            value = gain.getMinimum();
        } else {
            value = (float) (20.0D * Math.log10(volume));
            value = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), value));
        }

        gain.setValue(value);
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');

        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }

        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public static boolean supports(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return false;
        }

        String extension = extension(path);
        return extension.equals("mp3") || extension.equals("wav");
    }
}
