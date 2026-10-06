package com.radig.vinylcraft.client.recorder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.radig.vinylcraft.client.library.AlbumCoverResolver;
import com.radig.vinylcraft.client.library.MusicLibraryEntry;

import net.minecraft.core.BlockPos;

/**
 * Estado compartido entre las dos pantallas del asistente de grabación.
 * Así podemos ir y volver entre "Pistas" y "Portada" sin perder nada.
 */
public final class VinylRecorderDraft {

    private final BlockPos recorderPos;

    private final List<MusicLibraryEntry> selectedTracks =
            new ArrayList<>();

    private Path selectedCoverPath;
    private AlbumCoverResolver.CoverResult selectedCoverPreview;

    public VinylRecorderDraft(BlockPos recorderPos) {
        this.recorderPos = recorderPos.immutable();
    }

    public BlockPos recorderPos() {
        return recorderPos;
    }

    public List<MusicLibraryEntry> selectedTracks() {
        return selectedTracks;
    }

    public Path selectedCoverPath() {
        return selectedCoverPath;
    }

    public void setSelectedCover(
            Path path,
            AlbumCoverResolver.CoverResult preview) {

        this.selectedCoverPath = path;
        this.selectedCoverPreview = preview;
    }

    public void clearSelectedCover() {
        this.selectedCoverPath = null;
        this.selectedCoverPreview = null;
    }

    public AlbumCoverResolver.CoverResult selectedCoverPreview() {
        return selectedCoverPreview;
    }
}
