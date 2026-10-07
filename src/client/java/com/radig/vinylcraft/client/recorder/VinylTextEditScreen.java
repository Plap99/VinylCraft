package com.radig.vinylcraft.client.recorder;

import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Editor pequeño reutilizable para corregir título, artista o nombre de pista. */
public final class VinylTextEditScreen extends Screen {

    private final Screen parent;
    private final String label;
    private final String initialValue;
    private final Consumer<String> onSave;

    private EditBox textBox;

    public VinylTextEditScreen(
            Screen parent,
            String label,
            String initialValue,
            Consumer<String> onSave) {

        super(Component.literal(label));
        this.parent = parent;
        this.label = label;
        this.initialValue = initialValue == null ? "" : initialValue;
        this.onSave = onSave;
    }

    @Override
    protected void init() {
        int boxWidth = Math.min(360, Math.max(220, width - 80));
        int x = (width - boxWidth) / 2;
        int y = height / 2 - 10;

        textBox = addRenderableWidget(
                new EditBox(
                        font,
                        x,
                        y,
                        boxWidth,
                        20,
                        Component.literal(label)
                )
        );

        textBox.setMaxLength(256);
        textBox.setValue(initialValue);
        textBox.setCursorPosition(textBox.getValue().length());
        setInitialFocus(textBox);

        int buttonWidth = Math.min(120, (boxWidth - 8) / 2);

        addRenderableWidget(
                Button.builder(
                        Component.literal("Cancelar"),
                        button -> closeToParent()
                )
                .bounds(
                        x,
                        y + 34,
                        buttonWidth,
                        20
                )
                .build()
        );

        addRenderableWidget(
                new RecorderColorButton(
                        x + boxWidth - buttonWidth,
                        y + 34,
                        buttonWidth,
                        20,
                        Component.literal("Guardar"),
                        button -> save(),
                        RecorderColorButton.Theme.GREEN
                )
        );
    }

    private void save() {
        if (textBox == null) {
            return;
        }

        String value = textBox.getValue() == null
                ? ""
                : textBox.getValue().trim();

        if (value.isBlank()) {
            return;
        }

        if (onSave != null) {
            onSave.accept(value);
        }

        closeToParent();
    }

    private void closeToParent() {
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        closeToParent();
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta) {

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.centeredText(
                font,
                label,
                width / 2,
                height / 2 - 42,
                0xFFFFFFFF
        );
    }
}
