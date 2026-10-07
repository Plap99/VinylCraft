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


    public static String getTrackDurationsEncoded(
            ItemStack stack) {

        if (stack == null || stack.isEmpty()) {
            return null;
        }

        return stack.get(
                ModDataComponents.ALBUM_TRACK_DURATIONS
        );
    }


    public static void setTrackDurationsEncoded(
            ItemStack stack,
            String encoded) {

        if (
                stack == null
                        || stack.isEmpty()
                        || encoded == null
                        || encoded.isBlank()
        ) {
            return;
        }

        stack.set(
                ModDataComponents.ALBUM_TRACK_DURATIONS,
                encoded
        );
    }


    public static long[] getTrackDurationsTicks(
            ItemStack stack) {

        String encoded = getTrackDurationsEncoded(stack);

        if (encoded == null || encoded.isBlank()) {
            return new long[0];
        }

        String[] parts = encoded.split(",");
        long[] result = new long[parts.length];

        int count = 0;

        for (String part : parts) {
            try {
                long value = Long.parseLong(part.trim());

                if (value > 0L) {
                    result[count++] = value;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        if (count == result.length) {
            return result;
        }

        return java.util.Arrays.copyOf(result, count);
    }




    public static int getWallSize(ItemStack stack) {

        if (stack == null || stack.isEmpty()) {
            return 1;
        }

        Integer size = stack.get(ModDataComponents.ALBUM_WALL_SIZE);
        return size == null ? 1 : Math.max(1, Math.min(10, size));
    }


    public static void setWallSize(ItemStack stack, int size) {

        if (stack == null || stack.isEmpty()) {
            return;
        }

        stack.set(
                ModDataComponents.ALBUM_WALL_SIZE,
                Math.max(1, Math.min(10, size))
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

        stack.remove(
                ModDataComponents.ALBUM_TRACK_DURATIONS
        );

        stack.remove(
                ModDataComponents.ALBUM_WALL_SIZE
        );
    }
}