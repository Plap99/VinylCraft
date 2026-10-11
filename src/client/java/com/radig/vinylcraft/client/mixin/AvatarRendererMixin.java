package com.radig.vinylcraft.client.mixin;

import com.radig.vinylcraft.client.render.becoya.BecoyaRenderData;
import com.radig.vinylcraft.item.DiscmanData;
import com.radig.vinylcraft.item.HeadphonesItem;
import com.radig.vinylcraft.player.DiscmanEquipment;

import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Copia el estado de Becoya's Module al AvatarRenderState seguro para render. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL")
    )
    private void vinylcraft$extractBecoyaAccessories(
            Avatar avatar,
            AvatarRenderState state,
            float partialTick,
            CallbackInfo ci) {

        boolean hasDiscman = false;
        boolean hasHeadphones = false;
        String color = "black";

        if (avatar instanceof Player player) {
            ItemStack discman = DiscmanData.findEquipped(player);
            ItemStack headphones = DiscmanEquipment.getHeadphones(player);
            hasDiscman = !discman.isEmpty();
            hasHeadphones = headphones.getItem() instanceof HeadphonesItem;
            if (headphones.getItem() instanceof HeadphonesItem item) {
                color = item.colorName();
            }
        }

        FabricRenderState fabric = (FabricRenderState) (Object) state;
        fabric.setData(BecoyaRenderData.HAS_DISCMAN, hasDiscman);
        fabric.setData(BecoyaRenderData.HAS_HEADPHONES, hasHeadphones);
        fabric.setData(BecoyaRenderData.HEADPHONES_COLOR, color);
    }
}
