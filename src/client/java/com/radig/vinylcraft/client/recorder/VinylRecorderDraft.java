package com.radig.vinylcraft.client.recorder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import com.radig.vinylcraft.client.library.AlbumCoverResolver;
import com.radig.vinylcraft.client.library.MusicLibraryEntry;
import com.radig.vinylcraft.client.library.MusicLibraryTreeWidget;

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

    private final Set<Path> treeExpandedFolders = new HashSet<>();
    private Path treeSelectedPath;
    private double treeScrollAmount;

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

    public void captureTreeState(MusicLibraryTreeWidget widget) {
        if (widget == null) {
            return;
        }

        treeExpandedFolders.clear();
        treeExpandedFolders.addAll(widget.getExpandedFoldersSnapshot());
        treeSelectedPath = widget.getSelectedPath();
        treeScrollAmount = widget.getScrollAmount();
    }

    public void restoreTreeState(MusicLibraryTreeWidget widget) {
        if (widget == null) {
            return;
        }

        widget.restoreState(
                treeExpandedFolders,
                treeSelectedPath,
                treeScrollAmount
        );
    }
}
