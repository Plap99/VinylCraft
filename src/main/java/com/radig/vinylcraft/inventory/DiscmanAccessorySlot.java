package com.radig.vinylcraft.inventory;

import com.radig.vinylcraft.item.DiscmanData;
import com.radig.vinylcraft.item.ModItems;
import com.radig.vinylcraft.item.HeadphonesItem;
import com.radig.vinylcraft.item.VinylData;
import com.radig.vinylcraft.player.DiscmanEquipmentHolder;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Slot real del inventario para Discman o vinilo grabado. */
public final class DiscmanAccessorySlot extends Slot {

    public enum Type {
        HEADPHONES,
        DISCMAN,
        VINYL
    }

    private final Type type;

    public DiscmanAccessorySlot(
            Container container,
            int containerSlot,
            int x,
            int y,
            Type type) {

        super(container, containerSlot, x, y);
        this.type = type;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (type == Type.HEADPHONES) {
            return stack != null && !stack.isEmpty() && stack.getItem() instanceof HeadphonesItem;
        }

        if (type == Type.DISCMAN) {
            return DiscmanData.isDiscman(stack);
        }

        if (!container.getItem(DiscmanEquipmentHolder.DISCMAN_SLOT).is(ModItems.DISCMAN)) {
            return false;
        }

        return stack.is(ModItems.BLANK_VINYL)
                && VinylData.hasAlbum(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }

    @Override
    public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
        super.setByPlayer(newStack, oldStack);
        resetPlaybackIfMediaChanged();
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        super.onTake(player, stack);

        if (type == Type.DISCMAN && DiscmanData.getState(stack) == DiscmanData.PLAYING) {
            DiscmanData.setState(stack, DiscmanData.PAUSED);
        }

        if (type == Type.VINYL) {
            ItemStack discman = container.getItem(DiscmanEquipmentHolder.DISCMAN_SLOT);

            if (!discman.isEmpty()) {
                DiscmanData.setState(discman, DiscmanData.STOPPED);
                DiscmanData.setPlaybackTicks(discman, 0L);
            }
        }
    }

    private void resetPlaybackIfMediaChanged() {
        if (type != Type.VINYL) {
            return;
        }

        ItemStack discman = container.getItem(DiscmanEquipmentHolder.DISCMAN_SLOT);

        if (!discman.isEmpty()) {
            DiscmanData.setState(discman, DiscmanData.STOPPED);
            DiscmanData.setPlaybackTicks(discman, 0L);
        }
    }
}
