package com.radig.vinylcraft.client;

import com.radig.vinylcraft.VinylCraft;
import com.radig.vinylcraft.block.entity.ModBlockEntities;
import com.radig.vinylcraft.client.render.VinylPlayerBlockEntityRenderer;
import com.radig.vinylcraft.client.config.VinylHudConfig;
import com.radig.vinylcraft.client.config.VinylHudSettingsScreen;
import com.radig.vinylcraft.client.render.VinylPlayerTonearmModel;
import com.radig.vinylcraft.client.render.VinylPlayerButtonModel;
import com.radig.vinylcraft.client.render.VinylRecorderBlockEntityRenderer;
import com.radig.vinylcraft.client.render.VinylRecorderTonearmModel;
import com.radig.vinylcraft.client.render.AlbumFrameBlockEntityRenderer;
import com.radig.vinylcraft.client.sound.VinylPlayerSoundManager;
import com.radig.vinylcraft.client.music.RecordedAlbumStore;
import com.radig.vinylcraft.sound.VinylPlayerAudioBridge;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

import com.radig.vinylcraft.item.ModItems;
import com.radig.vinylcraft.item.VinylData;
import com.radig.vinylcraft.music.AlbumData;
import com.radig.vinylcraft.music.ModAlbums;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

import com.radig.vinylcraft.client.library.MusicLibraryEntry;
import com.radig.vinylcraft.client.library.MusicLibraryScanner;

import com.radig.vinylcraft.client.library.AudioMetadata;
import com.radig.vinylcraft.client.library.AudioMetadataReader;

import com.radig.vinylcraft.client.library.MusicLibraryConfig;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;
import com.radig.vinylcraft.client.library.MusicLibraryScreen;
import com.radig.vinylcraft.client.recorder.VinylRecorderScreen;
import com.radig.vinylcraft.client.recorder.VinylAlbumInfoScreen;
import com.radig.vinylcraft.recorder.VinylRecorderClientBridge;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

public class VinylCraftClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        RecordedAlbumStore.load();
        registerVinylTooltips();
        MusicLibraryConfig.load();
        VinylHudConfig.load();

        VinylRecorderClientBridge.setOpenScreenHandler(
                pos -> {
                    var client = net.minecraft.client.Minecraft.getInstance();

                    if (client.gui.screen() == null) {
                        client.gui.setScreen(
                                new VinylRecorderScreen(pos)
                        );
                    }
                }
        );

        VinylRecorderClientBridge.setOpenAlbumInfoHandler(pos -> {
            var client = net.minecraft.client.Minecraft.getInstance();
            if (client.gui.screen() == null) {
                client.gui.setScreen(new VinylAlbumInfoScreen(pos));
            }
        });

        ModelLayerRegistry.registerModelLayer(
                VinylPlayerBlockEntityRenderer.TONEARM_LAYER,
                VinylPlayerTonearmModel::createLayer
        );

        ModelLayerRegistry.registerModelLayer(
                VinylPlayerBlockEntityRenderer.BUTTON_LAYER,
                VinylPlayerButtonModel::createLayer
        );

        ModelLayerRegistry.registerModelLayer(
                VinylRecorderBlockEntityRenderer.TONEARM_LAYER,
                VinylRecorderTonearmModel::createLayer
        );

        BlockEntityRenderers.register(
                ModBlockEntities.VINYL_PLAYER,
                VinylPlayerBlockEntityRenderer::new
        );

        BlockEntityRenderers.register(
                ModBlockEntities.VINYL_RECORDER,
                VinylRecorderBlockEntityRenderer::new
        );

        BlockEntityRenderers.register(
                ModBlockEntities.ALBUM_FRAME,
                AlbumFrameBlockEntityRenderer::new
        );

        /*
         * Conectamos el BlockEntity común
         * con el sistema de audio del cliente.
         */
        VinylPlayerAudioBridge.setClientTicker(
                VinylPlayerSoundManager::tickPlayer
        );

        ClientTickEvents.END_CLIENT_TICK.register(
                client -> {

                        while (OPEN_HUD_SETTINGS_KEY.consumeClick()) {

                                if (client.gui.screen() != null) {
                                        continue;
                                }

                                client.gui.setScreen(
                                        new VinylHudSettingsScreen()
                                );
                        }

                        while (OPEN_LIBRARY_KEY.consumeClick()) {

                                if (client.gui.screen() != null) {
                                        continue;
                                }

                                client.gui.setScreen(
                                        new MusicLibraryScreen()
                                );
                        }
                }
        );
    }

    private static void registerVinylTooltips() {
        ItemTooltipCallback.EVENT.register(
                (stack, tooltipContext, tooltipFlag, lines) -> {

                        if (!stack.is(ModItems.BLANK_VINYL)) {
                        return;
                        }

                        String albumId =
                                VinylData.getAlbumId(stack);

                        // Vinilo virgen
                        if (albumId == null) {

                        lines.add(
                                Component.literal("Sin grabar")
                                        .withStyle(ChatFormatting.GRAY)
                        );

                        lines.add(
                                Component.literal(
                                        "Usa este vinilo para crear un álbum"
                                ).withStyle(ChatFormatting.DARK_GRAY)
                        );

                        return;
                        }

                        AlbumData album =
                                ModAlbums.get(albumId);

                        if (album == null) {
                        lines.add(
                                Component.literal("Álbum no disponible en esta biblioteca")
                                        .withStyle(ChatFormatting.RED)
                        );
                        return;
                        }

                        // Artista
                        if (
                                album.artist() != null
                                && !album.artist().isBlank()
                        ) {
                        lines.add(
                                Component.literal(album.artist())
                                        .withStyle(ChatFormatting.GRAY)
                        );
                        }

                        // Número de pistas
                        int tracks = album.trackCount();

                        lines.add(
                                Component.literal(
                                        tracks
                                                + (tracks == 1
                                                ? " pista"
                                                : " pistas")
                                ).withStyle(ChatFormatting.DARK_GRAY)
                        );

                        // Duración total
                        long totalSeconds =
                                album.totalDurationMillis() / 1000L;

                        long minutes =
                                totalSeconds / 60L;

                        long seconds =
                                totalSeconds % 60L;

                        lines.add(
                                Component.literal(
                                        String.format(
                                                "%02d:%02d",
                                                minutes,
                                                seconds
                                        )
                                ).withStyle(ChatFormatting.DARK_GRAY)
                        );

                        int wallSize = VinylData.getWallSize(stack);

                        lines.add(
                                Component.literal(
                                        "Cuadro: " + wallSize + "x" + wallSize
                                ).withStyle(ChatFormatting.DARK_GRAY)
                        );

                        lines.add(
                                Component.literal(
                                        "Clic derecho en una pared para mostrar la portada"
                                ).withStyle(ChatFormatting.DARK_GRAY)
                        );
                }
        );
        }

        private static void testMusicLibraryScanner() {

                Path musicFolder =
                        Path.of(
                                "G:\\Music\\Panic! At The Disco"
                        );

                System.out.println();
                System.out.println(
                        "========== VINYLCRAFT MUSIC LIBRARY =========="
                );

                MusicLibraryEntry library =
                        MusicLibraryScanner.scan(
                                musicFolder
                        );

                if (library == null) {

                        System.out.println(
                                "[VinylCraft] No se pudo analizar la carpeta:"
                        );

                        System.out.println(
                                musicFolder
                        );

                        return;
                }

                printLibraryTree(
                        library,
                        0
                );

                System.out.println(
                        "=============================================="
                );

                System.out.println();
                }


                private static void printLibraryTree(
                        MusicLibraryEntry entry,
                        int depth) {

                String indent =
                        "  ".repeat(depth);

                String icon =
                        switch (entry.type()) {

                                case FOLDER ->
                                        "[DIR] ";

                                case AUDIO_FILE ->
                                        "[AUDIO] ";

                                case IMAGE_FILE ->
                                        "[IMAGE] ";
                        };

                System.out.println(
                        indent
                                + icon
                                + entry.name()
                );

                for (
                        MusicLibraryEntry child
                                : entry.children()
                ) {

                        printLibraryTree(
                                child,
                                depth + 1
                        );
                }
        }

        private static void testAudioMetadata() {
                Path audioFile =
                        Path.of(
                                "G:\\Music\\Panic! At The Disco"
                                + "\\Death of a Bachelor"
                                + "\\10 House of Memories.mp3"
                        );

                System.out.println();
                System.out.println(
                        "========== VINYLCRAFT METADATA =========="
                );

                AudioMetadata metadata =
                        AudioMetadataReader.read(audioFile);

                if (metadata == null) {

                        System.out.println(
                                "[VinylCraft] No se pudieron obtener "
                                        + "los metadatos."
                        );

                        System.out.println(
                                "=========================================="
                        );

                        return;
                }

                System.out.println(
                        "Archivo:      "
                                + metadata.file().getFileName()
                );

                System.out.println(
                        "Titulo:       "
                                + metadata.title()
                );

                System.out.println(
                        "Artista:      "
                                + metadata.artist()
                );

                System.out.println(
                        "Album:        "
                                + metadata.album()
                );

                System.out.println(
                        "Album Artist: "
                                + metadata.albumArtist()
                );

                System.out.println(
                        "Pista:        "
                                + metadata.trackNumber()
                );

                System.out.println(
                        "Disco:        "
                                + metadata.discNumber()
                );

                System.out.println(
                        "Año:          "
                                + metadata.year()
                );

                System.out.println(
                        "Duracion:     "
                                + metadata.formattedDuration()
                );

                System.out.println(
                        "Portada:      "
                                + (
                                        metadata.hasEmbeddedCover()
                                                ? "SI"
                                                : "NO"
                                )
                );

                if (metadata.hasEmbeddedCover()) {

                        System.out.println(
                                "Portada bytes: "
                                        + metadata.embeddedCover().length
                        );
                }

                System.out.println(
                        "=========================================="
                );

                System.out.println();
        }

        private static final KeyMapping.Category VINYLCRAFT_CATEGORY =
                KeyMapping.Category.register(
                        VinylCraft.id("vinylcraft")
                );

        private static final KeyMapping OPEN_HUD_SETTINGS_KEY =
                KeyMappingHelper.registerKeyMapping(
                        new KeyMapping(
                                "key.vinylcraft.open_hud_settings",
                                InputConstants.Type.KEYSYM,
                                GLFW.GLFW_KEY_F6,
                                VINYLCRAFT_CATEGORY
                        )
                );

        private static final KeyMapping OPEN_LIBRARY_KEY =
                KeyMappingHelper.registerKeyMapping(
                        new KeyMapping(
                                "key.vinylcraft.open_library",
                                InputConstants.Type.KEYSYM,
                                GLFW.GLFW_KEY_F7,
                                VINYLCRAFT_CATEGORY
                        )
                );
        }