package com.radig.vinylcraft.item;

import com.radig.vinylcraft.component.ModDataComponents;
import com.radig.vinylcraft.player.DiscmanEquipment;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Estado del Discman y helpers para los slots reales de Becoya's Module. */
public final class DiscmanData {

    public static final int STOPPED = 0;
    public static final int PLAYING = 1;
    public static final int PAUSED = 2;

    private DiscmanData() {
    }

    public static boolean isDiscman(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(ModItems.DISCMAN);
    }

    /** Compatibilidad con el Paso 21 antiguo; ya no define el equipado real. */
    public static boolean isEquipped(ItemStack stack) {
        if (!isDiscman(stack)) {
            return false;
        }

        Boolean value = stack.get(ModDataComponents.DISCMAN_EQUIPPED);
        return Boolean.TRUE.equals(value);
    }

    /** Compatibilidad con mundos del Paso 21. El slot real manda desde Paso 22. */
    public static void setEquipped(ItemStack stack, boolean equipped) {
        if (!isDiscman(stack)) {
            return;
        }

        stack.set(ModDataComponents.DISCMAN_EQUIPPED, equipped);

        if (!equipped && getState(stack) == PLAYING) {
            setState(stack, PAUSED);
        }
    }

    /** Desde Paso 22 el Discman equipado es el ItemStack del slot accesorio real. */
    public static ItemStack findEquipped(Player player) {
        return DiscmanEquipment.getDiscman(player);
    }

    /** El vinilo ya no vive dentro del Discman; tiene su propio slot real. */
    public static ItemStack findVinyl(Player player) {
        return DiscmanEquipment.getVinyl(player);
    }

    public static boolean hasEquippedVinyl(Player player) {
        return DiscmanEquipment.hasRecordedVinyl(player);
    }

    /** API antigua, mantenida para que código legado compile. */
    public static boolean hasVinyl(ItemStack discman) {
        return isDiscman(discman) && VinylData.hasAlbum(discman);
    }

    public static int getState(ItemStack discman) {
        if (!isDiscman(discman)) {
            return STOPPED;
        }

        Integer value = discman.get(ModDataComponents.DISCMAN_STATE);
        return value == null ? STOPPED : Math.max(STOPPED, Math.min(PAUSED, value));
    }

    public static void setState(ItemStack discman, int state) {
        if (!isDiscman(discman)) {
            return;
        }

        discman.set(
                ModDataComponents.DISCMAN_STATE,
                Math.max(STOPPED, Math.min(PAUSED, state))
        );
    }

    public static long getPlaybackTicks(ItemStack discman) {
        if (!isDiscman(discman)) {
            return 0L;
        }

        Long value = discman.get(ModDataComponents.DISCMAN_PLAYBACK_TICKS);
        return value == null ? 0L : Math.max(0L, value);
    }

    public static void setPlaybackTicks(ItemStack discman, long ticks) {
        if (!isDiscman(discman)) {
            return;
        }

        discman.set(
                ModDataComponents.DISCMAN_PLAYBACK_TICKS,
                Math.max(0L, ticks)
        );
    }

    public static int getVolumePercent(ItemStack discman) {
        if (!isDiscman(discman)) {
            return 100;
        }

        Integer value = discman.get(ModDataComponents.DISCMAN_VOLUME);
        return value == null ? 100 : Math.max(0, Math.min(100, value));
    }

    public static void setVolumePercent(ItemStack discman, int percent) {
        if (!isDiscman(discman)) {
            return;
        }

        discman.set(
                ModDataComponents.DISCMAN_VOLUME,
                Math.max(0, Math.min(100, percent))
        );
    }

    /* ---------------------------------------------------------------------
     * LEGADO PASO 21
     * ------------------------------------------------------------------ */

    public static boolean insertVinyl(ItemStack discman, ItemStack vinyl) {
        if (
                !isDiscman(discman)
                        || hasVinyl(discman)
                        || vinyl == null
                        || !vinyl.is(ModItems.BLANK_VINYL)
                        || !VinylData.hasAlbum(vinyl)
        ) {
            return false;
        }

        String albumId = VinylData.getAlbumId(vinyl);
        String durations = VinylData.getTrackDurationsEncoded(vinyl);

        if (albumId == null || albumId.isBlank()) {
            return false;
        }

        VinylData.setAlbumId(discman, albumId);

        if (durations != null && !durations.isBlank()) {
            VinylData.setTrackDurationsEncoded(discman, durations);
        }

        VinylData.setWallSize(discman, VinylData.getWallSize(vinyl));
        setPlaybackTicks(discman, 0L);
        setState(discman, STOPPED);
        return true;
    }

    public static ItemStack ejectVinyl(ItemStack discman) {
        if (!hasVinyl(discman)) {
            return ItemStack.EMPTY;
        }

        ItemStack vinyl = new ItemStack(ModItems.BLANK_VINYL);
        String albumId = VinylData.getAlbumId(discman);
        String durations = VinylData.getTrackDurationsEncoded(discman);

        VinylData.setAlbumId(vinyl, albumId);

        if (durations != null && !durations.isBlank()) {
            VinylData.setTrackDurationsEncoded(vinyl, durations);
        }

        VinylData.setWallSize(vinyl, VinylData.getWallSize(discman));
        clearVinyl(discman);
        return vinyl;
    }

    public static void clearVinyl(ItemStack discman) {
        if (!isDiscman(discman)) {
            return;
        }

        discman.remove(ModDataComponents.ALBUM_ID);
        discman.remove(ModDataComponents.ALBUM_TRACK_DURATIONS);
        discman.remove(ModDataComponents.ALBUM_WALL_SIZE);
        setPlaybackTicks(discman, 0L);
        setState(discman, STOPPED);
    }

    /* ---------------------------------------------------------------------
     * RELOJ / PISTAS: el medio ahora es el VINILO del slot accesorio.
     * ------------------------------------------------------------------ */

    public static long[] getTrackDurationsTicks(ItemStack vinyl) {
        if (vinyl == null || vinyl.isEmpty()) {
            return new long[0];
        }

        return VinylData.getTrackDurationsTicks(vinyl);
    }

    public static long getTotalTicks(ItemStack vinyl) {
        long total = 0L;

        for (long duration : getTrackDurationsTicks(vinyl)) {
            total += Math.max(1L, duration);
        }

        return total;
    }

    public static int resolveTrackIndex(ItemStack vinyl, long playbackTicks) {
        long[] durations = getTrackDurationsTicks(vinyl);

        if (durations.length == 0) {
            return 0;
        }

        long accumulated = 0L;

        for (int index = 0; index < durations.length; index++) {
            long duration = Math.max(1L, durations[index]);

            if (playbackTicks < accumulated + duration) {
                return index;
            }

            accumulated += duration;
        }

        return durations.length - 1;
    }

    public static long trackStartTicks(ItemStack vinyl, int trackIndex) {
        long[] durations = getTrackDurationsTicks(vinyl);
        long accumulated = 0L;

        for (int index = 0; index < trackIndex && index < durations.length; index++) {
            accumulated += Math.max(1L, durations[index]);
        }

        return accumulated;
    }

    public static void previousTrack(ItemStack discman, ItemStack vinyl) {
        int current = resolveTrackIndex(vinyl, getPlaybackTicks(discman));
        int target = Math.max(0, current - 1);
        setPlaybackTicks(discman, trackStartTicks(vinyl, target));
    }

    public static void nextTrack(ItemStack discman, ItemStack vinyl) {
        long[] durations = getTrackDurationsTicks(vinyl);

        if (durations.length == 0) {
            return;
        }

        int current = resolveTrackIndex(vinyl, getPlaybackTicks(discman));
        int target = Math.min(durations.length - 1, current + 1);
        setPlaybackTicks(discman, trackStartTicks(vinyl, target));
    }
}
