package com.radig.vinylcraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Reproductor portátil de Becoya's Module. Ahora se equipa en un slot real. */
public final class DiscmanItem extends Item {

    public DiscmanItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(
            Level level,
            Player player,
            InteractionHand hand) {

        ItemStack current = player.getItemInHand(hand);

        if (!current.is(ModItems.DISCMAN)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            player.sendOverlayMessage(
                    Component.literal(
                            "Coloca el Discman en su slot del inventario"
                    )
            );
        }

        return InteractionResult.SUCCESS;
    }
}
