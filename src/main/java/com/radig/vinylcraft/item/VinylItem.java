package com.radig.vinylcraft.item;

import com.radig.vinylcraft.music.AlbumData;
import com.radig.vinylcraft.music.ModAlbums;
import com.radig.vinylcraft.block.AlbumFrameBlock;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class VinylItem extends Item {

    public VinylItem(Properties properties) {
        super(properties);
    }


    @Override
    public InteractionResult useOn(UseOnContext context) {

        ItemStack stack = context.getItemInHand();
        String albumId = VinylData.getAlbumId(stack);

        if (albumId == null || albumId.isBlank()) {
            return super.useOn(context);
        }

        Direction face = context.getClickedFace();

        if (face == null || !face.getAxis().isHorizontal()) {
            return super.useOn(context);
        }

        BlockPos target = context.getClickedPos().relative(face);
        int size = VinylData.getWallSize(stack);

        boolean placed = AlbumFrameBlock.placeAlbum(
                context.getLevel(),
                target,
                face,
                stack,
                size,
                context.getPlayer()
        );

        return placed
                ? InteractionResult.SUCCESS
                : InteractionResult.FAIL;
    }

    @Override
    public Component getName(ItemStack stack) {

        String albumId =
                VinylData.getAlbumId(stack);

        // Si no tiene álbum, conserva el nombre normal
        // definido en el archivo de idioma.
        if (albumId == null) {
            return super.getName(stack);
        }

        AlbumData album =
                ModAlbums.get(albumId);

        // Si por alguna razón el álbum ya no existe,
        // conservamos el nombre normal.
        if (album == null) {
            return super.getName(stack);
        }

        return Component.literal(
                album.title()
        );
    }
}