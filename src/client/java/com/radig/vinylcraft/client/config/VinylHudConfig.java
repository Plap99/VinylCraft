package com.radig.vinylcraft.client.config;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Configuración persistente del HUD y del audio posicional de VinylCraft.
 */
public final class VinylHudConfig {

    public enum Position {
        TOP_LEFT("Superior izquierda"),
        TOP_RIGHT("Superior derecha"),
        BOTTOM_LEFT("Inferior izquierda"),
        BOTTOM_RIGHT("Inferior derecha");

        private final String label;

        Position(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public Position next() {
            Position[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public enum SourceMode {
        ALL("Todas"),
        DISCMAN_ONLY("Solo Discman"),
        PLAYERS_ONLY("Solo reproductores");

        private final String label;

        SourceMode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public SourceMode next() {
            SourceMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public enum AlphaMode {
        BACKGROUND_ONLY("Solo fondo"),
        WHOLE_HUD("Todo el HUD");

        private final String label;

        AlphaMode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public AlphaMode next() {
            return this == BACKGROUND_ONLY
                    ? WHOLE_HUD
                    : BACKGROUND_ONLY;
        }
    }

    public static final float MIN_SCALE = 0.30F;
    public static final float MAX_SCALE = 1.00F;

    public static final float MIN_ALPHA = 0.20F;
    public static final float MAX_ALPHA = 1.00F;

    public static final float MIN_SOUND_DISTANCE = 4.0F;
    public static final float MAX_SOUND_DISTANCE = 128.0F;

    public static final float MIN_MUSIC_VOLUME = 0.0F;
    public static final float MAX_MUSIC_VOLUME = 1.0F;

    public static final int MIN_HUD_SOURCES = 1;
    public static final int MAX_HUD_SOURCES = 5;

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private static final Path CONFIG_FILE =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("vinylcraft")
                    .resolve("hud.json");

    private static boolean enabled = true;
    private static float scale = 0.50F;
    private static float alpha = 0.72F;
    private static Position position = Position.TOP_RIGHT;
    private static AlphaMode alphaMode = AlphaMode.BACKGROUND_ONLY;
    private static float soundDistance = 32.0F;
    private static float musicVolume = 1.0F;
    private static SourceMode sourceMode = SourceMode.ALL;
    private static int maxHudSources = 3;

    private VinylHudConfig() {
    }

    public static void load() {
        if (!Files.isRegularFile(CONFIG_FILE)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            Data data = GSON.fromJson(reader, Data.class);

            if (data == null) {
                return;
            }

            enabled = data.enabled;

            if (data.scale > 0.0F) {
                scale = clamp(data.scale, MIN_SCALE, MAX_SCALE);
            }

            if (data.alpha > 0.0F) {
                alpha = clamp(data.alpha, MIN_ALPHA, MAX_ALPHA);
            }

            if (data.soundDistance > 0.0F) {
                soundDistance = clamp(
                        data.soundDistance,
                        MIN_SOUND_DISTANCE,
                        MAX_SOUND_DISTANCE
                );
            }

            musicVolume = clamp(
                    data.musicVolume,
                    MIN_MUSIC_VOLUME,
                    MAX_MUSIC_VOLUME
            );

            if (data.position != null) {
                try {
                    position = Position.valueOf(data.position);
                } catch (IllegalArgumentException ignored) {
                    position = Position.TOP_RIGHT;
                }
            }

            if (data.alphaMode != null) {
                try {
                    alphaMode = AlphaMode.valueOf(data.alphaMode);
                } catch (IllegalArgumentException ignored) {
                    alphaMode = AlphaMode.BACKGROUND_ONLY;
                }
            }

            if (data.sourceMode != null) {
                try {
                    sourceMode = SourceMode.valueOf(data.sourceMode);
                } catch (IllegalArgumentException ignored) {
                    sourceMode = SourceMode.ALL;
                }
            }

            if (data.maxHudSources > 0) {
                maxHudSources = clampInt(
                        data.maxHudSources,
                        MIN_HUD_SOURCES,
                        MAX_HUD_SOURCES
                );
            }
        } catch (Exception exception) {
            System.err.println("[VinylCraft] No se pudo cargar hud.json");
            exception.printStackTrace();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());

            Data data = new Data();
            data.enabled = enabled;
            data.scale = scale;
            data.alpha = alpha;
            data.position = position.name();
            data.alphaMode = alphaMode.name();
            data.soundDistance = soundDistance;
            data.musicVolume = musicVolume;
            data.sourceMode = sourceMode.name();
            data.maxHudSources = maxHudSources;

            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (Exception exception) {
            System.err.println("[VinylCraft] No se pudo guardar hud.json");
            exception.printStackTrace();
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static float getScale() {
        return scale;
    }

    public static void setScale(float value) {
        scale = clamp(value, MIN_SCALE, MAX_SCALE);
    }

    public static float getAlpha() {
        return alpha;
    }

    public static void setAlpha(float value) {
        alpha = clamp(value, MIN_ALPHA, MAX_ALPHA);
    }

    public static Position getPosition() {
        return position;
    }

    public static void setPosition(Position value) {
        position = value == null ? Position.TOP_RIGHT : value;
    }

    public static AlphaMode getAlphaMode() {
        return alphaMode;
    }

    public static void setAlphaMode(AlphaMode value) {
        alphaMode = value == null
                ? AlphaMode.BACKGROUND_ONLY
                : value;
    }

    public static float getSoundDistance() {
        return soundDistance;
    }

    public static void setSoundDistance(float value) {
        soundDistance = clamp(
                value,
                MIN_SOUND_DISTANCE,
                MAX_SOUND_DISTANCE
        );
    }

    public static float getMusicVolume() {
        return musicVolume;
    }

    public static void setMusicVolume(float value) {
        musicVolume = clamp(
                value,
                MIN_MUSIC_VOLUME,
                MAX_MUSIC_VOLUME
        );
    }


    public static SourceMode getSourceMode() {
        return sourceMode;
    }

    public static void setSourceMode(SourceMode value) {
        sourceMode = value == null ? SourceMode.ALL : value;
    }

    public static int getMaxHudSources() {
        return maxHudSources;
    }

    public static void setMaxHudSources(int value) {
        maxHudSources = clampInt(
                value,
                MIN_HUD_SOURCES,
                MAX_HUD_SOURCES
        );
    }

    public static Path getConfigFile() {
        return CONFIG_FILE;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return min;
        }

        return Math.max(min, Math.min(max, value));
    }

    private static final class Data {
        private boolean enabled = true;
        private float scale = 0.50F;
        private float alpha = 0.72F;
        private String position = Position.TOP_RIGHT.name();
        private String alphaMode = AlphaMode.BACKGROUND_ONLY.name();
        private float soundDistance = 32.0F;
        private float musicVolume = 1.0F;
        private String sourceMode = SourceMode.ALL.name();
        private int maxHudSources = 3;
    }
}
