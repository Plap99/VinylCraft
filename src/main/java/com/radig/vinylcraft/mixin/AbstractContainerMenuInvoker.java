package com.radig.vinylcraft.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuInvoker {

    @Invoker("addSlot")
    Slot vinylcraft$addSlot(Slot slot);

    @Invoker("moveItemStackTo")
    boolean vinylcraft$moveItemStackTo(
            ItemStack stack,
            int startIndex,
            int endIndex,
            boolean reverseDirection);
}
