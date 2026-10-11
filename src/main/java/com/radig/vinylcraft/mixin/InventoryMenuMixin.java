package com.radig.vinylcraft.mixin;

import com.radig.vinylcraft.inventory.DiscmanAccessorySlot;
import com.radig.vinylcraft.item.DiscmanData;
import com.radig.vinylcraft.item.HeadphonesItem;
import com.radig.vinylcraft.item.ModItems;
import com.radig.vinylcraft.item.VinylData;
import com.radig.vinylcraft.player.DiscmanEquipmentHolder;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Unique private static final int VINYLCRAFT$PLAYER_INV_START = 9;
    @Unique private static final int VINYLCRAFT$PLAYER_INV_END = 45;
    @Unique private static final int VINYLCRAFT$HEADPHONES_MENU_SLOT = 46;
    @Unique private static final int VINYLCRAFT$DISCMAN_MENU_SLOT = 47;
    @Unique private static final int VINYLCRAFT$VINYL_MENU_SLOT = 48;

    /** Sube el slot de resultado 10 px para liberar el bloque inferior derecho. */
    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/InventoryMenu;addResultSlot(Lnet/minecraft/world/entity/player/Player;II)Lnet/minecraft/world/inventory/Slot;"
            ),
            index = 2
    )
    private int vinylcraft$moveCraftResultUp(int originalY) {
        return 18;
    }

    /** Audífonos, Discman y vinilo apilados encima del escudo. */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void vinylcraft$addDiscmanSlots(
            Inventory inventory,
            boolean active,
            Player owner,
            CallbackInfo ci) {

        SimpleContainer equipment =
                ((DiscmanEquipmentHolder) owner).vinylcraft$getDiscmanEquipment();

        AbstractContainerMenuInvoker invoker =
                (AbstractContainerMenuInvoker) this;

        invoker.vinylcraft$addSlot(new DiscmanAccessorySlot(
                equipment,
                DiscmanEquipmentHolder.HEADPHONES_SLOT,
                77,
                8,
                DiscmanAccessorySlot.Type.HEADPHONES
        ));

        invoker.vinylcraft$addSlot(new DiscmanAccessorySlot(
                equipment,
                DiscmanEquipmentHolder.DISCMAN_SLOT,
                77,
                26,
                DiscmanAccessorySlot.Type.DISCMAN
        ));

        invoker.vinylcraft$addSlot(new DiscmanAccessorySlot(
                equipment,
                DiscmanEquipmentHolder.VINYL_SLOT,
                77,
                44,
                DiscmanAccessorySlot.Type.VINYL
        ));
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void vinylcraft$quickMoveAccessory(
            Player player,
            int slotIndex,
            CallbackInfoReturnable<ItemStack> cir) {

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (slotIndex < 0 || slotIndex > VINYLCRAFT$VINYL_MENU_SLOT) return;

        Slot source = menu.getSlot(slotIndex);
        if (source == null || !source.hasItem()) return;

        ItemStack stack = source.getItem();
        ItemStack original = stack.copy();
        AbstractContainerMenuInvoker invoker = (AbstractContainerMenuInvoker) this;
        boolean handled = false;

        if (slotIndex == VINYLCRAFT$HEADPHONES_MENU_SLOT) {
            handled = invoker.vinylcraft$moveItemStackTo(
                    stack, VINYLCRAFT$PLAYER_INV_START, VINYLCRAFT$PLAYER_INV_END, false
            );
        } else if (slotIndex == VINYLCRAFT$DISCMAN_MENU_SLOT) {
            if (DiscmanData.getState(stack) == DiscmanData.PLAYING) {
                DiscmanData.setState(stack, DiscmanData.PAUSED);
            }
            handled = invoker.vinylcraft$moveItemStackTo(
                    stack, VINYLCRAFT$PLAYER_INV_START, VINYLCRAFT$PLAYER_INV_END, false
            );
        } else if (slotIndex == VINYLCRAFT$VINYL_MENU_SLOT) {
            ItemStack discman = menu.getSlot(VINYLCRAFT$DISCMAN_MENU_SLOT).getItem();
            if (!discman.isEmpty()) {
                DiscmanData.setState(discman, DiscmanData.STOPPED);
                DiscmanData.setPlaybackTicks(discman, 0L);
            }
            handled = invoker.vinylcraft$moveItemStackTo(
                    stack, VINYLCRAFT$PLAYER_INV_START, VINYLCRAFT$PLAYER_INV_END, false
            );
        } else if (stack.getItem() instanceof HeadphonesItem) {
            Slot target = menu.getSlot(VINYLCRAFT$HEADPHONES_MENU_SLOT);
            if (!target.hasItem() && target.mayPlace(stack)) {
                handled = invoker.vinylcraft$moveItemStackTo(
                        stack, VINYLCRAFT$HEADPHONES_MENU_SLOT, VINYLCRAFT$HEADPHONES_MENU_SLOT + 1, false
                );
            }
        } else if (DiscmanData.isDiscman(stack)) {
            Slot target = menu.getSlot(VINYLCRAFT$DISCMAN_MENU_SLOT);
            if (!target.hasItem() && target.mayPlace(stack)) {
                handled = invoker.vinylcraft$moveItemStackTo(
                        stack, VINYLCRAFT$DISCMAN_MENU_SLOT, VINYLCRAFT$DISCMAN_MENU_SLOT + 1, false
                );
            }
        } else if (stack.is(ModItems.BLANK_VINYL) && VinylData.hasAlbum(stack)) {
            Slot target = menu.getSlot(VINYLCRAFT$VINYL_MENU_SLOT);
            if (!target.hasItem() && target.mayPlace(stack)) {
                handled = invoker.vinylcraft$moveItemStackTo(
                        stack, VINYLCRAFT$VINYL_MENU_SLOT, VINYLCRAFT$VINYL_MENU_SLOT + 1, false
                );
            }
        }

        if (!handled) return;

        if (stack.isEmpty()) source.setByPlayer(ItemStack.EMPTY);
        else source.setChanged();

        cir.setReturnValue(original);
    }
}
