package com.radig.vinylcraft.player;

import com.radig.vinylcraft.item.DiscmanData;
import com.radig.vinylcraft.item.HeadphonesItem;
import com.radig.vinylcraft.item.ModItems;
import com.radig.vinylcraft.item.VinylData;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Acceso común a los slots reales Audífonos/Discman/Vinilo del jugador. */
public final class DiscmanEquipment {

    private DiscmanEquipment() {
    }

    public static SimpleContainer get(Player player) {
        if (player instanceof DiscmanEquipmentHolder holder) {
            return holder.vinylcraft$getDiscmanEquipment();
        }
        return new SimpleContainer(DiscmanEquipmentHolder.SIZE);
    }

    public static ItemStack getHeadphones(Player player) {
        if (player == null) return ItemStack.EMPTY;
        return get(player).getItem(DiscmanEquipmentHolder.HEADPHONES_SLOT);
    }

    public static ItemStack getDiscman(Player player) {
        if (player == null) return ItemStack.EMPTY;
        return get(player).getItem(DiscmanEquipmentHolder.DISCMAN_SLOT);
    }

    public static ItemStack getVinyl(Player player) {
        if (player == null) return ItemStack.EMPTY;
        return get(player).getItem(DiscmanEquipmentHolder.VINYL_SLOT);
    }

    public static boolean hasHeadphones(Player player) {
        ItemStack stack = getHeadphones(player);
        return !stack.isEmpty() && stack.getItem() instanceof HeadphonesItem;
    }

    public static boolean hasDiscman(Player player) {
        return DiscmanData.isDiscman(getDiscman(player));
    }

    public static boolean hasRecordedVinyl(Player player) {
        ItemStack vinyl = getVinyl(player);
        return vinyl.is(ModItems.BLANK_VINYL) && VinylData.hasAlbum(vinyl);
    }

    /** Controles disponibles aunque falten audífonos: reproduce en silencio. */
    public static boolean isReady(Player player) {
        return hasDiscman(player) && hasRecordedVinyl(player);
    }

    public static boolean canHear(Player player) {
        return isReady(player) && hasHeadphones(player);
    }

    public static void stopPlayback(Player player) {
        ItemStack discman = getDiscman(player);
        if (!discman.isEmpty()) {
            DiscmanData.setState(discman, DiscmanData.STOPPED);
            DiscmanData.setPlaybackTicks(discman, 0L);
        }
    }
}
