package com.radig.vinylcraft.client.discman;

import java.nio.file.Path;

import com.radig.vinylcraft.client.library.AlbumCoverResolver;
import com.radig.vinylcraft.client.library.AlbumCoverTextureManager;
import com.radig.vinylcraft.client.sound.DiscmanSoundManager;
import com.radig.vinylcraft.item.DiscmanData;
import com.radig.vinylcraft.item.ModItems;
import com.radig.vinylcraft.item.VinylData;
import com.radig.vinylcraft.music.AlbumData;
import com.radig.vinylcraft.music.ModAlbums;
import com.radig.vinylcraft.music.TrackData;
import com.radig.vinylcraft.network.DiscmanActionPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** GUI funcional de Becoya's Module. */
public final class DiscmanScreen extends Screen {

    private Button insertEjectButton;
    private Button previousButton;
    private Button stopButton;
    private Button playPauseButton;
    private Button nextButton;
    private Button closeButton;

    public DiscmanScreen() {
        super(Component.literal("Becoya's Module - Discman"));
    }

    @Override
    protected void init() {
        Layout layout = layout();

        int buttonWidth = 54;
        int gap = 6;
        int controlsWidth = buttonWidth * 4 + gap * 3;
        int controlsX = layout.left + (layout.panelWidth - controlsWidth) / 2;
        int controlsY = layout.controlsY;

        previousButton = addRenderableWidget(Button.builder(
                Component.literal("|◀"),
                button -> DiscmanSoundManager.sendAction(DiscmanActionPayload.PREVIOUS)
        ).bounds(controlsX, controlsY, buttonWidth, 20).build());

        stopButton = addRenderableWidget(Button.builder(
                Component.literal("■"),
                button -> DiscmanSoundManager.sendAction(DiscmanActionPayload.STOP)
        ).bounds(controlsX + (buttonWidth + gap), controlsY, buttonWidth, 20).build());

        playPauseButton = addRenderableWidget(Button.builder(
                Component.literal("▶"),
                button -> DiscmanSoundManager.sendAction(DiscmanActionPayload.PLAY_PAUSE)
        ).bounds(controlsX + (buttonWidth + gap) * 2, controlsY, buttonWidth, 20).build());

        nextButton = addRenderableWidget(Button.builder(
                Component.literal("▶|"),
                button -> DiscmanSoundManager.sendAction(DiscmanActionPayload.NEXT)
        ).bounds(controlsX + (buttonWidth + gap) * 3, controlsY, buttonWidth, 20).build());

        insertEjectButton = addRenderableWidget(Button.builder(
                Component.literal("Insertar vinilo"),
                button -> {
                    ItemStack discman = equippedDiscman();

                    if (DiscmanData.hasVinyl(discman)) {
                        DiscmanSoundManager.sendAction(DiscmanActionPayload.EJECT);
                    } else {
                        ClientPlayNetworking.send(
                                new DiscmanActionPayload(
                                        DiscmanActionPayload.INSERT,
                                        0L
                                )
                        );
                    }
                }
        ).bounds(
                layout.left + layout.panelWidth / 2 - 85,
                layout.insertY,
                170,
                20
        ).build());

        closeButton = addRenderableWidget(Button.builder(
                Component.literal("Cerrar"),
                button -> onClose()
        ).bounds(
                layout.left + layout.panelWidth / 2 - 65,
                layout.closeY,
                130,
                20
        ).build());
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta) {

        refreshControls();

        /*
         * Primero dibujamos TODO el panel y su contenido. Los widgets se
         * extraen al final mediante super.extractRenderState(), para que el
         * rectángulo negro no quede encima de los botones.
         */
        drawPanel(graphics);

        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawPanel(GuiGraphicsExtractor graphics) {
        Layout layout = layout();

        int left = layout.left;
        int top = layout.top;
        int right = layout.right;
        int bottom = layout.bottom;

        graphics.fill(left, top, right, bottom, 0xD9000000);
        graphics.centeredText(
                font,
                "Becoya's Module - Discman",
                width / 2,
                top + 10,
                0xFFFFFFFF
        );

        ItemStack discman = equippedDiscman();

        if (discman.isEmpty()) {
            graphics.centeredText(
                    font,
                    "No hay un Discman equipado",
                    width / 2,
                    layout.contentTop + 20,
                    0xFFFF8888
            );
            graphics.centeredText(
                    font,
                    "Sostén uno y haz clic derecho para equiparlo.",
                    width / 2,
                    layout.contentTop + 40,
                    0xFFAAAAAA
            );
            return;
        }

        graphics.text(
                font,
                "Equipado",
                left + 16,
                top + 28,
                0xFF77FF99,
                false
        );

        if (!DiscmanData.hasVinyl(discman)) {
            graphics.centeredText(
                    font,
                    "Sin vinilo",
                    width / 2,
                    layout.contentTop + 24,
                    0xFFAAAAAA
            );
            graphics.centeredText(
                    font,
                    "Ten un álbum grabado en el inventario y pulsa Insertar vinilo.",
                    width / 2,
                    layout.contentTop + 44,
                    0xFF777777
            );
            return;
        }

        String albumId = VinylData.getAlbumId(discman);
        AlbumData album = albumId == null ? null : ModAlbums.get(albumId);

        if (album == null || album.isEmpty()) {
            graphics.centeredText(
                    font,
                    "Álbum no disponible en la biblioteca local",
                    width / 2,
                    layout.contentTop + 30,
                    0xFFFF8888
            );
            return;
        }

        int availableContentHeight = Math.max(58, layout.controlsY - layout.contentTop - 8);
        int coverSize = Math.max(58, Math.min(82, availableContentHeight));
        int coverX = left + 18;
        int coverY = layout.contentTop;

        drawCover(graphics, album, coverX, coverY, coverSize);

        int textX = coverX + coverSize + 18;
        int textWidth = Math.max(100, right - textX - 18);

        graphics.text(
                font,
                font.plainSubstrByWidth(album.title(), textWidth),
                textX,
                coverY,
                0xFFFFFFFF,
                true
        );
        graphics.text(
                font,
                font.plainSubstrByWidth(album.artist(), textWidth),
                textX,
                coverY + 16,
                0xFFBBBBBB,
                false
        );

        long ticks = DiscmanSoundManager.getLocalPlaybackTicks();
        int trackIndex = resolveTrackIndex(album, ticks);
        TrackData track = album.getTrack(trackIndex);
        long trackStart = resolveTrackStartTick(album, trackIndex);
        long inTrack = Math.max(0L, ticks - trackStart);

        if (track != null) {
            graphics.text(
                    font,
                    font.plainSubstrByWidth(track.title(), textWidth),
                    textX,
                    coverY + 36,
                    0xFFFFFFFF,
                    false
            );
            graphics.text(
                    font,
                    formatTicks(inTrack)
                            + " / "
                            + formatTicks(track.durationTicks()),
                    textX,
                    coverY + 52,
                    0xFFAAAAAA,
                    false
            );
            graphics.text(
                    font,
                    "Pista " + (trackIndex + 1) + " de " + album.trackCount(),
                    textX,
                    coverY + 68,
                    0xFF888888,
                    false
            );
        }
    }

    private Layout layout() {
        int margin = 8;
        int panelWidth = Math.min(470, Math.max(250, width - margin * 2));
        int panelHeight = Math.min(230, Math.max(190, height - margin * 2));
        int left = (width - panelWidth) / 2;
        int top = Math.max(margin, (height - panelHeight) / 2);
        int right = left + panelWidth;
        int bottom = Math.min(height - margin, top + panelHeight);

        /*
         * Los tres renglones de botones se anclan desde ABAJO. De este modo
         * siempre permanecen visibles incluso con GUI Scale grande.
         */
        int closeY = bottom - 24;
        int insertY = closeY - 26;
        int controlsY = insertY - 26;
        int contentTop = top + 44;

        return new Layout(
                panelWidth,
                left,
                top,
                right,
                bottom,
                contentTop,
                controlsY,
                insertY,
                closeY
        );
    }

    private void refreshControls() {
        if (insertEjectButton == null) {
            return;
        }

        ItemStack discman = equippedDiscman();

        if (discman.isEmpty()) {
            setControlsEnabled(false);
            insertEjectButton.active = false;
            insertEjectButton.setMessage(Component.literal("Insertar vinilo"));
            return;
        }

        boolean hasVinyl = DiscmanData.hasVinyl(discman);
        int state = DiscmanData.getState(discman);

        insertEjectButton.setMessage(
                Component.literal(hasVinyl ? "Expulsar vinilo" : "Insertar vinilo")
        );
        insertEjectButton.active = hasVinyl || hasRecordedVinylInInventory();
        setControlsEnabled(hasVinyl);
        playPauseButton.setMessage(
                Component.literal(state == DiscmanData.PLAYING ? "Ⅱ" : "▶")
        );
    }

    private ItemStack equippedDiscman() {
        if (minecraft == null || minecraft.player == null) {
            return ItemStack.EMPTY;
        }

        return DiscmanData.findEquipped(minecraft.player);
    }

    private boolean hasRecordedVinylInInventory() {
        if (minecraft == null || minecraft.player == null) {
            return false;
        }

        var inventory = minecraft.player.getInventory();

        for (int index = 0; index < inventory.getContainerSize(); index++) {
            ItemStack stack = inventory.getItem(index);

            if (stack.is(ModItems.BLANK_VINYL) && VinylData.hasAlbum(stack)) {
                return true;
            }
        }

        return false;
    }

    private void setControlsEnabled(boolean enabled) {
        previousButton.active = enabled;
        stopButton.active = enabled;
        playPauseButton.active = enabled;
        nextButton.active = enabled;
    }

    private static String formatTicks(long ticks) {
        long totalSeconds = Math.max(0L, ticks / 20L);
        return String.format("%02d:%02d", totalSeconds / 60L, totalSeconds % 60L);
    }

    private static int resolveTrackIndex(AlbumData album, long ticks) {
        long accumulated = 0L;

        for (int index = 0; index < album.trackCount(); index++) {
            TrackData track = album.getTrack(index);

            if (track == null) {
                continue;
            }

            long duration = Math.max(1L, track.durationTicks());

            if (ticks < accumulated + duration) {
                return index;
            }

            accumulated += duration;
        }

        return Math.max(0, album.trackCount() - 1);
    }

    private static long resolveTrackStartTick(AlbumData album, int trackIndex) {
        long accumulated = 0L;

        for (int index = 0; index < trackIndex; index++) {
            TrackData track = album.getTrack(index);

            if (track != null) {
                accumulated += Math.max(1L, track.durationTicks());
            }
        }

        return accumulated;
    }

    private void drawCover(
            GuiGraphicsExtractor graphics,
            AlbumData album,
            int x,
            int y,
            int size) {

        if (album.coverFile() == null || album.coverFile().isBlank()) {
            graphics.fill(x, y, x + size, y + size, 0xFF181818);
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

    private record Layout(
            int panelWidth,
            int left,
            int top,
            int right,
            int bottom,
            int contentTop,
            int controlsY,
            int insertY,
            int closeY) {
    }
}
