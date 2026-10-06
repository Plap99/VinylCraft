package com.radig.vinylcraft.client.render;

import java.nio.file.Files;
import java.nio.file.Path;

import com.radig.vinylcraft.block.entity.VinylPlayerBlockEntity;
import com.radig.vinylcraft.client.config.VinylHudConfig;
import com.radig.vinylcraft.client.library.AlbumCoverResolver;
import com.radig.vinylcraft.client.library.AlbumCoverTextureManager;
import com.radig.vinylcraft.client.sound.VinylPlayerSoundManager;
import com.radig.vinylcraft.item.VinylData;
import com.radig.vinylcraft.music.AlbumData;
import com.radig.vinylcraft.music.ModAlbums;
import com.radig.vinylcraft.music.TrackData;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** HUD compacto de reproducción de VinylCraft. */
public final class VinylPlaybackHud {

    private static final int MARGIN = 8;
    private static final int PANEL_WIDTH = 216;
    private static final int PANEL_HEIGHT = 60;
    private static final int COVER_SIZE = 48;

    private VinylPlaybackHud() {
    }

    public static void extractRenderState(
            GuiGraphicsExtractor graphics) {

        Minecraft minecraft = Minecraft.getInstance();

        if (
                minecraft.player == null
                        || minecraft.level == null
                        || minecraft.gui.screen() != null
                        || minecraft.gui.hud.isHidden()
                        || !VinylHudConfig.isEnabled()
        ) {
            return;
        }

        VinylPlayerBlockEntity playerEntity =
                VinylPlayerSoundManager.getHudPlayer();

        if (playerEntity == null) {
            return;
        }

        String albumId = VinylData.getAlbumId(playerEntity.getVinyl());
        AlbumData album = ModAlbums.get(albumId);

        if (album == null || album.isEmpty()) {
            return;
        }

        PlaybackInfo playback = resolvePlayback(
                album,
                playerEntity.getPlaybackTicks()
        );

        if (playback == null || playback.track() == null) {
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

            int panelX = switch (VinylHudConfig.getPosition()) {
                case TOP_LEFT, BOTTOM_LEFT -> MARGIN;
                case TOP_RIGHT, BOTTOM_RIGHT ->
                        scaledGuiWidth - PANEL_WIDTH - MARGIN;
            };

            int panelY = switch (VinylHudConfig.getPosition()) {
                case TOP_LEFT, TOP_RIGHT -> MARGIN;
                case BOTTOM_LEFT, BOTTOM_RIGHT ->
                        scaledGuiHeight - PANEL_HEIGHT - MARGIN;
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

            Font font = minecraft.font;
            int textX = coverX + COVER_SIZE + 8;
            int textWidth = PANEL_WIDTH - COVER_SIZE - 20;
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
        } finally {
            graphics.pose().popMatrix();
        }
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
