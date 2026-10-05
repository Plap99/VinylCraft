package com.radig.vinylcraft.client.library;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class MusicLibraryTreeWidget
        extends AbstractWidget {

    private static final int ROW_HEIGHT = 20;
    private static final int INDENT_WIDTH = 14;
    private Runnable selectionChangedListener;

    private final List<VisibleEntry> visibleEntries =
            new ArrayList<>();

    private final Set<Path> expandedFolders =
            new HashSet<>();

    private double scrollAmount;

    private Path selectedPath;
    private MusicLibraryEntry selectedEntry;

    public void setSelectionChangedListener(
            Runnable listener) {

        this.selectionChangedListener = listener;
    }

    private void notifySelectionChanged() {

        if (selectionChangedListener != null) {
            selectionChangedListener.run();
        }
    }

    public MusicLibraryTreeWidget(
            int x,
            int y,
            int width,
            int height) {

        super(
                x,
                y,
                width,
                height,
                Component.literal(
                        "Biblioteca de música"
                )
        );

        rebuildEntries();
    }


    public void rebuildEntries() {

        visibleEntries.clear();

        for (
                MusicLibraryEntry library :
                MusicLibraryManager.getLibraries()
        ) {

            addVisibleEntry(
                    library,
                    0
            );
        }

        clampScroll();
    }


    private void addVisibleEntry(
            MusicLibraryEntry entry,
            int depth) {

        visibleEntries.add(
                new VisibleEntry(
                        entry,
                        depth
                )
        );

        if (!entry.isFolder()) {
            return;
        }

        Path path =
                normalize(entry.path());

        if (!expandedFolders.contains(path)) {
            return;
        }

        for (
                MusicLibraryEntry child :
                entry.children()
        ) {

            addVisibleEntry(
                    child,
                    depth + 1
            );
        }
    }


    private void toggleFolder(
            MusicLibraryEntry entry) {

        if (
                entry == null
                || !entry.isFolder()
        ) {
            return;
        }

        Path path =
                normalize(entry.path());

        if (expandedFolders.contains(path)) {
            expandedFolders.remove(path);
        } else {
            expandedFolders.add(path);
        }

        selectedPath = path;

        rebuildEntries();
    }


    @Override
    protected void extractWidgetRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta) {

        graphics.fill(
                getX(),
                getY(),
                getX() + width,
                getY() + height,
                0x88000000
        );
        
        int top = getY();
        int bottom = getY() + height;

        int firstIndex =
                Math.max(
                        0,
                        (int) (
                                scrollAmount
                                / ROW_HEIGHT
                        )
                );

        int y =
                top
                - (int) scrollAmount
                + firstIndex * ROW_HEIGHT;


        for (
                int index = firstIndex;
                index < visibleEntries.size();
                index++
        ) {

            if (y + ROW_HEIGHT > bottom) {
                break;
            }

            if (y >= top) {

                VisibleEntry visible =
                        visibleEntries.get(index);

                drawEntry(
                        graphics,
                        visible,
                        y
                );
            }

            y += ROW_HEIGHT;
        }

        drawScrollbar(graphics);
    }


    private void drawScrollbar(
            GuiGraphicsExtractor graphics) {

        double contentHeight =
                visibleEntries.size()
                * (double) ROW_HEIGHT;

        if (contentHeight <= height) {
            return;
        }

        int barWidth = 4;

        int barX =
                getX()
                + width
                - barWidth
                - 2;

        int barTop =
                getY();

        int barHeight =
                height;


        int thumbHeight =
                Math.max(
                        12,
                        (int) (
                                barHeight
                                * (height / contentHeight)
                        )
                );


        double maxScroll =
                contentHeight - height;

        double scrollPercent =
                maxScroll <= 0.0D
                        ? 0.0D
                        : scrollAmount / maxScroll;


        int thumbTravel =
                barHeight - thumbHeight;

        int thumbY =
                barTop
                + (int) (
                        thumbTravel
                        * scrollPercent
                );


        graphics.fill(
                barX,
                barTop,
                barX + barWidth,
                barTop + barHeight,
                0x66000000
        );


        graphics.fill(
                barX,
                thumbY,
                barX + barWidth,
                thumbY + thumbHeight,
                0xFFAAAAAA
        );
    }

    private void drawEntry(
            GuiGraphicsExtractor graphics,
            VisibleEntry visible,
            int y) {

        MusicLibraryEntry entry =
                visible.entry();

        int textX =
                getX()
                + 6
                + visible.depth()
                * INDENT_WIDTH;

        String prefix;

        if (entry.isFolder()) {

            boolean expanded =
                    expandedFolders.contains(
                            normalize(entry.path())
                    );

            prefix =
                    expanded
                            ? "▼ "
                            : "▶ ";

        } else if (entry.isAudioFile()) {

            prefix = "♪ ";

        } else if (entry.isImageFile()) {

            prefix = "▣ ";

        } else {

            prefix = "";
        }


        int color =
                normalize(entry.path())
                        .equals(selectedPath)
                        ? 0xFFFFFF55
                        : 0xFFFFFFFF;


        var font =
                net.minecraft.client.Minecraft
                        .getInstance()
                        .font;

        String fullText =
                prefix + entry.name();

        int rightPadding = 12;

        int availableWidth =
                getX()
                + width
                - rightPadding
                - textX;

        String displayText =
                font.plainSubstrByWidth(
                        fullText,
                        Math.max(0, availableWidth)
                );

        if (
                !displayText.equals(fullText)
                && availableWidth > font.width("...")
        ) {

            displayText =
                    font.plainSubstrByWidth(
                            fullText,
                            availableWidth
                            - font.width("...")
                    )
                    + "...";
        }

        graphics.text(
                font,
                displayText,
                textX,
                y + 5,
                color,
                true
        );
    }


    @Override
    public boolean mouseClicked(
            MouseButtonEvent event,
            boolean doubleClick) {

        double mouseX = event.x();
        double mouseY = event.y();

        if (
                mouseX < getX()
                || mouseX >= getX() + width
                || mouseY < getY()
                || mouseY >= getY() + height
        ) {
            return false;
        }

        int index =
                (int) (
                        (
                                mouseY
                                - getY()
                                + scrollAmount
                        )
                        / ROW_HEIGHT
                );

        if (
                index < 0
                || index >= visibleEntries.size()
        ) {
            return false;
        }
        
        double rowY =
                getY()
                + index * ROW_HEIGHT
                - scrollAmount;

        if (
                rowY < getY()
                || rowY + ROW_HEIGHT > getY() + height
        ) {
            return false;
        }

        MusicLibraryEntry entry =
                visibleEntries
                        .get(index)
                        .entry();

        selectedEntry = entry;

        selectedPath =
                normalize(entry.path());

        notifySelectionChanged();

        if (entry.isFolder()) {

            toggleFolder(entry);

        } else if (entry.isAudioFile()) {

            System.out.println(
                    "[VinylCraft] Audio seleccionado: "
                            + entry.path()
            );

        } else if (entry.isImageFile()) {

            System.out.println(
                    "[VinylCraft] Imagen seleccionada: "
                            + entry.path()
            );
        }

        return true;
    }


    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount) {

        if (
                mouseX < getX()
                || mouseX >= getX() + width
                || mouseY < getY()
                || mouseY >= getY() + height
        ) {
            return false;
        }

        scrollAmount -=
                verticalAmount
                * ROW_HEIGHT
                * 3.0D;

        clampScroll();

        return true;
    }


    private void clampScroll() {

        double contentHeight =
                visibleEntries.size()
                * (double) ROW_HEIGHT;

        double maxScroll =
                Math.max(
                        0.0D,
                        contentHeight - height
                );

        scrollAmount =
                Math.max(
                        0.0D,
                        Math.min(
                                scrollAmount,
                                maxScroll
                        )
                );
    }


    public Path getSelectedPath() {
        return selectedPath;
    }

    public MusicLibraryEntry getSelectedEntry() {
        return selectedEntry;
    }

    private Path normalize(Path path) {

        if (path == null) {
            return null;
        }

        return path
                .toAbsolutePath()
                .normalize();
    }

    @Override
    protected void updateWidgetNarration(
            NarrationElementOutput narrationElementOutput) {

        this.defaultButtonNarrationText(
                narrationElementOutput
        );
    }

    private record VisibleEntry(
            MusicLibraryEntry entry,
            int depth) {
    }
}