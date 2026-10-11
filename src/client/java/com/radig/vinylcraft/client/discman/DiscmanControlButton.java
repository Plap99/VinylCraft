package com.radig.vinylcraft.client.discman;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Botón compacto coloreado para los controles del Discman en el inventario. */
public final class DiscmanControlButton extends Button {

    public enum Theme {
        BLUE,
        RED,
        GREEN,
        YELLOW
    }

    private Theme theme;

    public DiscmanControlButton(
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

    public void setTheme(Theme theme) {
        if (theme != null) {
            this.theme = theme;
        }
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
            fillColor = 0xCC343434;
            borderColor = 0xFF555555;
            textColor = 0xFF8F8F8F;
        } else {
            boolean hover = this.isHoveredOrFocused();

            switch (theme) {
                case RED -> {
                    fillColor = hover ? 0xFFE04444 : 0xFFAA2424;
                    borderColor = hover ? 0xFFFF8A8A : 0xFFE45B5B;
                }
                case GREEN -> {
                    fillColor = hover ? 0xFF35A957 : 0xFF23773D;
                    borderColor = hover ? 0xFF83E39B : 0xFF55B76F;
                }
                case YELLOW -> {
                    fillColor = hover ? 0xFFE2B938 : 0xFFB48D1C;
                    borderColor = hover ? 0xFFFFDF75 : 0xFFE2BD45;
                }
                default -> {
                    fillColor = hover ? 0xFF3B8CC9 : 0xFF28669A;
                    borderColor = hover ? 0xFF83C8F4 : 0xFF5FA3D5;
                }
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

        /*
         * Pausa se dibuja como icono real para evitar que la fuente de
         * Minecraft la convierta visualmente en un "Ⅱ" extraño.
         */
        if (theme == Theme.YELLOW) {
            int centerX = getX() + getWidth() / 2;
            int iconTop = getY() + Math.max(2, (getHeight() - 5) / 2);

            graphics.fill(
                    centerX - 3,
                    iconTop,
                    centerX - 1,
                    iconTop + 5,
                    textColor
            );

            graphics.fill(
                    centerX + 1,
                    iconTop,
                    centerX + 3,
                    iconTop + 5,
                    textColor
            );

            return;
        }

        var font = Minecraft.getInstance().font;
        int textY = getY() + Math.max(0, (getHeight() - font.lineHeight) / 2) + 1;
        String symbol = getMessage().getString();
        int textX = getX() + getWidth() / 2;

        /* STOP ya estaba bien; conservamos exactamente su ajuste. */
        if ("■".equals(symbol)) {
            textX += 1;
        }

        graphics.centeredText(
                font,
                getMessage(),
                textX,
                textY,
                textColor
        );
    }
}
