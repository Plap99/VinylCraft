package com.radig.vinylcraft.client.library;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class AlbumCoverTextureManager {

    private static final Map<String, Identifier> TEXTURE_CACHE =
            new HashMap<>();

    private static final Map<Identifier, CoverSize> TEXTURE_SIZES =
            new HashMap<>();

    public record CoverSize(int width, int height) {
    }

    private static int nextTextureId = 0;

    private AlbumCoverTextureManager() {
    }

    public static Identifier getTexture(
            AlbumCoverResolver.CoverResult cover) {

        if (cover == null) {
            return null;
        }

        String cacheKey =
                createCacheKey(cover);

        Identifier cached =
                TEXTURE_CACHE.get(cacheKey);

        if (cached != null) {
            return cached;
        }

        NativeImage image = null;

        try {

            image = loadImage(cover);

            if (image == null) {
                return null;
            }

            Identifier textureId =
                    Identifier.fromNamespaceAndPath(
                            "vinylcraft",
                            "album_cover/"
                                    + nextTextureId++
                    );

            DynamicTexture texture =
                    new DynamicTexture(
                            () ->
                                    "VinylCraft album cover: "
                                            + cacheKey,
                            image
                    );

            TextureManager textureManager =
                    Minecraft
                            .getInstance()
                            .getTextureManager();

            textureManager.register(
                    textureId,
                    texture
            );

            TEXTURE_CACHE.put(
                    cacheKey,
                    textureId
            );

            TEXTURE_SIZES.put(
                    textureId,
                    new CoverSize(
                            image.getWidth(),
                            image.getHeight()
                    )
            );

            System.out.println(
                    "[VinylCraft] Textura de portada cargada: "
                            + textureId
                            + " ("
                            + image.getWidth()
                            + "x"
                            + image.getHeight()
                            + ")"
            );

            return textureId;

        } catch (Exception exception) {

            System.err.println(
                    "[VinylCraft] Error cargando portada: "
                            + cacheKey
            );

            exception.printStackTrace();

            if (
                    image != null
                            && !image.isClosed()
            ) {
                image.close();
            }

            return null;
        }
    }

    private static NativeImage loadImage(
            AlbumCoverResolver.CoverResult cover)
            throws IOException {

        if (cover.isEmbedded()) {

            byte[] data =
                    cover.data();

            if (
                    data == null
                            || data.length == 0
            ) {
                return null;
            }

            return readImageBytes(data);
        }

        Path path =
                cover.path();

        if (
                path == null
                        || !Files.isRegularFile(path)
        ) {
            return null;
        }

        byte[] data =
                Files.readAllBytes(path);

        return readImageBytes(data);
    }

    private static NativeImage readImageBytes(
            byte[] data)
            throws IOException {

        if (
                data == null
                        || data.length == 0
        ) {
            return null;
        }

        /*
         * PNG
         */
        if (isPng(data)) {

            return NativeImage.read(data);
        }

        /*
         * JPG / JPEG y otros formatos que ImageIO
         * pueda decodificar.
         */
        try (
                InputStream stream =
                        new ByteArrayInputStream(data)
        ) {

            BufferedImage bufferedImage =
                    ImageIO.read(stream);

            if (bufferedImage == null) {

                throw new IOException(
                        "Formato de imagen no compatible"
                );
            }

            return convertToNativeImage(
                    bufferedImage
            );
        }
    }

    private static boolean isPng(
            byte[] data) {

        return data.length >= 8
                && (data[0] & 0xFF) == 0x89
                && data[1] == 0x50
                && data[2] == 0x4E
                && data[3] == 0x47
                && data[4] == 0x0D
                && data[5] == 0x0A
                && data[6] == 0x1A
                && data[7] == 0x0A;
    }

    private static NativeImage convertToNativeImage(
            BufferedImage bufferedImage) {

        int width =
                bufferedImage.getWidth();

        int height =
                bufferedImage.getHeight();

        NativeImage nativeImage =
                new NativeImage(
                        width,
                        height,
                        true
                );

        for (int y = 0; y < height; y++) {

            for (int x = 0; x < width; x++) {

                int argb =
                        bufferedImage.getRGB(
                                x,
                                y
                        );

                int alpha =
                        (argb >> 24) & 0xFF;

                int red =
                        (argb >> 16) & 0xFF;

                int green =
                        (argb >> 8) & 0xFF;

                int blue =
                        argb & 0xFF;

                /*
                 * NativeImage usa ABGR en este método.
                 */
                int abgr =
                        (alpha << 24)
                                | (blue << 16)
                                | (green << 8)
                                | red;

                nativeImage.setPixelABGR(
                        x,
                        y,
                        abgr
                );
            }
        }

        return nativeImage;
    }

    private static String createCacheKey(
            AlbumCoverResolver.CoverResult cover) {

        if (cover.isEmbedded()) {

            byte[] data =
                    cover.data();

            return "embedded:"
                    + System.identityHashCode(data)
                    + ":"
                    + (data == null ? 0 : data.length);
        }

        Path path =
                cover.path();

        if (path == null) {
            return "unknown";
        }

        return path
                .toAbsolutePath()
                .normalize()
                .toString();
    }

    public static CoverSize getSize(
            Identifier textureId) {

        return TEXTURE_SIZES.get(textureId);
    }

    public static void clear() {

        Minecraft minecraft =
                Minecraft.getInstance();

        TextureManager textureManager =
                minecraft.getTextureManager();

        for (
                Identifier textureId :
                TEXTURE_CACHE.values()
        ) {

            textureManager.release(
                    textureId
            );
        }

        TEXTURE_CACHE.clear();
        TEXTURE_SIZES.clear();

        nextTextureId = 0;

        System.out.println(
                "[VinylCraft] Caché de portadas liberada"
        );
    }

    public static int size() {
        return TEXTURE_CACHE.size();
    }
}