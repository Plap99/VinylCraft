package com.radig.vinylcraft.client.render;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.radig.vinylcraft.client.config.VinylHudConfig;
import com.radig.vinylcraft.client.library.AlbumCoverResolver;
import com.radig.vinylcraft.client.library.AlbumCoverTextureManager;
import com.radig.vinylcraft.music.AlbumData;
import com.radig.vinylcraft.music.TrackData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** HUD multifuente de reproducción de VinylCraft. */
public final class VinylPlaybackHud {

    private static final int MARGIN = 8;
    private static final int PANEL_WIDTH = 216;
    private static final int PANEL_HEIGHT = 60;
    private static final int PANEL_GAP = 4;
    private static final int COVER_SIZE = 48;
    private static final int SOURCE_ICON_SIZE = 9;
    private static final int FOOTER_HEIGHT = 15;

    private VinylPlaybackHud() {
    }

    public static void extractRenderState(
            GuiGraphicsExtractor graphics) {

        extractRenderStateInternal(
                graphics,
                false,
                false
        );
    }

    /**
     * Versión usada dentro de InventoryScreen. Sólo representa la fuente
     * Discman y mantiene el comportamiento de mostrar el álbum cargado aun
     * cuando el transporte está en STOP.
     */
    public static void extractDiscmanInventoryRenderState(
            GuiGraphicsExtractor graphics) {

        extractRenderStateInternal(
                graphics,
                true,
                true
        );
    }

    private static void extractRenderStateInternal(
            GuiGraphicsExtractor graphics,
            boolean allowScreen,
            boolean discmanOnly) {

        Minecraft minecraft = Minecraft.getInstance();

        if (
                minecraft.player == null
                        || minecraft.level == null
                        || (!allowScreen && minecraft.gui.screen() != null)
                        || minecraft.gui.hud.isHidden()
                        || !VinylHudConfig.isEnabled()
        ) {
            return;
        }

        List<PlaybackHudSource> sources =
                PlaybackHudManager.collectSources(discmanOnly);

        if (sources.isEmpty()) {
            return;
        }

        float scale = VinylHudConfig.getScale();

        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);

        try {
            int scaledGuiWidth = Math.max(
                    1,
                    (int) Math.floor(graphics.guiWidth() / scale)
            );

            int scaledGuiHeight = Math.max(
                    1,
                    (int) Math.floor(graphics.guiHeight() / scale)
            );

            int requestedMax = discmanOnly
                    ? 1
                    : VinylHudConfig.getMaxHudSources();

            int maxBySpace = Math.max(
                    1,
                    (scaledGuiHeight - (MARGIN * 2) + PANEL_GAP)
                            / (PANEL_HEIGHT + PANEL_GAP)
            );

            int visibleCount = Math.min(
                    sources.size(),
                    Math.min(requestedMax, maxBySpace)
            );

            int hiddenCount = Math.max(0, sources.size() - visibleCount);

            /*
             * Si habrá indicador de fuentes ocultas, reservamos también su
             * altura para que nunca se salga del borde de la pantalla.
             */
            if (!discmanOnly && hiddenCount > 0) {
                int availableForCards = Math.max(
                        PANEL_HEIGHT,
                        scaledGuiHeight
                                - (MARGIN * 2)
                                - FOOTER_HEIGHT
                                - PANEL_GAP
                );

                int maxCardsWithFooter = Math.max(
                        1,
                        (availableForCards + PANEL_GAP)
                                / (PANEL_HEIGHT + PANEL_GAP)
                );

                visibleCount = Math.min(visibleCount, maxCardsWithFooter);
                hiddenCount = Math.max(0, sources.size() - visibleCount);
            }

            VinylHudConfig.Position position = VinylHudConfig.getPosition();
            boolean topAnchored =
                    position == VinylHudConfig.Position.TOP_LEFT
                            || position == VinylHudConfig.Position.TOP_RIGHT;

            int panelX = switch (position) {
                case TOP_LEFT, BOTTOM_LEFT -> MARGIN;
                case TOP_RIGHT, BOTTOM_RIGHT ->
                        scaledGuiWidth - PANEL_WIDTH - MARGIN;
            };

            int configuredAlpha = Math.round(
                    VinylHudConfig.getAlpha() * 255.0F
            );

            boolean fadeWholeHud =
                    VinylHudConfig.getAlphaMode()
                            == VinylHudConfig.AlphaMode.WHOLE_HUD;

            int contentAlpha = fadeWholeHud
                    ? configuredAlpha
                    : 255;

            int panelColor =
                    ((configuredAlpha & 0xFF) << 24)
                            | 0x00101010;

            int borderAlpha = fadeWholeHud
                    ? configuredAlpha
                    : Math.min(
                            255,
                            Math.max(40, configuredAlpha + 24)
                    );

            int borderColor =
                    ((borderAlpha & 0xFF) << 24)
                            | 0x005C5C5C;

            for (int index = 0; index < visibleCount; index++) {
                int panelY = topAnchored
                        ? MARGIN + index * (PANEL_HEIGHT + PANEL_GAP)
                        : scaledGuiHeight
                                - MARGIN
                                - PANEL_HEIGHT
                                - index * (PANEL_HEIGHT + PANEL_GAP);

                renderSourceCard(
                        graphics,
                        minecraft,
                        sources.get(index),
                        panelX,
                        panelY,
                        panelColor,
                        borderColor,
                        contentAlpha
                );
            }

            if (!discmanOnly && hiddenCount > 0) {
                int footerY;

                if (topAnchored) {
                    footerY =
                            MARGIN
                                    + visibleCount * (PANEL_HEIGHT + PANEL_GAP);
                } else {
                    int topMostPanelY =
                            scaledGuiHeight
                                    - MARGIN
                                    - PANEL_HEIGHT
                                    - (visibleCount - 1)
                                            * (PANEL_HEIGHT + PANEL_GAP);

                    footerY = topMostPanelY - PANEL_GAP - FOOTER_HEIGHT;
                }

                renderMoreFooter(
                        graphics,
                        minecraft.font,
                        panelX,
                        footerY,
                        hiddenCount,
                        panelColor,
                        borderColor,
                        contentAlpha
                );
            }
        } finally {
            graphics.pose().popMatrix();
        }
    }

    private static void renderSourceCard(
            GuiGraphicsExtractor graphics,
            Minecraft minecraft,
            PlaybackHudSource source,
            int panelX,
            int panelY,
            int panelColor,
            int borderColor,
            int contentAlpha) {

        AlbumData album = source.album();
        PlaybackInfo playback = resolvePlayback(
                album,
                source.playbackTicks()
        );

        if (playback == null || playback.track() == null) {
            return;
        }

        graphics.fill(
                panelX,
                panelY,
                panelX + PANEL_WIDTH,
                panelY + PANEL_HEIGHT,
                panelColor
        );

        graphics.outline(
                panelX,
                panelY,
                PANEL_WIDTH,
                PANEL_HEIGHT,
                borderColor
        );

        int coverX = panelX + 6;
        int coverY = panelY + 6;

        drawCover(
                graphics,
                album,
                coverX,
                coverY,
                COVER_SIZE,
                contentAlpha
        );

        int sourceIconX =
                panelX + PANEL_WIDTH - SOURCE_ICON_SIZE - 6;
        int sourceIconY = panelY + 6;

        drawSourceIcon(
                graphics,
                source.type(),
                sourceIconX,
                sourceIconY,
                contentAlpha
        );

        Font font = minecraft.font;
        int textX = coverX + COVER_SIZE + 8;
        int textWidth =
                PANEL_WIDTH
                        - COVER_SIZE
                        - 20
                        - SOURCE_ICON_SIZE
                        - 4;
        int textY = panelY + 7;

        String title = fit(
                font,
                safe(playback.track().title(), "Sin título"),
                textWidth
        );

        String artist = fit(
                font,
                safe(album.artist(), "Artista desconocido"),
                textWidth
        );

        String time =
                formatTicks(playback.elapsedInTrackTicks())
                        + " / "
                        + formatTicks(playback.trackDurationTicks());

        String trackNumber =
                "Pista "
                        + (playback.trackIndex() + 1)
                        + " de "
                        + album.trackCount();

        graphics.text(
                font,
                title,
                textX,
                textY,
                argb(contentAlpha, 0xFFFFFF),
                true
        );

        graphics.text(
                font,
                artist,
                textX,
                textY + 12,
                argb(contentAlpha, 0xBBBBBB),
                false
        );

        graphics.text(
                font,
                time,
                textX,
                textY + 25,
                argb(contentAlpha, 0xFFFFFF),
                false
        );

        graphics.text(
                font,
                trackNumber,
                textX,
                textY + 38,
                argb(contentAlpha, 0xAAAAAA),
                false
        );
    }

    private static void renderMoreFooter(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int hiddenCount,
            int panelColor,
            int borderColor,
            int contentAlpha) {

        graphics.fill(
                x,
                y,
                x + PANEL_WIDTH,
                y + FOOTER_HEIGHT,
                panelColor
        );

        graphics.outline(
                x,
                y,
                PANEL_WIDTH,
                FOOTER_HEIGHT,
                borderColor
        );

        graphics.centeredText(
                font,
                "+ " + hiddenCount + " fuentes más",
                x + PANEL_WIDTH / 2,
                y + 3,
                argb(contentAlpha, 0xAAAAAA)
        );
    }

    private static void drawSourceIcon(
            GuiGraphicsExtractor graphics,
            PlaybackHudSource.SourceType type,
            int x,
            int y,
            int alpha) {

        int color = argb(alpha, 0xB8B8B8);
        int dark = argb(alpha, 0x686868);

        if (type == PlaybackHudSource.SourceType.DISCMAN) {
            // Mini audífonos: arco + dos copas.
            graphics.fill(x + 2, y, x + 7, y + 1, color);
            graphics.fill(x + 1, y + 1, x + 2, y + 4, color);
            graphics.fill(x + 7, y + 1, x + 8, y + 4, color);
            graphics.fill(x, y + 3, x + 2, y + 7, dark);
            graphics.fill(x + 7, y + 3, x + 9, y + 7, dark);
            return;
        }

        // Mini vinilo / tocadiscos.
        graphics.fill(x + 3, y, x + 6, y + 1, color);
        graphics.fill(x + 1, y + 1, x + 8, y + 2, color);
        graphics.fill(x, y + 2, x + 9, y + 7, color);
        graphics.fill(x + 1, y + 7, x + 8, y + 8, color);
        graphics.fill(x + 3, y + 8, x + 6, y + 9, color);
        graphics.fill(x + 4, y + 4, x + 5, y + 5, dark);
    }

    private static PlaybackInfo resolvePlayback(
            AlbumData album,
            long playbackTicks) {

        long accumulated = 0L;

        for (int index = 0; index < album.trackCount(); index++) {
            TrackData track = album.getTrack(index);

            if (track == null) {
                continue;
            }

            long duration = Math.max(1L, track.durationTicks());

            if (playbackTicks < accumulated + duration) {
                return new PlaybackInfo(
                        index,
                        track,
                        Math.max(0L, playbackTicks - accumulated),
                        duration
                );
            }

            accumulated += duration;
        }

        int lastIndex = Math.max(0, album.trackCount() - 1);
        TrackData lastTrack = album.getTrack(lastIndex);

        if (lastTrack == null) {
            return null;
        }

        long duration = Math.max(1L, lastTrack.durationTicks());

        return new PlaybackInfo(
                lastIndex,
                lastTrack,
                duration,
                duration
        );
    }

    private static void drawCover(
            GuiGraphicsExtractor graphics,
            AlbumData album,
            int x,
            int y,
            int size,
            int alpha) {

        Identifier texture = getCoverTexture(album);

        if (texture == null) {
            graphics.fill(
                    x,
                    y,
                    x + size,
                    y + size,
                    argb(alpha, 0x1E1E1E)
            );

            graphics.centeredText(
                    Minecraft.getInstance().font,
                    "♪",
                    x + size / 2,
                    y + size / 2 - 4,
                    argb(alpha, 0x777777)
            );
            return;
        }

        int drawWidth = size;
        int drawHeight = size;

        AlbumCoverTextureManager.CoverSize source =
                AlbumCoverTextureManager.getSize(texture);

        if (source != null) {
            if (source.width() >= source.height()) {
                drawHeight = Math.max(
                        1,
                        Math.round(
                                size
                                        * (
                                            source.height()
                                                    / (float) source.width()
                                        )
                        )
                );
            } else {
                drawWidth = Math.max(
                        1,
                        Math.round(
                                size
                                        * (
                                            source.width()
                                                    / (float) source.height()
                                        )
                        )
                );
            }
        }

        int tint = argb(alpha, 0xFFFFFF);

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
                drawHeight,
                tint
        );
    }

    private static Identifier getCoverTexture(AlbumData album) {
        String coverFile = album.coverFile();

        if (coverFile == null || coverFile.isBlank()) {
            return null;
        }

        try {
            Path path = Path.of(coverFile)
                    .toAbsolutePath()
                    .normalize();

            if (!Files.isRegularFile(path)) {
                return null;
            }

            AlbumCoverResolver.CoverResult cover =
                    new AlbumCoverResolver.CoverResult(
                            AlbumCoverResolver.SourceType.FILE,
                            path,
                            null
                    );

            return AlbumCoverTextureManager.getTexture(cover);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int argb(int alpha, int rgb) {
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    private static String fit(
            Font font,
            String value,
            int width) {

        if (font.width(value) <= width) {
            return value;
        }

        String ellipsis = "...";
        int usable = Math.max(8, width - font.width(ellipsis));
        return font.plainSubstrByWidth(value, usable) + ellipsis;
    }

    private static String safe(
            String value,
            String fallback) {

        return value == null || value.isBlank()
                ? fallback
                : value;
    }

    private static String formatTicks(long ticks) {
        long totalSeconds = Math.max(0L, ticks / 20L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private record PlaybackInfo(
            int trackIndex,
            TrackData track,
            long elapsedInTrackTicks,
            long trackDurationTicks) {
    }
}
