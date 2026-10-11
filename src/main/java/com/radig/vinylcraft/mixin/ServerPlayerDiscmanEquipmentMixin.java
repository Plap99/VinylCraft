package com.radig.vinylcraft.mixin;

import com.radig.vinylcraft.player.DiscmanEquipmentHolder;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDiscmanEquipmentMixin {

    /** Conserva los slots durante respawn cuando vanilla conserva el inventario. */
    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void vinylcraft$restoreDiscmanEquipment(
            ServerPlayer oldPlayer,
            boolean alive,
            CallbackInfo ci) {

        DiscmanEquipmentHolder from = (DiscmanEquipmentHolder) oldPlayer;
        DiscmanEquipmentHolder to = (DiscmanEquipmentHolder) (Object) this;

        SimpleContainer source = from.vinylcraft$getDiscmanEquipment();
        SimpleContainer target = to.vinylcraft$getDiscmanEquipment();

        for (int index = 0; index < source.getContainerSize(); index++) {
            target.setItem(index, source.getItem(index).copy());
        }
    }
}
