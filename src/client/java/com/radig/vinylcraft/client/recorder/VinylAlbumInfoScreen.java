package com.radig.vinylcraft.client.recorder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.radig.vinylcraft.block.entity.VinylRecorderBlockEntity;
import com.radig.vinylcraft.client.library.AlbumCoverResolver;
import com.radig.vinylcraft.client.library.AlbumCoverTextureManager;
import com.radig.vinylcraft.client.music.RecordedAlbumStore;
import com.radig.vinylcraft.item.VinylData;
import com.radig.vinylcraft.music.AlbumData;
import com.radig.vinylcraft.music.ModAlbums;
import com.radig.vinylcraft.music.TrackData;
import com.radig.vinylcraft.network.SetAlbumWallSizePayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Consulta y corrección de metadatos de un vinilo grabado.
 * Se puede cambiar portada, título, artista y nombre visible de las pistas,
 * pero nunca agregar/eliminar/reordenar pistas desde aquí.
 */
public final class VinylAlbumInfoScreen extends Screen {

    private static final int ROW = 18;
    private static final int COVER_SIZE = 56;
    private static final int EDIT_BUTTON_SIZE = 16;

    private final BlockPos recorderPos;
    private int scroll;
    private Button wallSizeButton;
    private Button titleEditButton;
    private Button artistEditButton;
    private final List<Button> trackEditButtons = new ArrayList<>();

    public VinylAlbumInfoScreen(BlockPos recorderPos) {
        super(Component.literal("VinylCraft - Información del álbum"));
        this.recorderPos = recorderPos.immutable();
    }

    @Override
    protected void init() {
        trackEditButtons.clear();
        Layout layout = getLayout();
        AlbumData album = currentAlbum();

        addRenderableWidget(
                Button.builder(
                        Component.literal("Cerrar"),
                        b -> onClose()
                )
                .bounds(
                        width / 2 - 65,
                        height - 30,
                        130,
                        20
                )
                .build()
        );

        wallSizeButton = addRenderableWidget(
                new RecorderColorButton(
                        layout.right() - 146,
                        layout.top() + 10,
                        134,
                        20,
                        getWallSizeLabel(),
                        b -> cycleWallSize(),
                        RecorderColorButton.Theme.NEUTRAL
                )
        );

        if (album == null) {
            wallSizeButton.active = false;
            return;
        }

        int titleButtonX = layout.right() - 168;

        titleEditButton = addRenderableWidget(
                new RecorderColorButton(
                        titleButtonX,
                        layout.top() + 34,
                        EDIT_BUTTON_SIZE,
                        EDIT_BUTTON_SIZE,
                        Component.literal("✎"),
                        b -> editAlbumTitle(),
                        RecorderColorButton.Theme.NEUTRAL
                )
        );

        artistEditButton = addRenderableWidget(
                new RecorderColorButton(
                        titleButtonX,
                        layout.top() + 54,
                        EDIT_BUTTON_SIZE,
                        EDIT_BUTTON_SIZE,
                        Component.literal("✎"),
                        b -> editAlbumArtist(),
                        RecorderColorButton.Theme.NEUTRAL
                )
        );

        int visibleRows = getVisibleRows(layout, album);

        for (int row = 0; row < visibleRows; row++) {
            final int visibleRow = row;

            Button button = addRenderableWidget(
                    new RecorderColorButton(
                            layout.listRight() - EDIT_BUTTON_SIZE,
                            layout.listTop() + row * ROW - 3,
                            EDIT_BUTTON_SIZE,
                            16,
                            Component.literal("✎"),
                            b -> editTrackAtVisibleRow(visibleRow),
                            RecorderColorButton.Theme.NEUTRAL
                    )
            );

            trackEditButtons.add(button);
        }

        updateTrackEditButtons();
    }

    private void editAlbumTitle() {
        AlbumData album = currentAlbum();

        if (album == null || minecraft == null) {
            return;
        }

        minecraft.gui.setScreen(
                new VinylTextEditScreen(
                        this,
                        "Editar título del álbum",
                        album.title(),
                        value -> {
                            AlbumData latest = ModAlbums.get(album.id());

                            if (latest != null) {
                                RecordedAlbumStore.registerAndSave(
                                        new AlbumData(
                                                latest.id(),
                                                value,
                                                latest.artist(),
                                                latest.coverFile(),
                                                latest.tracks()
                                        )
                                );
                            }
                        }
                )
        );
    }

    private void editAlbumArtist() {
        AlbumData album = currentAlbum();

        if (album == null || minecraft == null) {
            return;
        }

        minecraft.gui.setScreen(
                new VinylTextEditScreen(
                        this,
                        "Editar artista",
                        album.artist(),
                        value -> {
                            AlbumData latest = ModAlbums.get(album.id());

                            if (latest != null) {
                                RecordedAlbumStore.registerAndSave(
                                        new AlbumData(
                                                latest.id(),
                                                latest.title(),
                                                value,
                                                latest.coverFile(),
                                                latest.tracks()
                                        )
                                );
                            }
                        }
                )
        );
    }

    private void editTrackAtVisibleRow(int visibleRow) {
        AlbumData album = currentAlbum();

        if (album == null || minecraft == null) {
            return;
        }

        int trackIndex = scroll + visibleRow;
        TrackData track = album.getTrack(trackIndex);

        if (track == null) {
            return;
        }

        minecraft.gui.setScreen(
                new VinylTextEditScreen(
                        this,
                        "Editar nombre de pista " + (trackIndex + 1),
                        track.title(),
                        value -> updateTrackTitle(album.id(), trackIndex, value)
                )
        );
    }

    private void updateTrackTitle(String albumId, int trackIndex, String value) {
        AlbumData latest = ModAlbums.get(albumId);

        if (latest == null || trackIndex < 0 || trackIndex >= latest.trackCount()) {
            return;
        }

        List<TrackData> tracks = new ArrayList<>(latest.tracks());
        TrackData original = tracks.get(trackIndex);

        tracks.set(
                trackIndex,
                new TrackData(
                        original.id(),
                        value,
                        original.audioFile(),
                        original.durationMillis()
                )
        );

        RecordedAlbumStore.registerAndSave(
                new AlbumData(
                        latest.id(),
                        latest.title(),
                        latest.artist(),
                        latest.coverFile(),
                        tracks
                )
        );
    }

    private void openCoverEditor() {
        AlbumData album = currentAlbum();

        if (album == null || minecraft == null) {
            return;
        }

        minecraft.gui.setScreen(
                new VinylAlbumCoverEditScreen(
                        this,
                        album.id()
                )
        );
    }

    private void cycleWallSize() {
        VinylRecorderBlockEntity recorder = currentRecorder();

        if (recorder == null || !recorder.hasRecordedVinyl()) {
            return;
        }

        ItemStack stack = recorder.getVinyl();
        int nextSize = VinylData.getWallSize(stack) + 1;

        if (nextSize > 10) {
            nextSize = 1;
        }

        VinylData.setWallSize(stack, nextSize);
        recorder.setChanged();

        ClientPlayNetworking.send(
                new SetAlbumWallSizePayload(recorderPos, nextSize)
        );

        if (wallSizeButton != null) {
            wallSizeButton.setMessage(getWallSizeLabel());
        }
    }

    private Component getWallSizeLabel() {
        VinylRecorderBlockEntity recorder = currentRecorder();
        int wallSize = 1;

        if (recorder != null && recorder.hasRecordedVinyl()) {
            wallSize = VinylData.getWallSize(recorder.getVinyl());
        }

        return Component.literal("Cuadro: " + wallSize + "x" + wallSize);
    }

    private VinylRecorderBlockEntity currentRecorder() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }

        if (!(minecraft.level.getBlockEntity(recorderPos) instanceof VinylRecorderBlockEntity recorder)) {
            return null;
        }

        return recorder;
    }

    private AlbumData currentAlbum() {
        VinylRecorderBlockEntity recorder = currentRecorder();

        if (recorder == null || !recorder.hasRecordedVinyl()) {
            return null;
        }

        return ModAlbums.get(VinylData.getAlbumId(recorder.getVinyl()));
    }

    private static String format(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        return String.format("%02d:%02d", seconds / 60L, seconds % 60L);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta) {

        AlbumData album = currentAlbum();
        Layout layout = getLayout();

        graphics.centeredText(
                font,
                "Información del vinilo",
                width / 2,
                18,
                0xFFFFFFFF
        );

        graphics.fill(
                layout.left(),
                layout.top(),
                layout.right(),
                layout.bottom(),
                0xB9000000
        );

        if (album == null) {
            graphics.centeredText(
                    font,
                    "No se encontraron datos para este disco",
                    width / 2,
                    layout.top() + 20,
                    0xFFFF9999
            );
            super.extractRenderState(graphics, mouseX, mouseY, delta);
            return;
        }

        drawCover(
                graphics,
                album,
                layout.coverX(),
                layout.coverY(),
                COVER_SIZE
        );

        boolean coverHovered = mouseX >= layout.coverX()
                && mouseX < layout.coverX() + COVER_SIZE
                && mouseY >= layout.coverY()
                && mouseY < layout.coverY() + COVER_SIZE;

        int border = coverHovered ? 0xFFFFFFFF : 0xFF666666;
        drawOutline(
                graphics,
                layout.coverX() - 1,
                layout.coverY() - 1,
                COVER_SIZE + 2,
                COVER_SIZE + 2,
                border
        );

        int textRight = layout.right() - 176;
        int detailsWidth = Math.max(80, textRight - layout.detailsX());

        graphics.text(
                font,
                font.plainSubstrByWidth(album.title(), detailsWidth),
                layout.detailsX(),
                layout.top() + 36,
                0xFFFFFFFF,
                true
        );

        graphics.text(
                font,
                font.plainSubstrByWidth(album.artist(), detailsWidth),
                layout.detailsX(),
                layout.top() + 56,
                0xFFCCCCCC,
                false
        );

        graphics.text(
                font,
                "Pistas: "
                        + album.trackCount()
                        + "   •   Total: "
                        + format(album.totalDurationMillis()),
                layout.detailsX(),
                layout.top() + 76,
                0xFFBBBBBB,
                false
        );

        int visibleRows = getVisibleRows(layout, album);
        int maximum = Math.max(0, album.trackCount() - visibleRows);
        scroll = Math.max(0, Math.min(maximum, scroll));

        for (int index = scroll;
                index < album.trackCount() && index < scroll + visibleRows;
                index++) {

            TrackData track = album.getTrack(index);
            int y = layout.listTop() + (index - scroll) * ROW;
            String duration = format(track.durationMillis());
            int editSpace = EDIT_BUTTON_SIZE + 8;
            int durationWidth = font.width(duration) + 12;
            int titleWidth = Math.max(
                    50,
                    (layout.listRight() - layout.listLeft())
                            - durationWidth
                            - editSpace
            );

            graphics.text(
                    font,
                    font.plainSubstrByWidth(
                            (index + 1) + ". " + track.title(),
                            titleWidth
                    ),
                    layout.listLeft(),
                    y,
                    0xFFFFFFFF,
                    false
            );

            graphics.text(
                    font,
                    duration,
                    layout.listRight() - editSpace - durationWidth,
                    y,
                    0xFFAAAAAA,
                    false
            );
        }

        if (maximum > 0) {
            int scrollX = layout.right() - 5;
            int listHeight = Math.max(1, layout.bottom() - layout.listTop() - 8);
            int thumbHeight = Math.max(10, listHeight * visibleRows / album.trackCount());
            int thumbY = layout.listTop()
                    + (listHeight - thumbHeight) * scroll / maximum;

            graphics.fill(
                    scrollX,
                    layout.listTop(),
                    scrollX + 2,
                    layout.listTop() + listHeight,
                    0x55444444
            );

            graphics.fill(
                    scrollX,
                    thumbY,
                    scrollX + 2,
                    thumbY + thumbHeight,
                    0xFFAAAAAA
            );
        }

        /* Los widgets se extraen al final para que el panel no los oscurezca. */
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawOutline(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int w,
            int h,
            int color) {

        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawCover(
            GuiGraphicsExtractor graphics,
            AlbumData album,
            int x,
            int y,
            int size) {

        if (album.coverFile() == null || album.coverFile().isBlank()) {
            return;
        }

        try {
            var cover = new AlbumCoverResolver.CoverResult(
                    AlbumCoverResolver.SourceType.FILE,
                    Path.of(album.coverFile()),
                    null
            );

            Identifier texture = AlbumCoverTextureManager.getTexture(cover);

            if (texture != null) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        texture,
                        x,
                        y,
                        0.0F,
                        0.0F,
                        size,
                        size,
                        size,
                        size
                );
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Layout layout = getLayout();

        if (event.x() >= layout.coverX()
                && event.x() < layout.coverX() + COVER_SIZE
                && event.y() >= layout.coverY()
                && event.y() < layout.coverY() + COVER_SIZE) {
            openCoverEditor();
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount) {

        AlbumData album = currentAlbum();

        if (album != null) {
            Layout layout = getLayout();
            int visible = getVisibleRows(layout, album);

            scroll = Math.max(
                    0,
                    Math.min(
                            Math.max(0, album.trackCount() - visible),
                            scroll - (int) Math.signum(verticalAmount) * 3
                    )
            );

            updateTrackEditButtons();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private int getVisibleRows(Layout layout, AlbumData album) {
        if (album == null) {
            return 1;
        }

        return Math.max(
                1,
                (layout.bottom() - layout.listTop() - 8) / ROW
        );
    }

    private void updateTrackEditButtons() {
        AlbumData album = currentAlbum();

        for (int row = 0; row < trackEditButtons.size(); row++) {
            Button button = trackEditButtons.get(row);
            int trackIndex = scroll + row;
            boolean valid = album != null && trackIndex < album.trackCount();

            button.visible = valid;
            button.active = valid;
        }
    }

    private Layout getLayout() {
        int totalWidth = Math.min(620, width - 24);
        int left = (width - totalWidth) / 2;
        int right = left + totalWidth;
        int top = 42;
        int bottom = height - 44;
        int coverX = left + 12;
        int coverY = top + 12;
        int detailsX = coverX + COVER_SIZE + 14;
        int listTop = top + 94;
        int listLeft = left + 12;
        int listRight = right - 12;

        return new Layout(
                left,
                right,
                top,
                bottom,
                coverX,
                coverY,
                detailsX,
                listTop,
                listLeft,
                listRight
        );
    }

    private record Layout(
            int left,
            int right,
            int top,
            int bottom,
            int coverX,
            int coverY,
            int detailsX,
            int listTop,
            int listLeft,
            int listRight) {
    }
}
