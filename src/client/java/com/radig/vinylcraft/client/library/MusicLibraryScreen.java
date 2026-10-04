package com.radig.vinylcraft.client.library;

import java.nio.file.Path;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MusicLibraryScreen extends Screen {

    private Path selectedFolder;

    public MusicLibraryScreen() {
        super(Component.literal("Biblioteca VinylCraft"));
    }

    @Override
    protected void init() {

        int centerX = this.width / 2;

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("+ Agregar carpeta"),
                        button -> addFolder()
                )
                .bounds(
                        centerX - 155,
                        55,
                        145,
                        20
                )
                .build()
        );

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("↻ Actualizar"),
                        button -> {
                            MusicLibraryManager.refresh();
                            this.rebuildWidgets();
                        }
                )
                .bounds(
                        centerX + 10,
                        55,
                        145,
                        20
                )
                .build()
        );

        List<Path> folders =
                MusicLibraryManager.getRootFolders();

        int y = 95;

        for (Path folder : folders) {

            Path currentFolder = folder;

            String displayName =
                    getFolderDisplayName(currentFolder);

            boolean selected =
                    currentFolder.equals(selectedFolder);

            String buttonText =
                    selected
                            ? "▶ " + displayName
                            : displayName;

            this.addRenderableWidget(
                    Button.builder(
                            Component.literal(buttonText),
                            button -> {
                                selectedFolder =
                                        currentFolder;

                                this.rebuildWidgets();
                            }
                    )
                    .bounds(
                            centerX - 155,
                            y,
                            310,
                            20
                    )
                    .build()
            );

            y += 24;
        }

        Button removeButton =
                Button.builder(
                        Component.literal("− Quitar carpeta"),
                        button -> removeSelectedFolder()
                )
                .bounds(
                        centerX - 155,
                        this.height - 65,
                        145,
                        20
                )
                .build();

        removeButton.active =
                selectedFolder != null;

        this.addRenderableWidget(
                removeButton
        );

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Cerrar"),
                        button -> this.onClose()
                )
                .bounds(
                        centerX + 10,
                        this.height - 65,
                        145,
                        20
                )
                .build()
        );
    }

    private void addFolder() {

        Path folder =
                NativeFolderPicker.pickFolder();

        if (folder == null) {
            return;
        }

        MusicLibraryManager.addRootFolder(
                folder
        );

        MusicLibraryConfig.save();

        selectedFolder =
                folder.toAbsolutePath().normalize();

        this.rebuildWidgets();
    }

    private void removeSelectedFolder() {

        if (selectedFolder == null) {
            return;
        }

        MusicLibraryManager.removeRootFolder(
                selectedFolder
        );

        MusicLibraryConfig.save();

        selectedFolder = null;

        this.rebuildWidgets();
    }

    private String getFolderDisplayName(
            Path folder) {

        if (folder == null) {
            return "";
        }

        Path fileName =
                folder.getFileName();

        if (fileName != null) {
            return fileName.toString();
        }

        return folder.toString();
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta) {

        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta
        );

        int centerX = this.width / 2;

        graphics.centeredText(
                this.font,
                "Biblioteca VinylCraft",
                centerX,
                20,
                0xFFFFFFFF
        );

        graphics.centeredText(
                this.font,
                "Carpetas de música",
                centerX,
                37,
                0xFFAAAAAA
        );

        if (
                MusicLibraryManager
                        .getRootFolders()
                        .isEmpty()
        ) {

            graphics.centeredText(
                    this.font,
                    "Todavía no hay carpetas configuradas",
                    centerX,
                    105,
                    0xFF888888
            );
        }

        if (selectedFolder != null) {

            graphics.centeredText(
                    this.font,
                    selectedFolder.toString(),
                    centerX,
                    this.height - 88,
                    0xFFAAAAAA
            );
        }
    }

    @Override
    public void onClose() {

        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(null);
        }
    }
}