package com.radig.vinylcraft.client.recorder;

import java.nio.file.Path;
import java.util.List;

import com.radig.vinylcraft.client.library.AudioMetadata;
import com.radig.vinylcraft.client.library.AudioMetadataCache;
import com.radig.vinylcraft.client.library.MusicLibraryEntry;
import com.radig.vinylcraft.client.library.MusicLibraryManager;
import com.radig.vinylcraft.client.library.MusicLibraryScreen;
import com.radig.vinylcraft.client.library.MusicLibraryTreeWidget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Paso 1 del grabador: selección y orden de pistas.
 *
 * La portada se eligirá en una segunda pantalla para evitar que ambas
 * tareas compitan por el mismo espacio y mantener la interfaz limpia.
 */
public class VinylRecorderScreen extends Screen {

    private static final int SIDE_MARGIN = 24;
    private static final int MAX_CONTENT_WIDTH = 1000;
    private static final int CONTENT_TOP = 42;
    private static final int BOTTOM_MARGIN = 42;
    private static final int PANEL_GAP = 10;
    private static final int ROW_HEIGHT = 16;

    private final VinylRecorderDraft draft;

    private MusicLibraryTreeWidget treeWidget;
    private Button addTrackButton;
    private Button removeTrackButton;
    private Button nextButton;

    private int trackScroll = 0;

    public VinylRecorderScreen(BlockPos recorderPos) {
        this(new VinylRecorderDraft(recorderPos));
    }

    VinylRecorderScreen(VinylRecorderDraft draft) {
        super(Component.literal("Grabador de vinilos"));
        this.draft = draft;
    }

    @Override
    protected void init() {

        MusicLibraryManager.refresh();

        Layout layout = getLayout();

        int contentHeight =
                Math.max(80, layout.contentBottom() - CONTENT_TOP);

        treeWidget = new MusicLibraryTreeWidget(
                layout.contentX(),
                CONTENT_TOP,
                layout.treeWidth(),
                contentHeight
        );

        this.addRenderableWidget(treeWidget);

        treeWidget.setSelectionChangedListener(
                this::updateButtons
        );

        int buttonGap = 5;
        int rowWidth = layout.panelWidth() - buttonGap * 2;
        int buttonWidth = Math.max(48, rowWidth / 3);
        int usedWidth = buttonWidth * 3 + buttonGap * 2;
        int rowX = layout.panelX()
                + Math.max(0, (layout.panelWidth() - usedWidth) / 2);

        addTrackButton = this.addRenderableWidget(
                new RecorderColorButton(
                        rowX,
                        CONTENT_TOP,
                        buttonWidth,
                        20,
                        Component.literal("+ Añadir"),
                        button -> addSelectedTrack(),
                        RecorderColorButton.Theme.GREEN
                )
        );

        removeTrackButton = this.addRenderableWidget(
                new RecorderColorButton(
                        rowX + buttonWidth + buttonGap,
                        CONTENT_TOP,
                        buttonWidth,
                        20,
                        Component.literal("- Quitar"),
                        button -> removeLastTrack(),
                        RecorderColorButton.Theme.RED
                )
        );

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Carpetas"),
                        button -> openLibraryManager()
                )
                .bounds(
                        rowX + (buttonWidth + buttonGap) * 2,
                        CONTENT_TOP,
                        buttonWidth,
                        20
                )
                .build()
        );

        int bottomY = this.height - 30;
        int actionWidth = Math.min(170, (layout.contentWidth() - 8) / 2);

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Cancelar"),
                        button -> this.onClose()
                )
                .bounds(
                        layout.contentX(),
                        bottomY,
                        actionWidth,
                        20
                )
                .build()
        );

        nextButton = this.addRenderableWidget(
                new RecorderColorButton(
                        layout.contentX()
                                + layout.contentWidth()
                                - actionWidth,
                        bottomY,
                        actionWidth,
                        20,
                        Component.literal("Siguiente >"),
                        button -> openCoverStep(),
                        RecorderColorButton.Theme.GREEN
                )
        );

        updateButtons();
    }

    private void openLibraryManager() {

        MusicLibraryManager.refresh();

        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(
                    new MusicLibraryScreen(this)
            );
        }
    }

    private void openCoverStep() {
        if (draft.selectedTracks().isEmpty()) {
            return;
        }

        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(
                    new VinylRecorderCoverScreen(draft)
            );
        }
    }

    private void addSelectedTrack() {

        if (treeWidget == null) {
            return;
        }

        MusicLibraryEntry entry =
                treeWidget.getSelectedEntry();

        if (entry == null || !entry.isAudioFile()) {
            return;
        }

        Path normalized = entry.path()
                .toAbsolutePath()
                .normalize();

        boolean alreadyAdded = draft.selectedTracks().stream()
                .map(track -> track.path().toAbsolutePath().normalize())
                .anyMatch(normalized::equals);

        if (!alreadyAdded) {
            draft.selectedTracks().add(entry);
            trackScroll = Math.max(0, draft.selectedTracks().size() - 1);
        }

        updateButtons();
    }

    private void removeLastTrack() {
        List<MusicLibraryEntry> tracks = draft.selectedTracks();

        if (!tracks.isEmpty()) {
            tracks.remove(tracks.size() - 1);
        }

        trackScroll = Math.min(
                trackScroll,
                Math.max(0, tracks.size() - 1)
        );

        updateButtons();
    }

    private void updateButtons() {

        MusicLibraryEntry entry =
                treeWidget == null
                        ? null
                        : treeWidget.getSelectedEntry();

        if (addTrackButton != null) {
            addTrackButton.active =
                    entry != null
                            && entry.isAudioFile();
        }

        if (removeTrackButton != null) {
            removeTrackButton.active = !draft.selectedTracks().isEmpty();
        }

        if (nextButton != null) {
            nextButton.active = !draft.selectedTracks().isEmpty();
        }
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount) {

        Layout layout = getLayout();

        if (
                mouseX >= layout.trackAreaX()
                        && mouseX < layout.trackAreaX() + layout.trackAreaWidth()
                        && mouseY >= layout.trackListTop()
                        && mouseY < layout.trackListBottom()
        ) {
            int visibleRows = getVisibleTrackRows(layout);
            int maxScroll =
                    Math.max(
                            0,
                            draft.selectedTracks().size() - visibleRows
                    );

            trackScroll -= (int) Math.signum(verticalAmount) * 3;
            trackScroll = Math.max(0, Math.min(maxScroll, trackScroll));
            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                horizontalAmount,
                verticalAmount
        );
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

        Layout layout = getLayout();

        graphics.centeredText(
                this.font,
                "Grabador de vinilos",
                this.width / 2,
                18,
                0xFFFFFFFF
        );

        graphics.fill(
                layout.panelX(),
                CONTENT_TOP,
                layout.panelX() + layout.panelWidth(),
                layout.contentBottom(),
                0x88000000
        );

        drawTrackSection(graphics, layout);

        if (MusicLibraryManager.getRootFolders().isEmpty()) {
            graphics.centeredText(
                    this.font,
                    "No hay carpetas de música configuradas. Usa Carpetas.",
                    layout.contentX() + layout.treeWidth() / 2,
                    CONTENT_TOP + 18,
                    0xFFFFAA55
            );
        }
    }

    private void drawTrackSection(
            GuiGraphicsExtractor graphics,
            Layout layout) {

        int x = layout.trackAreaX();
        int y = layout.panelTop() + 8;
        int maxWidth = Math.max(30, layout.trackAreaWidth());

        graphics.text(
                this.font,
                "Pistas seleccionadas: " + draft.selectedTracks().size(),
                x,
                y,
                0xFFFFFFFF,
                true
        );

        y = layout.trackListTop();

        if (draft.selectedTracks().isEmpty()) {
            graphics.text(
                    this.font,
                    "Añade canciones desde el árbol.",
                    x,
                    y,
                    0xFF888888,
                    false
            );
            return;
        }

        int visibleRows = getVisibleTrackRows(layout);
        int maxScroll =
                Math.max(
                        0,
                        draft.selectedTracks().size() - visibleRows
                );

        trackScroll = Math.max(0, Math.min(maxScroll, trackScroll));

        int end =
                Math.min(
                        draft.selectedTracks().size(),
                        trackScroll + visibleRows
                );

        for (int index = trackScroll; index < end; index++) {

            MusicLibraryEntry entry = draft.selectedTracks().get(index);
            AudioMetadata metadata = AudioMetadataCache.get(entry.path());

            String title =
                    metadata != null && metadata.hasTitle()
                            ? metadata.title()
                            : entry.name();

            String line = (index + 1) + ". " + title;

            line = this.font.plainSubstrByWidth(
                    line,
                    Math.max(20, maxWidth - 8)
            );

            graphics.text(
                    this.font,
                    line,
                    x,
                    y,
                    0xFFFFFFFF,
                    false
            );

            y += ROW_HEIGHT;
        }

        if (maxScroll > 0) {
            int barX = layout.trackAreaX() + layout.trackAreaWidth() - 3;
            int barTop = layout.trackListTop();
            int barBottom = layout.trackListBottom();
            int barHeight = barBottom - barTop;

            graphics.fill(
                    barX,
                    barTop,
                    barX + 2,
                    barBottom,
                    0x55333333
            );

            int thumbHeight =
                    Math.max(
                            12,
                            Math.round(
                                    barHeight
                                            * (visibleRows / (float) draft.selectedTracks().size())
                            )
                    );

            int thumbY =
                    barTop
                            + Math.round(
                                (barHeight - thumbHeight)
                                        * (trackScroll / (float) maxScroll)
                            );

            graphics.fill(
                    barX,
                    thumbY,
                    barX + 2,
                    thumbY + thumbHeight,
                    0xFFAAAAAA
            );
        }
    }

    private int getVisibleTrackRows(Layout layout) {
        return Math.max(
                1,
                (layout.trackListBottom() - layout.trackListTop())
                        / ROW_HEIGHT
        );
    }

    private int getContentWidth() {
        return Math.min(
                MAX_CONTENT_WIDTH,
                Math.max(
                        360,
                        this.width - SIDE_MARGIN * 2
                )
        );
    }

    private Layout getLayout() {
        int contentWidth = getContentWidth();
        int contentX = (this.width - contentWidth) / 2;
        int treeWidth = Math.max(180, (int) (contentWidth * 0.58F));
        int panelX = contentX + treeWidth + PANEL_GAP;
        int panelWidth = contentWidth - treeWidth - PANEL_GAP;
        int contentBottom = this.height - BOTTOM_MARGIN;
        int panelTop = CONTENT_TOP + 28;

        int innerPadding = 10;
        int trackAreaX = panelX + innerPadding;
        int trackAreaWidth = Math.max(80, panelWidth - innerPadding * 2);
        int trackListTop = panelTop + 28;
        int trackListBottom = Math.max(
                trackListTop + ROW_HEIGHT,
                contentBottom - 10
        );

        return new Layout(
                contentX,
                contentWidth,
                treeWidth,
                panelX,
                panelWidth,
                panelTop,
                contentBottom,
                trackAreaX,
                trackAreaWidth,
                trackListTop,
                trackListBottom
        );
    }

    private record Layout(
            int contentX,
            int contentWidth,
            int treeWidth,
            int panelX,
            int panelWidth,
            int panelTop,
            int contentBottom,
            int trackAreaX,
            int trackAreaWidth,
            int trackListTop,
            int trackListBottom) {
    }
}
