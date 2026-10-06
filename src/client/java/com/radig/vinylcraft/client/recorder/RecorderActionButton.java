package com.radig.vinylcraft.client.recorder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Botón de acción principal del grabador.
 * Conserva el comportamiento/narración de Button, pero dibuja
 * una superficie roja encima del sprite vanilla.
 */
public final class RecorderActionButton extends Button {

    public RecorderActionButton(
            int x,
            int y,
            int width,
            int height,
            Component message,
            OnPress onPress) {

        super(
                x,
                y,
                width,
                height,
                message,
                onPress,
                DEFAULT_NARRATION
        );
    }

    @Override
    protected void extractContents(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta) {

        int fillColor;
        int borderColor;
        int textColor;

        if (!this.active) {
            fillColor = 0xCC4A2525;
            borderColor = 0xFF6B4747;
            textColor = 0xFFB8A0A0;
        } else if (this.isHoveredOrFocused()) {
            fillColor = 0xFFE04444;
            borderColor = 0xFFFF8A8A;
            textColor = 0xFFFFFFFF;
        } else {
            fillColor = 0xFFAA2424;
            borderColor = 0xFFE45B5B;
            textColor = 0xFFFFFFFF;
        }

        graphics.fill(
                getX() + 1,
                getY() + 1,
                getRight() - 1,
                getBottom() - 1,
                fillColor
        );

        graphics.fill(
                getX() + 1,
                getY() + 1,
                getRight() - 1,
                getY() + 2,
                borderColor
        );

        graphics.centeredText(
                Minecraft.getInstance().font,
                getMessage(),
                getX() + getWidth() / 2,
                getY() + (getHeight() - 8) / 2,
                textColor
        );
    }
}
