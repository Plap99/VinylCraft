package com.radig.vinylcraft.item;

import com.radig.vinylcraft.music.AlbumData;
import com.radig.vinylcraft.music.ModAlbums;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class VinylItem extends Item {

    public VinylItem(Properties properties) {
        super(properties);
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