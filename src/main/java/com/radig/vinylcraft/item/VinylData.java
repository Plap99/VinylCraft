package com.radig.vinylcraft.item;

import com.radig.vinylcraft.component.ModDataComponents;

import net.minecraft.world.item.ItemStack;

public final class VinylData {

    private VinylData() {
    }

    public static boolean hasAlbum(
            ItemStack stack) {

        if (stack == null || stack.isEmpty()) {
            return false;
        }

        return stack.has(
                ModDataComponents.ALBUM_ID
        );
    }


    public static String getAlbumId(
            ItemStack stack) {

        if (stack == null || stack.isEmpty()) {
            return null;
        }

        return stack.get(
                ModDataComponents.ALBUM_ID
        );
    }


    public static void setAlbumId(
            ItemStack stack,
            String albumId) {

        if (
                stack == null
                || stack.isEmpty()
                || albumId == null
                || albumId.isBlank()
        ) {
            return;
        }

        stack.set(
                ModDataComponents.ALBUM_ID,
                albumId
        );
    }


    public static void clearAlbum(
            ItemStack stack) {

        if (stack == null || stack.isEmpty()) {
            return;
        }

        stack.remove(
                ModDataComponents.ALBUM_ID
        );
    }
}