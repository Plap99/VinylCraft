package com.radig.vinylcraft.client.library;

import java.nio.file.Path;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MusicLibraryScreen extends Screen {

    private Path selectedFolder;
    private MusicLibraryTreeWidget treeWidget;
    private Button removeButton;
    private AudioMetadata selectedMetadata;
    
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

        int contentWidth =
                Math.min(
                        620,
                        this.width - 40
                );

        int contentX =
                centerX - contentWidth / 2;

        int treeWidth;

        if (contentWidth >= 500) {
            treeWidth =
                    (int) (contentWidth * 0.58);
        } else {
            treeWidth =
                    contentWidth;
        }

        int treeTop = 90;

        int bottomMargin = 18;
        int buttonHeight = 20;
        int gapAboveButtons = 8;

        int buttonsY =
                this.height
                - bottomMargin
                - buttonHeight;

        int treeBottom =
                buttonsY
                - gapAboveButtons;

        int treeHeight =
                treeBottom
                - treeTop;

        treeWidget =
                new MusicLibraryTreeWidget(
                        contentX,
                        treeTop,
                        treeWidth,
                        treeHeight
                );

        this.addRenderableWidget(treeWidget);

        treeWidget.setSelectionChangedListener(
                this::handleTreeSelectionChanged
        );

        removeButton =
                Button.builder(
                        Component.literal("− Quitar carpeta"),
                        button -> removeSelectedFolder()
                )
                .bounds(
                        centerX - 155,
                        buttonsY,
                        145,
                        buttonHeight
                )
                .build();

        updateRemoveButton();

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
                        buttonsY,
                        145,
                        buttonHeight
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

        if (treeWidget == null) {
            return;
        }

        Path selected =
                treeWidget.getSelectedPath();

        if (selected == null) {
            return;
        }

        Path normalizedSelected =
                selected.toAbsolutePath().normalize();

        boolean isRootFolder =
                MusicLibraryManager
                        .getRootFolders()
                        .stream()
                        .map(path ->
                                path.toAbsolutePath()
                                        .normalize()
                        )
                        .anyMatch(
                                normalizedSelected::equals
                        );

        if (!isRootFolder) {
            return;
        }

        MusicLibraryManager.removeRootFolder(
                normalizedSelected
        );

        MusicLibraryConfig.save();

        this.rebuildWidgets();
    }

    private void updateRemoveButton() {

        if (
                removeButton == null
                || treeWidget == null
        ) {
            return;
        }

        Path selected =
                treeWidget.getSelectedPath();

        boolean isRootFolder = false;

        if (selected != null) {

            Path normalizedSelected =
                    selected.toAbsolutePath().normalize();

            for (
                    Path root :
                    MusicLibraryManager.getRootFolders()
            ) {

                if (
                        root.toAbsolutePath()
                                .normalize()
                                .equals(normalizedSelected)
                ) {

                    isRootFolder = true;
                    break;
                }
            }
        }

        removeButton.active =
                isRootFolder;
    }

    private void handleTreeSelectionChanged() {

        updateRemoveButton();

        selectedMetadata = null;

        if (treeWidget == null) {
            return;
        }

        MusicLibraryEntry entry =
                treeWidget.getSelectedEntry();

        if (
                entry == null
                || !entry.isAudioFile()
        ) {
            return;
        }

        selectedMetadata =
                AudioMetadataCache.get(
                        entry.path()
                );

        if (selectedMetadata != null) {

            System.out.println(
                    "[VinylCraft] Metadata seleccionada:"
            );

            System.out.println(
                    "  Título: "
                            + selectedMetadata.title()
            );

            System.out.println(
                    "  Artista: "
                            + selectedMetadata.artist()
            );

            System.out.println(
                    "  Álbum: "
                            + selectedMetadata.album()
            );

            System.out.println(
                    "  Duración: "
                            + selectedMetadata.formattedDuration()
            );
        }
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

        if (selectedMetadata != null) {

            int contentWidth =
                    Math.min(
                            620,
                            this.width - 40
                    );

            int contentX =
                    centerX - contentWidth / 2;

            int treeWidth =
                    contentWidth >= 500
                            ? (int) (contentWidth * 0.58)
                            : contentWidth;

            int panelX =
                    contentX
                    + treeWidth
                    + 15;

            int panelY = 100;


            int panelWidth =
                    contentWidth
                    - treeWidth
                    - 15;

            if (panelWidth <= 0) {
                return;
            }

            graphics.text(
                    this.font,
                    selectedMetadata.title(),
                    panelX,
                    panelY,
                    0xFFFFFFFF,
                    true
            );

            graphics.text(
                    this.font,
                    selectedMetadata.artist(),
                    panelX,
                    panelY + 18,
                    0xFFAAAAAA,
                    true
            );

            graphics.text(
                    this.font,
                    "Álbum: " + selectedMetadata.album(),
                    panelX,
                    panelY + 45,
                    0xFFCCCCCC,
                    true
            );

            graphics.text(
                    this.font,
                    "Año: " + selectedMetadata.year(),
                    panelX,
                    panelY + 63,
                    0xFFCCCCCC,
                    true
            );

            graphics.text(
                    this.font,
                    "Pista: " + selectedMetadata.trackNumber(),
                    panelX,
                    panelY + 81,
                    0xFFCCCCCC,
                    true
            );

            graphics.text(
                    this.font,
                    "Duración: "
                            + selectedMetadata.formattedDuration(),
                    panelX,
                    panelY + 99,
                    0xFFCCCCCC,
                    true
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