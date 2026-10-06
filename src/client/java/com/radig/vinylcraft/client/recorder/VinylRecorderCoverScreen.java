package com.radig.vinylcraft.client.recorder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.radig.vinylcraft.client.library.AlbumCoverResolver;
import com.radig.vinylcraft.client.library.AlbumCoverTextureManager;
import com.radig.vinylcraft.client.library.AudioMetadata;
import com.radig.vinylcraft.client.library.AudioMetadataCache;
import com.radig.vinylcraft.client.library.MusicLibraryEntry;
import com.radig.vinylcraft.client.library.MusicLibraryManager;
import com.radig.vinylcraft.client.library.MusicLibraryTreeWidget;
import com.radig.vinylcraft.client.music.RecordedAlbumStore;
import com.radig.vinylcraft.music.AlbumData;
import com.radig.vinylcraft.music.TrackData;
import com.radig.vinylcraft.network.StartVinylRecordingPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Paso 2 del grabador: selección de portada y confirmación final. */
public class VinylRecorderCoverScreen extends Screen {

    private static final int SIDE_MARGIN = 24;
    private static final int MAX_CONTENT_WIDTH = 1000;
    private static final int CONTENT_TOP = 42;
    private static final int BOTTOM_MARGIN = 42;
    private static final int PANEL_GAP = 10;

    private final VinylRecorderDraft draft;

    private MusicLibraryTreeWidget treeWidget;
    private Button useCoverButton;
    private Button removeCoverButton;
    private Button recordButton;

    private AlbumCoverResolver.CoverResult candidateCoverPreview;

    VinylRecorderCoverScreen(VinylRecorderDraft draft) {
        super(Component.literal("Grabador de vinilos - Portada"));
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
                this::handleTreeSelectionChanged
        );

        int buttonGap = 6;
        int buttonWidth = Math.max(72, (layout.panelWidth() - buttonGap - 20) / 2);
        int buttonsX = layout.panelX() + 10;
        int buttonsY = layout.coverButtonsY();

        useCoverButton = this.addRenderableWidget(
                new RecorderColorButton(
                        buttonsX,
                        buttonsY,
                        buttonWidth,
                        20,
                        Component.literal("+ Elegir"),
                        button -> selectCoverFromTree(),
                        RecorderColorButton.Theme.GREEN
                )
        );

        removeCoverButton = this.addRenderableWidget(
                new RecorderColorButton(
                        buttonsX + buttonWidth + buttonGap,
                        buttonsY,
                        buttonWidth,
                        20,
                        Component.literal("- Quitar"),
                        button -> removeCover(),
                        RecorderColorButton.Theme.RED
                )
        );

        int bottomY = this.height - 30;
        int actionWidth = Math.min(170, (layout.contentWidth() - 8) / 2);

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("< Atrás"),
                        button -> goBack()
                )
                .bounds(
                        layout.contentX(),
                        bottomY,
                        actionWidth,
                        20
                )
                .build()
        );

        recordButton = this.addRenderableWidget(
                new RecorderActionButton(
                        layout.contentX()
                                + layout.contentWidth()
                                - actionWidth,
                        bottomY,
                        actionWidth,
                        20,
                        Component.literal("● Grabar vinilo"),
                        button -> startRecording()
                )
        );

        updateButtons();
    }

    private void goBack() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(
                    new VinylRecorderScreen(draft)
            );
        }
    }

    private void handleTreeSelectionChanged() {

        MusicLibraryEntry entry =
                treeWidget == null
                        ? null
                        : treeWidget.getSelectedEntry();

        candidateCoverPreview =
                entry == null
                        ? null
                        : resolveCoverFromEntry(entry);

        updateButtons();
    }

    private void selectCoverFromTree() {

        MusicLibraryEntry entry =
                treeWidget == null
                        ? null
                        : treeWidget.getSelectedEntry();

        if (entry == null) {
            return;
        }

        AlbumCoverResolver.CoverResult cover =
                candidateCoverPreview != null
                        ? candidateCoverPreview
                        : resolveCoverFromEntry(entry);

        if (cover == null) {
            return;
        }

        Path persistentPath = makeCoverPersistent(cover);

        if (persistentPath == null) {
            return;
        }

        draft.setSelectedCover(
                persistentPath,
                new AlbumCoverResolver.CoverResult(
                        AlbumCoverResolver.SourceType.FILE,
                        persistentPath,
                        null
                )
        );

        candidateCoverPreview = null;
        updateButtons();
    }

    private void removeCover() {
        draft.clearSelectedCover();
        candidateCoverPreview = null;
        updateButtons();
    }

    private AlbumCoverResolver.CoverResult resolveCoverFromEntry(
            MusicLibraryEntry entry) {

        if (entry.isImageFile()) {
            return new AlbumCoverResolver.CoverResult(
                    AlbumCoverResolver.SourceType.FILE,
                    entry.path(),
                    null
            );
        }

        if (!entry.isAudioFile()) {
            return null;
        }

        AudioMetadata metadata =
                AudioMetadataCache.get(entry.path());

        return AlbumCoverResolver.resolve(
                entry.path(),
                metadata
        );
    }

    private Path makeCoverPersistent(
            AlbumCoverResolver.CoverResult cover) {

        if (cover == null) {
            return null;
        }

        if (cover.isFile() && cover.path() != null) {
            return cover.path()
                    .toAbsolutePath()
                    .normalize();
        }

        byte[] data = cover.data();

        if (data == null || data.length == 0) {
            return null;
        }

        try {
            Path folder =
                    FabricLoader.getInstance()
                            .getConfigDir()
                            .resolve("vinylcraft")
                            .resolve("covers");

            Files.createDirectories(folder);

            Path file = folder.resolve(
                    "cover_"
                            + UUID.randomUUID()
                            + detectImageExtension(data)
            );

            Files.write(
                    file,
                    data,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            return file.toAbsolutePath().normalize();

        } catch (IOException exception) {
            System.err.println(
                    "[VinylCraft] No se pudo guardar la portada embebida"
            );
            exception.printStackTrace();
            return null;
        }
    }

    private static String detectImageExtension(byte[] data) {

        if (
                data.length >= 8
                        && (data[0] & 0xFF) == 0x89
                        && data[1] == 0x50
                        && data[2] == 0x4E
                        && data[3] == 0x47
        ) {
            return ".png";
        }

        if (
                data.length >= 3
                        && (data[0] & 0xFF) == 0xFF
                        && (data[1] & 0xFF) == 0xD8
                        && (data[2] & 0xFF) == 0xFF
        ) {
            return ".jpg";
        }

        if (
                data.length >= 12
                        && data[0] == 'R'
                        && data[1] == 'I'
                        && data[2] == 'F'
                        && data[3] == 'F'
                        && data[8] == 'W'
                        && data[9] == 'E'
                        && data[10] == 'B'
                        && data[11] == 'P'
        ) {
            return ".webp";
        }

        return ".img";
    }

    private void updateButtons() {

        if (useCoverButton != null) {
            useCoverButton.active = candidateCoverPreview != null;
        }

        if (removeCoverButton != null) {
            removeCoverButton.active = draft.selectedCoverPath() != null;
        }

        if (recordButton != null) {
            recordButton.active =
                    !draft.selectedTracks().isEmpty()
                            && draft.selectedCoverPath() != null;
        }
    }

    private void startRecording() {

        AlbumData album = buildAlbum();

        if (
                album == null
                        || album.isEmpty()
                        || draft.selectedCoverPath() == null
        ) {
            return;
        }

        RecordedAlbumStore.registerAndSave(album);

        ClientPlayNetworking.send(
                new StartVinylRecordingPayload(
                        draft.recorderPos(),
                        album.id(),
                        encodeTrackDurations(album)
                )
        );

        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(null);
        }
    }

    @Override
    public void onClose() {
        goBack();
    }

    private String encodeTrackDurations(AlbumData album) {

        if (album == null || album.isEmpty()) {
            return "";
        }

        StringBuilder encoded = new StringBuilder();

        for (TrackData track : album.tracks()) {
            if (!encoded.isEmpty()) {
                encoded.append(',');
            }

            encoded.append(
                    Math.max(1L, track.durationTicks())
            );
        }

        return encoded.toString();
    }

    private AlbumData buildAlbum() {

        if (
                draft.selectedTracks().isEmpty()
                        || draft.selectedCoverPath() == null
        ) {
            return null;
        }

        List<TrackData> tracks = new ArrayList<>();
        List<AudioMetadata> metadataList = new ArrayList<>();

        for (MusicLibraryEntry entry : draft.selectedTracks()) {

            AudioMetadata metadata =
                    AudioMetadataCache.get(entry.path());

            metadataList.add(metadata);

            String title =
                    metadata != null && metadata.hasTitle()
                            ? metadata.title()
                            : entry.name();

            long duration =
                    metadata == null
                            ? 0L
                            : metadata.durationMillis();

            String absolutePath =
                    entry.path()
                            .toAbsolutePath()
                            .normalize()
                            .toString();

            String trackId =
                    UUID.nameUUIDFromBytes(
                            absolutePath.getBytes(
                                    java.nio.charset.StandardCharsets.UTF_8
                            )
                    )
                    .toString()
                    .replace("-", "");

            tracks.add(
                    new TrackData(
                            trackId,
                            title,
                            absolutePath,
                            duration
                    )
            );
        }

        String albumId =
                "recorded_"
                        + UUID.randomUUID()
                                .toString()
                                .replace("-", "");

        AudioMetadata first = metadataList.get(0);

        String albumTitle =
                first != null && first.hasAlbum()
                        ? first.album()
                        : "Álbum personalizado";

        String artist = resolveAlbumArtist(metadataList);

        return new AlbumData(
                albumId,
                albumTitle,
                artist,
                draft.selectedCoverPath().toString(),
                tracks
        );
    }

    private String resolveAlbumArtist(
            List<AudioMetadata> metadataList) {

        String resolved = "";

        for (AudioMetadata metadata : metadataList) {

            if (metadata == null) {
                continue;
            }

            String candidate =
                    metadata.hasAlbumArtist()
                            ? metadata.albumArtist()
                            : metadata.artist();

            if (candidate == null || candidate.isBlank()) {
                continue;
            }

            if (resolved.isBlank()) {
                resolved = candidate;
                continue;
            }

            if (!resolved.equalsIgnoreCase(candidate)) {
                return "Varios artistas";
            }
        }

        return resolved.isBlank()
                ? "Artista desconocido"
                : resolved;
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
                "Grabador de vinilos · Portada",
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

        drawCoverSection(graphics, layout);
    }

    private void drawCoverSection(
            GuiGraphicsExtractor graphics,
            Layout layout) {

        int centerX = layout.panelX() + layout.panelWidth() / 2;

        graphics.centeredText(
                this.font,
                "Portada del álbum",
                centerX,
                CONTENT_TOP + 10,
                0xFFFFFFFF
        );

        AlbumCoverResolver.CoverResult preview =
                candidateCoverPreview != null
                        ? candidateCoverPreview
                        : draft.selectedCoverPreview();

        int previewSize = layout.coverPreviewSize();
        int previewX = centerX - previewSize / 2;
        int previewY = layout.coverPreviewY();

        graphics.fill(
                previewX - 1,
                previewY - 1,
                previewX + previewSize + 1,
                previewY + previewSize + 1,
                draft.selectedCoverPath() != null
                        ? 0xAA2E8B57
                        : 0xAA555555
        );

        graphics.fill(
                previewX,
                previewY,
                previewX + previewSize,
                previewY + previewSize,
                0xFF161616
        );

        if (preview != null) {
            drawCoverPreview(
                    graphics,
                    preview,
                    previewX,
                    previewY,
                    previewSize
            );
        }

        String status;
        int statusColor;

        if (candidateCoverPreview != null) {
            status = "Vista de selección";
            statusColor = 0xFFAAAAAA;
        } else if (draft.selectedCoverPath() != null) {
            status = "✓ Portada elegida";
            statusColor = 0xFF77DD88;
        } else {
            status = "Selecciona una imagen o una canción con portada";
            statusColor = 0xFF888888;
        }

        status = this.font.plainSubstrByWidth(
                status,
                Math.max(40, layout.panelWidth() - 20)
        );

        graphics.centeredText(
                this.font,
                status,
                centerX,
                previewY + previewSize + 5,
                statusColor
        );

        graphics.centeredText(
                this.font,
                draft.selectedTracks().size() + " pista(s) seleccionada(s)",
                centerX,
                previewY + previewSize + 18,
                0xFFAAAAAA
        );
    }

    private void drawCoverPreview(
            GuiGraphicsExtractor graphics,
            AlbumCoverResolver.CoverResult preview,
            int x,
            int y,
            int size) {

        Identifier texture =
                AlbumCoverTextureManager.getTexture(preview);

        if (texture == null) {
            return;
        }

        AlbumCoverTextureManager.CoverSize source =
                AlbumCoverTextureManager.getSize(texture);

        int drawWidth = size;
        int drawHeight = size;

        if (source != null) {
            if (source.width() >= source.height()) {
                drawHeight = Math.max(
                        1,
                        Math.round(
                                size
                                        * (source.height() / (float) source.width())
                        )
                );
            } else {
                drawWidth = Math.max(
                        1,
                        Math.round(
                                size
                                        * (source.width() / (float) source.height())
                        )
                );
            }
        }

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x + (size - drawWidth) / 2,
                y + (size - drawHeight) / 2,
                0.0F,
                0.0F,
                drawWidth,
                drawHeight,
                drawWidth,
                drawHeight
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

        int availablePanelHeight = contentBottom - CONTENT_TOP;
        int previewSize = Math.min(
                Math.max(72, panelWidth - 64),
                Math.max(72, availablePanelHeight - 125)
        );
        previewSize = Math.min(previewSize, 150);

        int previewY = CONTENT_TOP + 28;
        int buttonsY = Math.min(
                contentBottom - 26,
                previewY + previewSize + 36
        );

        return new Layout(
                contentX,
                contentWidth,
                treeWidth,
                panelX,
                panelWidth,
                contentBottom,
                previewSize,
                previewY,
                buttonsY
        );
    }

    private record Layout(
            int contentX,
            int contentWidth,
            int treeWidth,
            int panelX,
            int panelWidth,
            int contentBottom,
            int coverPreviewSize,
            int coverPreviewY,
            int coverButtonsY) {
    }
}
