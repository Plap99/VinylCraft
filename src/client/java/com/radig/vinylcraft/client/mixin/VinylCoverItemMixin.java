package com.radig.vinylcraft.client.mixin;

import java.nio.file.Path;
import com.radig.vinylcraft.client.library.AlbumCoverResolver;
import com.radig.vinylcraft.client.library.AlbumCoverTextureManager;
import com.radig.vinylcraft.item.ModItems;
import com.radig.vinylcraft.item.VinylData;
import com.radig.vinylcraft.music.AlbumData;
import com.radig.vinylcraft.music.ModAlbums;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dibuja la portada real sobre la funda del vinilo grabado SOLO en los GUI.
 * No altera el ItemStack, ni el disco 3D colocado ni el render en la mano.
 * Compatible con el motor GUI por extracción de Minecraft 26.2.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class VinylCoverItemMixin {

    @Inject(
        method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",
        at = @At("TAIL")
    )
    private void vinylcraft$drawRecordedCover(
            @Nullable LivingEntity owner,
            @Nullable Level level,
            ItemStack stack,
            int x,
            int y,
            int seed,
            CallbackInfo callback) {

        if (stack == null || !stack.is(ModItems.BLANK_VINYL)) return;
        String albumId = VinylData.getAlbumId(stack);
        if (albumId == null || albumId.isBlank()) return;
        AlbumData album = ModAlbums.get(albumId);
        if (album == null || album.coverFile() == null || album.coverFile().isBlank()) return;

        try {
            var cover = new AlbumCoverResolver.CoverResult(
                AlbumCoverResolver.SourceType.FILE,
                Path.of(album.coverFile()),
                null
            );
            Identifier texture = AlbumCoverTextureManager.getTexture(cover);
            if (texture == null) return;

            GuiGraphicsExtractor gui = (GuiGraphicsExtractor) (Object) this;
            gui.blit(
                RenderPipelines.GUI_TEXTURED, texture,
                x, y,
                0.0F, 0.0F,
                16, 16, 16, 16
            );
        } catch (Exception ignored) {
            // Sin carátula local: queda visible la funda base, sin cerrar el GUI.
        }
    }
}
