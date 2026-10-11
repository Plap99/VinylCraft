package com.radig.vinylcraft.client.discman;

import com.radig.vinylcraft.client.sound.DiscmanSoundManager;
import com.radig.vinylcraft.item.DiscmanData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Barra compacta de volumen propia del Discman (0-100%). */
public final class DiscmanVolumeSlider extends AbstractSliderButton {

    private boolean syncing;

    public DiscmanVolumeSlider(
            int x,
            int y,
            int width,
            int height) {

        super(
                x,
                y,
                width,
                height,
                Component.empty(),
                1.0D
        );

        refreshFromEquippedDiscman();
    }

    @Override
    protected void updateMessage() {
        int percent = (int) Math.round(this.value * 100.0D);
        setMessage(Component.literal("Volumen " + percent + "%"));
    }

    @Override
    protected void applyValue() {
        if (syncing) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {
            return;
        }

        ItemStack discman = DiscmanData.findEquipped(minecraft.player);

        if (discman.isEmpty()) {
            return;
        }

        int percent = (int) Math.round(this.value * 100.0D);

        // Respuesta inmediata en cliente mientras llega la sincronización.
        DiscmanData.setVolumePercent(discman, percent);
        DiscmanSoundManager.sendVolume(percent);
    }

    @Override
    public void extractWidgetRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta) {

        int left = getX();
        int top = getY();
        int right = getRight();
        int bottom = getBottom();

        int background = active ? 0xFF181818 : 0xFF262626;
        int fill = active ? 0xFF4AAE68 : 0xFF555555;
        int knob = active ? 0xFFF2F2F2 : 0xFF777777;
        int textColor = active ? 0xFFFFFFFF : 0xFF9A9A9A;

        graphics.fill(left, top, right, bottom, background);

        int innerWidth = Math.max(1, getWidth() - 2);
        int filled = Math.max(
                0,
                Math.min(
                        innerWidth,
                        (int) Math.round(innerWidth * this.value)
                )
        );

        if (filled > 0) {
            graphics.fill(
                    left + 1,
                    top + 1,
                    left + 1 + filled,
                    bottom - 1,
                    fill
            );
        }

        int knobX = left + filled;
        knobX = Math.max(left, Math.min(right - 1, knobX));

        graphics.fill(
                knobX,
                top,
                knobX + 1,
                bottom,
                knob
        );

        vinylcraft$drawTinyLabel(
                graphics,
                left,
                top,
                getWidth(),
                getHeight(),
                (int) Math.round(this.value * 100.0D) + "%",
                textColor
        );
    }

    private void vinylcraft$drawTinyLabel(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            String text,
            int color) {

        int textWidth = vinylcraft$getTinyTextWidth(text);
        int drawX = x + Math.max(0, (width - textWidth) / 2);
        int drawY = y + Math.max(0, (height - 5) / 2);

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            vinylcraft$drawTinyGlyph(graphics, drawX, drawY, ch, color);
            drawX += vinylcraft$getTinyGlyphWidth(ch) + 1;
        }
    }

    private int vinylcraft$getTinyTextWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            if (i > 0) width += 1;
            width += vinylcraft$getTinyGlyphWidth(text.charAt(i));
        }
        return width;
    }

    private int vinylcraft$getTinyGlyphWidth(char ch) {
        return ch == '%' ? 4 : 3;
    }

    private void vinylcraft$drawTinyGlyph(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            char ch,
            int color) {

        String[] rows = switch (ch) {
            case '0' -> new String[]{"111", "101", "101", "101", "111"};
            case '1' -> new String[]{"010", "110", "010", "010", "111"};
            case '2' -> new String[]{"111", "001", "111", "100", "111"};
            case '3' -> new String[]{"111", "001", "111", "001", "111"};
            case '4' -> new String[]{"101", "101", "111", "001", "001"};
            case '5' -> new String[]{"111", "100", "111", "001", "111"};
            case '6' -> new String[]{"111", "100", "111", "101", "111"};
            case '7' -> new String[]{"111", "001", "001", "001", "001"};
            case '8' -> new String[]{"111", "101", "111", "101", "111"};
            case '9' -> new String[]{"111", "101", "111", "001", "111"};
            case '%' -> new String[]{"1001", "0001", "0010", "1000", "1001"};
            default -> new String[]{"000", "000", "000", "000", "000"};
        };

        for (int row = 0; row < rows.length; row++) {
            String pattern = rows[row];
            for (int col = 0; col < pattern.length(); col++) {
                if (pattern.charAt(col) == '1') {
                    graphics.fill(x + col, y + row, x + col + 1, y + row + 1, color);
                }
            }
        }
    }

    public void refreshFromEquippedDiscman() {
        Minecraft minecraft = Minecraft.getInstance();

        ItemStack discman = minecraft.player == null
                ? ItemStack.EMPTY
                : DiscmanData.findEquipped(minecraft.player);

        this.active = !discman.isEmpty();

        int percent = discman.isEmpty()
                ? 100
                : DiscmanData.getVolumePercent(discman);

        double target = Math.max(0.0D, Math.min(1.0D, percent / 100.0D));

        if (Math.abs(target - this.value) > 0.0001D) {
            syncing = true;
            this.value = target;
            updateMessage();
            syncing = false;
        } else {
            updateMessage();
        }
    }
}
