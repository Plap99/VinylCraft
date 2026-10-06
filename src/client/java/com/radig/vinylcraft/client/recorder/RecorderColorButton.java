package com.radig.vinylcraft.client.recorder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Botón compacto con tema de color para acciones del grabador. */
public final class RecorderColorButton extends Button {

    public enum Theme {
        GREEN,
        RED
    }

    private final Theme theme;

    public RecorderColorButton(
            int x,
            int y,
            int width,
            int height,
            Component message,
            OnPress onPress,
            Theme theme) {

        super(
                x,
                y,
                width,
                height,
                message,
                onPress,
                DEFAULT_NARRATION
        );

        this.theme = theme;
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
            fillColor = 0xCC3A3A3A;
            borderColor = 0xFF595959;
            textColor = 0xFF929292;
        } else if (theme == Theme.GREEN) {
            if (this.isHoveredOrFocused()) {
                fillColor = 0xFF2F9B4E;
                borderColor = 0xFF73D68C;
            } else {
                fillColor = 0xFF206E38;
                borderColor = 0xFF4EAC68;
            }
            textColor = 0xFFFFFFFF;
        } else {
            if (this.isHoveredOrFocused()) {
                fillColor = 0xFFE04444;
                borderColor = 0xFFFF8A8A;
            } else {
                fillColor = 0xFFAA2424;
                borderColor = 0xFFE45B5B;
            }
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
