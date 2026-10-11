package com.radig.vinylcraft.client.recorder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
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
import com.radig.vinylcraft.music.ModAlbums;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Permite cambiar únicamente la portada de un álbum ya grabado. */
public final class VinylAlbumCoverEditScreen extends Screen {

    private final Screen parent;
    private final String albumId;

    private MusicLibraryTreeWidget treeWidget;
    private RecorderColorButton chooseButton;
    private AlbumCoverResolver.CoverResult candidate;

    public VinylAlbumCoverEditScreen(Screen parent, String albumId) {
        super(Component.literal("Cambiar portada del álbum"));
        this.parent = parent;
        this.albumId = albumId;
    }

    @Override
    protected void init() {
        MusicLibraryManager.refresh();

        int margin = 24;
        int contentTop = 42;
        int bottom = height - 42;
        int contentWidth = Math.max(360, width - margin * 2);
        int treeWidth = Math.max(220, Math.round(contentWidth * 0.58F));
        int gap = 12;
        int panelX = margin + treeWidth + gap;
        int panelWidth = Math.max(150, width - margin - panelX);

        treeWidget = addRenderableWidget(
                new MusicLibraryTreeWidget(
                        margin,
                        contentTop,
                        treeWidth,
                        Math.max(80, bottom - contentTop)
                )
        );

        restoreCurrentCoverLocation();

        treeWidget.setSelectionChangedListener(() -> {
            MusicLibraryEntry entry = treeWidget.getSelectedEntry();
            candidate = entry == null ? null : resolveCover(entry);
            updateButton();
        });

        int buttonY = height - 30;
        int buttonGap = 8;
        int buttonWidth = Math.max(88, Math.min(120, (panelWidth - buttonGap) / 2));
        int buttonsX = panelX + Math.max(0, (panelWidth - (buttonWidth * 2 + buttonGap)) / 2);

        addRenderableWidget(
                new RecorderColorButton(
                        buttonsX,
                        buttonY,
                        buttonWidth,
                        20,
                        Component.literal("Cancelar"),
                        button -> closeToParent(),
                        RecorderColorButton.Theme.NEUTRAL
                )
        );

        chooseButton = addRenderableWidget(
                new RecorderColorButton(
                        buttonsX + buttonWidth + buttonGap,
                        buttonY,
                        buttonWidth,
                        20,
                        Component.literal("+ Elegir"),
                        button -> applyCover(),
                        RecorderColorButton.Theme.GREEN
                )
        );

        updateButton();
    }

    private void restoreCurrentCoverLocation() {
        AlbumData album = ModAlbums.get(albumId);

        if (album == null || album.coverFile() == null || album.coverFile().isBlank()) {
            return;
        }

        try {
            Path current = Path.of(album.coverFile()).toAbsolutePath().normalize();
            Set<Path> expanded = new HashSet<>();
            Path parentPath = current.getParent();

            while (parentPath != null) {
                expanded.add(parentPath);
                parentPath = parentPath.getParent();
            }

            treeWidget.restoreState(expanded, current, 0.0D);
        } catch (Exception ignored) {
        }
    }

    private void updateButton() {
        if (chooseButton != null) {
            chooseButton.active = candidate != null;
        }
    }

    private AlbumCoverResolver.CoverResult resolveCover(MusicLibraryEntry entry) {
        if (entry == null) {
            return null;
        }

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

        AudioMetadata metadata = AudioMetadataCache.get(entry.path());
        return AlbumCoverResolver.resolve(entry.path(), metadata);
    }

    private void applyCover() {
        if (candidate == null) {
            return;
        }

        Path persistent = makeCoverPersistent(candidate);
        AlbumData album = ModAlbums.get(albumId);

        if (persistent == null || album == null) {
            return;
        }

        RecordedAlbumStore.registerAndSave(
                new AlbumData(
                        album.id(),
                        album.title(),
                        album.artist(),
                        persistent.toString(),
                        album.tracks()
                )
        );

        closeToParent();
    }

    private Path makeCoverPersistent(AlbumCoverResolver.CoverResult cover) {
        if (cover == null) {
            return null;
        }

        if (cover.isFile() && cover.path() != null) {
            return cover.path().toAbsolutePath().normalize();
        }

        byte[] data = cover.data();

        if (data == null || data.length == 0) {
            return null;
        }

        try {
            Path folder = FabricLoader.getInstance()
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
            return null;
        }
    }

    private static String detectImageExtension(byte[] data) {
        if (data.length >= 8
                && (data[0] & 0xFF) == 0x89
                && data[1] == 0x50
                && data[2] == 0x4E
                && data[3] == 0x47) {
            return ".png";
        }

        if (data.length >= 3
                && (data[0] & 0xFF) == 0xFF
                && (data[1] & 0xFF) == 0xD8
                && (data[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }

        if (data.length >= 12
                && data[0] == 'R'
                && data[1] == 'I'
                && data[2] == 'F'
                && data[3] == 'F'
                && data[8] == 'W'
                && data[9] == 'E'
                && data[10] == 'B'
                && data[11] == 'P') {
            return ".webp";
        }

        return ".img";
    }

    private AlbumCoverResolver.CoverResult previewCover() {
        if (candidate != null) {
            return candidate;
        }

        AlbumData album = ModAlbums.get(albumId);

        if (album == null || album.coverFile() == null || album.coverFile().isBlank()) {
            return null;
        }

        try {
            return new AlbumCoverResolver.CoverResult(
                    AlbumCoverResolver.SourceType.FILE,
                    Path.of(album.coverFile()),
                    null
            );
        } catch (Exception ignored) {
            return null;
        }
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
                "Cambiar portada del álbum",
                width / 2,
                18,
                0xFFFFFFFF
        );

        int margin = 24;
        int contentWidth = Math.max(360, width - margin * 2);
        int treeWidth = Math.max(220, Math.round(contentWidth * 0.58F));
        int panelX = margin + treeWidth + 12;
        int panelWidth = Math.max(150, width - margin - panelX);
        int previewSize = Math.min(150, Math.max(80, panelWidth - 20));
        int previewX = panelX + (panelWidth - previewSize) / 2;
        int previewY = 70;

        AlbumCoverResolver.CoverResult preview = previewCover();

        if (preview != null) {
            Identifier texture = AlbumCoverTextureManager.getTexture(preview);

            if (texture != null) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        texture,
                        previewX,
                        previewY,
                        0.0F,
                        0.0F,
                        previewSize,
                        previewSize,
                        previewSize,
                        previewSize
                );
            }
        } else {
            graphics.centeredText(
                    font,
                    "Selecciona una imagen o una canción con portada",
                    panelX + panelWidth / 2,
                    previewY + 20,
                    0xFFAAAAAA
            );
        }
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
}
