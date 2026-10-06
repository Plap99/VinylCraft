package com.radig.vinylcraft.client.library;

import java.nio.file.Path;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public class MusicLibraryScreen extends Screen {

    private static final int SIDE_MARGIN = 24;
    private static final int MAX_CONTENT_WIDTH = 1000;

    private static final int TWO_COLUMN_MIN_WIDTH = 360;

    private static final int BUTTON_TOP = 55;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 6;

    private static final int CONTENT_TOP = 90;
    private static final int CONTENT_BOTTOM_MARGIN = 18;
    private static final int PANEL_GAP = 10;

    private MusicLibraryTreeWidget treeWidget;
    private Button removeButton;

    private AudioMetadata selectedMetadata;
    private AlbumCoverResolver.CoverResult selectedCover;

    private final Screen returnScreen;

    public MusicLibraryScreen() {
        this(null);
    }

    public MusicLibraryScreen(Screen returnScreen) {
        super(Component.literal("Biblioteca VinylCraft"));
        this.returnScreen = returnScreen;
    }

    @Override
    protected void init() {

        int centerX = this.width / 2;

        int contentWidth =
                getContentWidth();

        int contentX =
                centerX - contentWidth / 2;

        /*
         * ─────────────────────────────────────
         * Barra superior
         * ─────────────────────────────────────
         */

        int buttonWidth =
                (contentWidth - BUTTON_GAP * 3) / 4;

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("+ Agregar carpeta"),
                        button -> addFolder()
                )
                .bounds(
                        contentX,
                        BUTTON_TOP,
                        buttonWidth,
                        BUTTON_HEIGHT
                )
                .build()
        );

        removeButton =
                Button.builder(
                        Component.literal("- Quitar carpeta"),
                        button -> removeSelectedFolder()
                )
                .bounds(
                        contentX
                                + buttonWidth
                                + BUTTON_GAP,
                        BUTTON_TOP,
                        buttonWidth,
                        BUTTON_HEIGHT
                )
                .build();

        this.addRenderableWidget(
                removeButton
        );

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Actualizar"),
                        button -> {
                            MusicLibraryManager.refresh();
                            this.rebuildWidgets();
                        }
                )
                .bounds(
                        contentX
                                + (buttonWidth + BUTTON_GAP) * 2,
                        BUTTON_TOP,
                        buttonWidth,
                        BUTTON_HEIGHT
                )
                .build()
        );

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Cerrar"),
                        button -> this.onClose()
                )
                .bounds(
                        contentX
                                + (buttonWidth + BUTTON_GAP) * 3,
                        BUTTON_TOP,
                        buttonWidth,
                        BUTTON_HEIGHT
                )
                .build()
        );

        /*
         * ─────────────────────────────────────
         * Área principal
         *
         *      60 % árbol
         *      40 % información
         * ─────────────────────────────────────
         */

        int treeWidth =
                getTreeWidth(contentWidth);

        int contentBottom =
                this.height
                        - CONTENT_BOTTOM_MARGIN;

        int contentHeight =
                Math.max(
                        40,
                        contentBottom
                                - CONTENT_TOP
                );

        treeWidget =
                new MusicLibraryTreeWidget(
                        contentX,
                        CONTENT_TOP,
                        treeWidth,
                        contentHeight
                );

        this.addRenderableWidget(
                treeWidget
        );

        treeWidget.setSelectionChangedListener(
                this::handleTreeSelectionChanged
        );

        updateRemoveButton();
    }

    /*
     * ─────────────────────────────────────────
     * Carpetas
     * ─────────────────────────────────────────
     */

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
                selected
                        .toAbsolutePath()
                        .normalize();

        boolean isRootFolder =
                MusicLibraryManager
                        .getRootFolders()
                        .stream()
                        .map(path ->
                                path
                                        .toAbsolutePath()
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

        selectedMetadata = null;

        this.rebuildWidgets();
    }

    /*
     * ─────────────────────────────────────────
     * Selección
     * ─────────────────────────────────────────
     */

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
                    selected
                            .toAbsolutePath()
                            .normalize();

            for (
                    Path root :
                    MusicLibraryManager.getRootFolders()
            ) {

                if (
                        root
                                .toAbsolutePath()
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
        selectedCover = null;

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

        selectedCover =
                AlbumCoverResolver.resolve(
                        entry.path(),
                        selectedMetadata
                );

        if (selectedCover != null) {

            if (selectedCover.isEmbedded()) {

                System.out.println(
                        "[VinylCraft] Portada: EMBEBIDA ("
                                + selectedCover.data().length
                                + " bytes)"
                );

            } else {

                System.out.println(
                        "[VinylCraft] Portada: "
                                + selectedCover.path()
                );
            }

        } else {

            System.out.println(
                    "[VinylCraft] Portada: no encontrada"
            );
        }
    }

    /*
     * ─────────────────────────────────────────
     * Layout
     * ─────────────────────────────────────────
     */

    private int getContentWidth() {

        return Math.min(
                MAX_CONTENT_WIDTH,
                Math.max(
                        320,
                        this.width
                                - SIDE_MARGIN * 2
                )
        );
    }

    private int getTreeWidth(
            int contentWidth) {

        if (contentWidth < TWO_COLUMN_MIN_WIDTH) {
            return contentWidth;
        }

        return (int) (
                contentWidth * 0.60F
        );
    }

    /*
     * ─────────────────────────────────────────
     * Render
     * ─────────────────────────────────────────
     */

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

        int centerX =
                this.width / 2;

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

        int contentWidth =
                getContentWidth();

        int contentX =
                centerX
                        - contentWidth / 2;

        int treeWidth =
                getTreeWidth(
                        contentWidth
                );

        int contentBottom =
                this.height
                        - CONTENT_BOTTOM_MARGIN;

        /*
        * Si la ventana permite dos columnas,
        * dibujamos una división visible 60 / 40.
        */

        if (contentWidth >= TWO_COLUMN_MIN_WIDTH) {

            int dividerX =
                    contentX
                    + treeWidth
                    + PANEL_GAP / 2;

            /*
            * Línea divisoria
            */

            graphics.fill(
                    dividerX,
                    CONTENT_TOP + 6,
                    dividerX + 1,
                    contentBottom - 6,
                    0xFF777777
            );

            int panelX =
                    contentX
                    + treeWidth
                    + PANEL_GAP;

            int panelWidth =
                    contentWidth
                    - treeWidth
                    - PANEL_GAP;

            drawMetadataPanel(
                    graphics,
                    panelX,
                    CONTENT_TOP,
                    panelWidth,
                    contentBottom
            );
        }

        if (
                MusicLibraryManager
                        .getRootFolders()
                        .isEmpty()
        ) {

            int emptyMessageX =
                    contentX
                    + treeWidth / 2;

            graphics.centeredText(
                    this.font,
                    "Todavía no hay carpetas configuradas",
                    emptyMessageX,
                    CONTENT_TOP + 15,
                    0xFF888888
            );
        }

        /*
         * Biblioteca vacía
         */

        if (
                MusicLibraryManager
                        .getRootFolders()
                        .isEmpty()
        ) {

            int messageX;

            int panelX =
                    contentX
                    + treeWidth
                    + PANEL_GAP;

            int panelWidth =
                    contentWidth
                    - treeWidth
                    - PANEL_GAP;

            /*
            * Panel derecho
            */

            graphics.fill(
                    panelX,
                    CONTENT_TOP,
                    panelX + panelWidth,
                    contentBottom,
                    0x99000000
            );

            /*
            * Separador vertical.
            */

            int dividerX =
                    contentX
                    + treeWidth
                    + PANEL_GAP / 2;

            graphics.fill(
                    dividerX,
                    CONTENT_TOP,
                    dividerX + 2,
                    contentBottom,
                    0xFFAAAAAA
            );

            /*
            * Título temporal para comprobar
            * visualmente la segunda columna.
            */

            graphics.centeredText(
                    this.font,
                    "Información",
                    panelX + panelWidth / 2,
                    CONTENT_TOP + 10,
                    0xFFFFFFFF
            );

            drawMetadataPanel(
                    graphics,
                    panelX,
                    CONTENT_TOP + 22,
                    panelWidth,
                    contentBottom
            );
        }
    }

    /*
     * ─────────────────────────────────────────
     * Panel derecho
     * ─────────────────────────────────────────
     */

    private void drawMetadataPanel(
            GuiGraphicsExtractor graphics,
            int panelX,
            int panelTop,
            int panelWidth,
            int panelBottom) {

        int padding = 12;

        int x =
                panelX + padding;

        int y =
                panelTop + padding;

        int usableWidth =
                panelWidth
                        - padding * 2;

        if (usableWidth <= 20) {
            return;
        }

        if (selectedMetadata == null) {

            graphics.centeredText(
                    this.font,
                    "Selecciona una canción",
                    panelX + panelWidth / 2,
                    y + 10,
                    0xFF888888
            );

            return;
        }

        Identifier coverTexture = null;

        if (selectedCover != null) {

            coverTexture =
                    AlbumCoverTextureManager.getTexture(
                            selectedCover
                    );
        }

        /*
        * Altura disponible dentro del panel.
        */
        int availablePanelHeight =
                panelBottom - y;

        /*
        * Si hay suficiente altura usamos portada grande.
        * Si no, usamos portada compacta junto al título.
        */
        boolean largeCoverMode =
                coverTexture != null
                        && availablePanelHeight >= 190;

        /*
        * ─────────────────────────────
        * PORTADA GRANDE
        * ─────────────────────────────
        */

        if (largeCoverMode) {

            int maxCoverSize =
                    Math.min(
                            usableWidth,
                            90
                    );

            int drawWidth = maxCoverSize;
            int drawHeight = maxCoverSize;

            AlbumCoverTextureManager.CoverSize sourceSize =
                    AlbumCoverTextureManager.getSize(
                            coverTexture
                    );

            if (sourceSize != null) {

                if (sourceSize.width() >= sourceSize.height()) {

                    drawHeight =
                            Math.max(
                                    1,
                                    Math.round(
                                            maxCoverSize
                                                    * (
                                                        sourceSize.height()
                                                        / (float) sourceSize.width()
                                                    )
                                    )
                            );

                } else {

                    drawWidth =
                            Math.max(
                                    1,
                                    Math.round(
                                            maxCoverSize
                                                    * (
                                                        sourceSize.width()
                                                        / (float) sourceSize.height()
                                                    )
                                    )
                            );
                }
            }

            int coverX =
                    panelX
                            + (panelWidth - drawWidth) / 2;

            int coverY = y;

            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    coverTexture,
                    coverX,
                    coverY,
                    0.0F,
                    0.0F,
                    drawWidth,
                    drawHeight,
                    drawWidth,
                    drawHeight
            );

            y +=
                    drawHeight + 10;
        }

        /*
        * ─────────────────────────────
        * PORTADA COMPACTA
        * ─────────────────────────────
        */

        int textWidth =
                usableWidth;

        int topTextWidth =
                usableWidth;

        int compactCoverSize = 0;

        if (
                coverTexture != null
                        && !largeCoverMode
        ) {

            compactCoverSize = 42;

            int drawWidth =
                    compactCoverSize;

            int drawHeight =
                    compactCoverSize;

            AlbumCoverTextureManager.CoverSize sourceSize =
                    AlbumCoverTextureManager.getSize(
                            coverTexture
                    );

            if (sourceSize != null) {

                if (
                        sourceSize.width()
                                >= sourceSize.height()
                ) {

                    drawHeight =
                            Math.max(
                                    1,
                                    Math.round(
                                            compactCoverSize
                                                    * (
                                                        sourceSize.height()
                                                        / (float) sourceSize.width()
                                                    )
                                    )
                            );

                } else {

                    drawWidth =
                            Math.max(
                                    1,
                                    Math.round(
                                            compactCoverSize
                                                    * (
                                                        sourceSize.width()
                                                        / (float) sourceSize.height()
                                                    )
                                    )
                            );
                }
            }

            int coverX =
                    panelX
                            + panelWidth
                            - padding
                            - drawWidth;

            int coverY =
                    y
                            + (
                                compactCoverSize
                                - drawHeight
                            ) / 2;

            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    coverTexture,
                    coverX,
                    coverY,
                    0.0F,
                    0.0F,
                    drawWidth,
                    drawHeight,
                    drawWidth,
                    drawHeight
            );

            topTextWidth =
                    Math.max(
                            30,
                            usableWidth
                                    - compactCoverSize
                                    - 6
                    );
        }

        /*
        * ─────────────────────────────
        * TÍTULO
        * ─────────────────────────────
        */

        String title =
                fitText(
                        selectedMetadata.title(),
                        topTextWidth
                );

        graphics.text(
                this.font,
                title,
                x,
                y,
                0xFFFFFFFF,
                true
        );

        y += 18;

        /*
        * ARTISTA
        */

        String artist =
                fitText(
                        selectedMetadata.artist(),
                        topTextWidth
                );

        graphics.text(
                this.font,
                artist,
                x,
                y,
                0xFFAAAAAA,
                true
        );

        /*
        * En modo compacto esperamos a que termine
        * el espacio ocupado por la portada.
        */

        if (compactCoverSize > 0) {

            y =
                    panelTop
                            + padding
                            + compactCoverSize
                            + 6;

        } else {

            y += 28;
        }

        /*
        * ─────────────────────────────
        * METADATOS
        * ─────────────────────────────
        */

        drawMetadataLine(
                graphics,
                "Álbum: ",
                selectedMetadata.album(),
                x,
                y,
                textWidth
        );

        y += 18;

        drawMetadataLine(
                graphics,
                "Año: ",
                String.valueOf(
                        selectedMetadata.year()
                ),
                x,
                y,
                textWidth
        );

        y += 18;

        drawMetadataLine(
                graphics,
                "Pista: ",
                String.valueOf(
                        selectedMetadata.trackNumber()
                ),
                x,
                y,
                textWidth
        );

        y += 18;

        drawMetadataLine(
                graphics,
                "Duración: ",
                selectedMetadata.formattedDuration(),
                x,
                y,
                textWidth
        );
    }

    private void drawMetadataLine(
            GuiGraphicsExtractor graphics,
            String label,
            String value,
            int x,
            int y,
            int width) {

        String text =
                label
                        + safeText(value);

        graphics.text(
                this.font,
                fitText(
                        text,
                        width
                ),
                x,
                y,
                0xFFCCCCCC,
                true
        );
    }

    /*
     * Recorta únicamente la representación visual.
     * Nunca modifica metadata ni nombres reales.
     */

    private String fitText(
            String text,
            int width) {

        String safe =
                safeText(text);

        if (
                width <= 0
                        || this.font.width(safe)
                        <= width
        ) {
            return safe;
        }

        String dots = "...";

        int dotsWidth =
                this.font.width(dots);

        if (width <= dotsWidth) {
            return "";
        }

        return this.font.plainSubstrByWidth(
                safe,
                width - dotsWidth
        ) + dots;
    }

    private String safeText(
            String text) {

        if (
                text == null
                        || text.isBlank()
        ) {
            return "Desconocido";
        }

        return text;
    }

    @Override
    public void onClose() {

        MusicLibraryManager.refresh();

        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(returnScreen);
        }
    }
}