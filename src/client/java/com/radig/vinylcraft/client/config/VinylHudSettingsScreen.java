package com.radig.vinylcraft.client.config;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Pantalla F6 para configurar el HUD y el alcance del audio de VinylCraft. */
public final class VinylHudSettingsScreen extends Screen {

    private Button enabledButton;
    private Button positionButton;
    private Button alphaModeButton;

    public VinylHudSettingsScreen() {
        super(Component.literal("VinylCraft - Configuración"));
    }

    @Override
    protected void init() {
        // Dos grupos: visualización (izquierda) y audio (derecha).
        int gap = 12;
        int available = Math.min(600, Math.max(300, width - 36));
        int controlWidth = (available - gap) / 2;
        int xLeft = (width - available) / 2;
        int xRight = xLeft + controlWidth + gap;
        int y = Math.max(43, height / 2 - 76);

        enabledButton = this.addRenderableWidget(
                Button.builder(enabledMessage(), button -> {
                    VinylHudConfig.setEnabled(!VinylHudConfig.isEnabled());
                    button.setMessage(enabledMessage());
                    VinylHudConfig.save();
                }).bounds(xLeft, y, controlWidth, 20).build()
        );

        positionButton = this.addRenderableWidget(
                Button.builder(positionMessage(), button -> {
                    VinylHudConfig.setPosition(VinylHudConfig.getPosition().next());
                    button.setMessage(positionMessage());
                    VinylHudConfig.save();
                }).bounds(xLeft, y + 26, controlWidth, 20).build()
        );

        this.addRenderableWidget(new SizeSlider(xLeft, y + 52, controlWidth, 20));
        this.addRenderableWidget(new AlphaSlider(xLeft, y + 78, controlWidth, 20));

        alphaModeButton = this.addRenderableWidget(
                Button.builder(alphaModeMessage(), button -> {
                    VinylHudConfig.setAlphaMode(VinylHudConfig.getAlphaMode().next());
                    button.setMessage(alphaModeMessage());
                    VinylHudConfig.save();
                }).bounds(xRight, y, controlWidth, 20).build()
        );
        this.addRenderableWidget(new SoundDistanceSlider(xRight, y + 26, controlWidth, 20));
        this.addRenderableWidget(new MusicVolumeSlider(xRight, y + 52, controlWidth, 20));

        this.addRenderableWidget(
                Button.builder(Component.literal("Cerrar"), button -> onClose())
                .bounds(width / 2 - 65, Math.min(height - 30, y + 145), 130, 20)
                .build()
        );
    }

    private Component enabledMessage() {
        return Component.literal(
                "HUD: " + (VinylHudConfig.isEnabled() ? "Activado" : "Desactivado")
        );
    }

    private Component positionMessage() {
        return Component.literal(
                "Posición: " + VinylHudConfig.getPosition().label()
        );
    }

    private Component alphaModeMessage() {
        return Component.literal(
                "Transparencia: " + VinylHudConfig.getAlphaMode().label()
        );
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta) {

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.centeredText(
                this.font,
                "VinylCraft - Configuración",
                this.width / 2,
                20,
                0xFFFFFFFF
        );
        int available = Math.min(600, Math.max(300, width - 36));
        int controlWidth = (available - 12) / 2;
        int left = (width - available) / 2;
        int y = Math.max(43, height / 2 - 76);
        graphics.centeredText(font, "HUD", left + controlWidth / 2,
                y - 12, 0xFFBBBBBB);
        graphics.centeredText(font, "Audio", left + controlWidth + 12 + controlWidth / 2,
                y - 12, 0xFFBBBBBB);

    }

    @Override
    public void onClose() {
        VinylHudConfig.save();

        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(null);
        }
    }

    private static final class SizeSlider extends AbstractSliderButton {

        private SizeSlider(int x, int y, int width, int height) {
            super(
                    x,
                    y,
                    width,
                    height,
                    Component.empty(),
                    normalize(
                            VinylHudConfig.getScale(),
                            VinylHudConfig.MIN_SCALE,
                            VinylHudConfig.MAX_SCALE
                    )
            );

            updateMessage();
        }

        @Override
        protected void updateMessage() {
            float scale = denormalize(
                    this.value,
                    VinylHudConfig.MIN_SCALE,
                    VinylHudConfig.MAX_SCALE
            );

            setMessage(
                    Component.literal(
                            "Tamaño: " + Math.round(scale * 100.0F) + "%"
                    )
            );
        }

        @Override
        protected void applyValue() {
            VinylHudConfig.setScale(
                    denormalize(
                            this.value,
                            VinylHudConfig.MIN_SCALE,
                            VinylHudConfig.MAX_SCALE
                    )
            );
            VinylHudConfig.save();
        }
    }

    private static final class AlphaSlider extends AbstractSliderButton {

        private AlphaSlider(int x, int y, int width, int height) {
            super(
                    x,
                    y,
                    width,
                    height,
                    Component.empty(),
                    normalize(
                            VinylHudConfig.getAlpha(),
                            VinylHudConfig.MIN_ALPHA,
                            VinylHudConfig.MAX_ALPHA
                    )
            );

            updateMessage();
        }

        @Override
        protected void updateMessage() {
            float alpha = denormalize(
                    this.value,
                    VinylHudConfig.MIN_ALPHA,
                    VinylHudConfig.MAX_ALPHA
            );

            setMessage(
                    Component.literal(
                            "Opacidad: " + Math.round(alpha * 100.0F) + "%"
                    )
            );
        }

        @Override
        protected void applyValue() {
            VinylHudConfig.setAlpha(
                    denormalize(
                            this.value,
                            VinylHudConfig.MIN_ALPHA,
                            VinylHudConfig.MAX_ALPHA
                    )
            );
            VinylHudConfig.save();
        }
    }

    /**
     * El rango usa una curva cuadrática: da mucha más precisión en distancias
     * pequeñas (una habitación) sin renunciar a poder llegar muy lejos.
     */
    private static final class SoundDistanceSlider extends AbstractSliderButton {

        private SoundDistanceSlider(int x, int y, int width, int height) {
            super(
                    x,
                    y,
                    width,
                    height,
                    Component.empty(),
                    normalizeDistance(VinylHudConfig.getSoundDistance())
            );

            updateMessage();
        }

        @Override
        protected void updateMessage() {
            float distance = denormalizeDistance(this.value);

            setMessage(
                    Component.literal(
                            "Distancia de audio: "
                                    + Math.round(distance)
                                    + " bloques"
                    )
            );
        }

        @Override
        protected void applyValue() {
            VinylHudConfig.setSoundDistance(
                    denormalizeDistance(this.value)
            );
            VinylHudConfig.save();
        }
    }


    private static final class MusicVolumeSlider extends AbstractSliderButton {

        private MusicVolumeSlider(int x, int y, int width, int height) {
            super(
                    x,
                    y,
                    width,
                    height,
                    Component.empty(),
                    normalize(
                            VinylHudConfig.getMusicVolume(),
                            VinylHudConfig.MIN_MUSIC_VOLUME,
                            VinylHudConfig.MAX_MUSIC_VOLUME
                    )
            );

            updateMessage();
        }

        @Override
        protected void updateMessage() {
            float volume = denormalize(
                    this.value,
                    VinylHudConfig.MIN_MUSIC_VOLUME,
                    VinylHudConfig.MAX_MUSIC_VOLUME
            );

            setMessage(
                    Component.literal(
                            "Volumen de música: "
                                    + Math.round(volume * 100.0F)
                                    + "%"
                    )
            );
        }

        @Override
        protected void applyValue() {
            VinylHudConfig.setMusicVolume(
                    denormalize(
                            this.value,
                            VinylHudConfig.MIN_MUSIC_VOLUME,
                            VinylHudConfig.MAX_MUSIC_VOLUME
                    )
            );
            VinylHudConfig.save();
        }
    }

    private static double normalize(float value, float min, float max) {
        return (value - min) / (double) (max - min);
    }

    private static float denormalize(double value, float min, float max) {
        double clamped = Math.max(0.0D, Math.min(1.0D, value));
        return (float) (min + (max - min) * clamped);
    }

    private static double normalizeDistance(float distance) {
        double linear = normalize(
                distance,
                VinylHudConfig.MIN_SOUND_DISTANCE,
                VinylHudConfig.MAX_SOUND_DISTANCE
        );

        return Math.sqrt(
                Math.max(0.0D, Math.min(1.0D, linear))
        );
    }

    private static float denormalizeDistance(double value) {
        double clamped = Math.max(0.0D, Math.min(1.0D, value));
        double curved = clamped * clamped;

        return (float) (
                VinylHudConfig.MIN_SOUND_DISTANCE
                        + (
                            VinylHudConfig.MAX_SOUND_DISTANCE
                                    - VinylHudConfig.MIN_SOUND_DISTANCE
                        ) * curved
        );
    }
}
