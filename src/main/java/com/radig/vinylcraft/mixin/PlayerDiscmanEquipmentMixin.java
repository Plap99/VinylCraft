package com.radig.vinylcraft.mixin;

import com.radig.vinylcraft.player.DiscmanEquipmentHolder;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerDiscmanEquipmentMixin implements DiscmanEquipmentHolder {

    @Unique
    private SimpleContainer vinylcraft$discmanEquipment;

    @Override
    public SimpleContainer vinylcraft$getDiscmanEquipment() {
        if (vinylcraft$discmanEquipment == null) {
            vinylcraft$discmanEquipment = new SimpleContainer(SIZE);
        }

        return vinylcraft$discmanEquipment;
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void vinylcraft$readDiscmanEquipment(
            ValueInput input,
            CallbackInfo ci) {

        SimpleContainer container = vinylcraft$getDiscmanEquipment();

        container.setItem(
                HEADPHONES_SLOT,
                input.read("VinylCraftHeadphones", ItemStack.OPTIONAL_CODEC)
                        .orElse(ItemStack.EMPTY)
        );

        container.setItem(
                DISCMAN_SLOT,
                input.read("VinylCraftDiscman", ItemStack.OPTIONAL_CODEC)
                        .orElse(ItemStack.EMPTY)
        );

        container.setItem(
                VINYL_SLOT,
                input.read("VinylCraftDiscmanVinyl", ItemStack.OPTIONAL_CODEC)
                        .orElse(ItemStack.EMPTY)
        );
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void vinylcraft$writeDiscmanEquipment(
            ValueOutput output,
            CallbackInfo ci) {

        SimpleContainer container = vinylcraft$getDiscmanEquipment();

        output.store(
                "VinylCraftHeadphones",
                ItemStack.OPTIONAL_CODEC,
                container.getItem(HEADPHONES_SLOT)
        );

        output.store(
                "VinylCraftDiscman",
                ItemStack.OPTIONAL_CODEC,
                container.getItem(DISCMAN_SLOT)
        );

        output.store(
                "VinylCraftDiscmanVinyl",
                ItemStack.OPTIONAL_CODEC,
                container.getItem(VINYL_SLOT)
        );
    }

    @Inject(method = "dropEquipment", at = @At("TAIL"))
    private void vinylcraft$dropDiscmanEquipment(
            ServerLevel level,
            CallbackInfo ci) {

        if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
            return;
        }

        Player player = (Player) (Object) this;
        SimpleContainer container = vinylcraft$getDiscmanEquipment();

        for (int index = 0; index < container.getContainerSize(); index++) {
            ItemStack stack = container.removeItemNoUpdate(index);

            if (!stack.isEmpty()) {
                player.drop(stack, true);
            }
        }
    }
}
