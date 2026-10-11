package com.radig.vinylcraft.client.mixin;

import com.radig.vinylcraft.client.discman.DiscmanControlButton;
import com.radig.vinylcraft.client.discman.DiscmanVolumeSlider;
import com.radig.vinylcraft.client.render.VinylPlaybackHud;
import com.radig.vinylcraft.client.sound.DiscmanSoundManager;
import com.radig.vinylcraft.item.DiscmanData;
import com.radig.vinylcraft.item.VinylData;
import com.radig.vinylcraft.network.DiscmanActionPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Becoya's Module integrado al inventario vanilla. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    @Unique private DiscmanControlButton vinylcraft$previousButton;
    @Unique private DiscmanControlButton vinylcraft$stopButton;
    @Unique private DiscmanControlButton vinylcraft$playPauseButton;
    @Unique private DiscmanControlButton vinylcraft$nextButton;
    @Unique private DiscmanVolumeSlider vinylcraft$volumeSlider;

    @Unique private static final int BUTTON_SIZE = 11;
    @Unique private static final int BUTTON_GAP = 2;
    @Unique private static final int CONTROL_BLOCK_WIDTH = BUTTON_SIZE * 2 + BUTTON_GAP;
    @Unique private static final int SLIDER_WIDTH = CONTROL_BLOCK_WIDTH + 2;
    @Unique private static final int SLIDER_HEIGHT = 10;

    @Inject(method = "init", at = @At("TAIL"))
    private void vinylcraft$addDiscmanControls(CallbackInfo ci) {
        InventoryScreen self = (InventoryScreen) (Object) this;
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) self;
        ScreenInvoker screen = (ScreenInvoker) self;

        int left = accessor.vinylcraft$getLeftPos();
        int top = accessor.vinylcraft$getTopPos();
        int controlsX = left + 141;
        int controlsY = top + 54;

        vinylcraft$stopButton = screen.vinylcraft$addRenderableWidget(
                new DiscmanControlButton(
                        controlsX, controlsY, BUTTON_SIZE, BUTTON_SIZE,
                        Component.literal("■"),
                        button -> DiscmanSoundManager.sendAction(DiscmanActionPayload.STOP),
                        DiscmanControlButton.Theme.RED
                )
        );
        vinylcraft$playPauseButton = screen.vinylcraft$addRenderableWidget(
                new DiscmanControlButton(
                        controlsX + BUTTON_SIZE + BUTTON_GAP, controlsY,
                        BUTTON_SIZE, BUTTON_SIZE,
                        Component.literal("▶"),
                        button -> DiscmanSoundManager.sendAction(DiscmanActionPayload.PLAY_PAUSE),
                        DiscmanControlButton.Theme.GREEN
                )
        );
        vinylcraft$previousButton = screen.vinylcraft$addRenderableWidget(
                new DiscmanControlButton(
                        controlsX, controlsY + BUTTON_SIZE + BUTTON_GAP,
                        BUTTON_SIZE, BUTTON_SIZE,
                        Component.literal("«"),
                        button -> DiscmanSoundManager.sendAction(DiscmanActionPayload.PREVIOUS),
                        DiscmanControlButton.Theme.BLUE
                )
        );
        vinylcraft$nextButton = screen.vinylcraft$addRenderableWidget(
                new DiscmanControlButton(
                        controlsX + BUTTON_SIZE + BUTTON_GAP,
                        controlsY + BUTTON_SIZE + BUTTON_GAP,
                        BUTTON_SIZE, BUTTON_SIZE,
                        Component.literal("»"),
                        button -> DiscmanSoundManager.sendAction(DiscmanActionPayload.NEXT),
                        DiscmanControlButton.Theme.BLUE
                )
        );

        /* Barra exactamente del ancho de los dos botones superiores. */
        vinylcraft$volumeSlider = screen.vinylcraft$addRenderableWidget(
                new DiscmanVolumeSlider(
                        controlsX,
                        controlsY - 14,
                        CONTROL_BLOCK_WIDTH,
                        SLIDER_HEIGHT
                )
        );
        vinylcraft$refreshDiscmanControls();
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void vinylcraft$refreshBeforeRender(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        vinylcraft$refreshDiscmanControls();
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void vinylcraft$drawDiscmanHudOverInventory(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        VinylPlaybackHud.extractDiscmanInventoryRenderState(graphics);
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void vinylcraft$drawDiscmanSlotsAndCraftingPatch(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {

        InventoryScreen self = (InventoryScreen) (Object) this;
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) self;
        int left = accessor.vinylcraft$getLeftPos();
        int top = accessor.vinylcraft$getTopPos();

        /* Slots extra: Audífonos, Discman y Vinilo. */
        for (int y : new int[]{7, 25, 43}) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    AbstractContainerScreen.INVENTORY_LOCATION,
                    left + 76, top + y,
                    76.0F, 61.0F,
                    18, 18, 256, 256
            );
        }

        /* Siluetas tipo vanilla para identificar cada slot. */
        vinylcraft$drawHeadphonesSlotIcon(graphics, left + 76, top + 7);
        vinylcraft$drawDiscmanSlotIcon(graphics, left + 76, top + 25);
        vinylcraft$drawVinylSlotIcon(graphics, left + 76, top + 43);

        /*
         * El slot real de resultado fue subido. Cubrimos la zona original y
         * redibujamos flecha+resultado sin comernos el borde derecho.
         */
        /*
         * Limpiar COMPLETA la posición vanilla antigua de flecha/resultado.
         * Terminamos antes del borde derecho real para no fabricar líneas
         * adicionales sobre el marco vanilla.
         */
        graphics.fill(
                left + 133, top + 26,
                left + 173, top + 50,
                0xFFC6C6C6
        );

        /* Flecha vanilla movida arriba. */
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                AbstractContainerScreen.INVENTORY_LOCATION,
                left + 133, top + 19,
                133.0F, 30.0F,
                18, 14, 256, 256
        );

        /* Marco del resultado movido arriba. */
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                AbstractContainerScreen.INVENTORY_LOCATION,
                left + 153, top + 17,
                152.0F, 27.0F,
                20, 20, 256, 256
        );
    }

    @Unique
    private void vinylcraft$refreshDiscmanControls() {
        if (vinylcraft$playPauseButton == null || vinylcraft$volumeSlider == null) return;

        InventoryScreen self = (InventoryScreen) (Object) this;
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) self;
        int left = accessor.vinylcraft$getLeftPos();
        int top = accessor.vinylcraft$getTopPos();
        int controlsX = left + 141;
        int controlsY = top + 54;

        vinylcraft$stopButton.setX(controlsX);
        vinylcraft$stopButton.setY(controlsY);
        vinylcraft$playPauseButton.setX(controlsX + BUTTON_SIZE + BUTTON_GAP);
        vinylcraft$playPauseButton.setY(controlsY);
        vinylcraft$previousButton.setX(controlsX);
        vinylcraft$previousButton.setY(controlsY + BUTTON_SIZE + BUTTON_GAP);
        vinylcraft$nextButton.setX(controlsX + BUTTON_SIZE + BUTTON_GAP);
        vinylcraft$nextButton.setY(controlsY + BUTTON_SIZE + BUTTON_GAP);
        vinylcraft$volumeSlider.setX(controlsX);
        vinylcraft$volumeSlider.setY(controlsY - 14);

        Minecraft minecraft = Minecraft.getInstance();
        boolean ready = false;
        int state = DiscmanData.STOPPED;

        if (minecraft.player != null) {
            ItemStack discman = DiscmanData.findEquipped(minecraft.player);
            ItemStack vinyl = DiscmanData.findVinyl(minecraft.player);
            ready = !discman.isEmpty() && !vinyl.isEmpty() && VinylData.hasAlbum(vinyl);
            if (!discman.isEmpty()) state = DiscmanData.getState(discman);
        }

        vinylcraft$previousButton.active = ready;
        vinylcraft$stopButton.active = ready;
        vinylcraft$playPauseButton.active = ready;
        vinylcraft$nextButton.active = ready;

        boolean playing = state == DiscmanData.PLAYING;
        vinylcraft$playPauseButton.setMessage(Component.literal(playing ? "Pausa" : "▶"));
        vinylcraft$playPauseButton.setTheme(
                playing ? DiscmanControlButton.Theme.YELLOW : DiscmanControlButton.Theme.GREEN
        );
        vinylcraft$volumeSlider.refreshFromEquippedDiscman();
    }


    @Unique
        private void vinylcraft$drawHeadphonesSlotIcon(GuiGraphicsExtractor graphics, int slotX, int slotY) {
        int dark = 0xFF5F5F5F;

        // Diadema superior
        graphics.fill(slotX + 5, slotY + 4, slotX + 12, slotY + 5, dark);
        graphics.fill(slotX + 4, slotY + 5, slotX + 5, slotY + 7, dark);
        graphics.fill(slotX + 12, slotY + 5, slotX + 13, slotY + 7, dark);

        // Bajadas laterales
        graphics.fill(slotX + 4, slotY + 7, slotX + 5, slotY + 10, dark);
        graphics.fill(slotX + 12, slotY + 7, slotX + 13, slotY + 10, dark);

        // Cojines
        graphics.fill(slotX + 3, slotY + 9, slotX + 5, slotY + 12, dark);
        graphics.fill(slotX + 12, slotY + 9, slotX + 14, slotY + 12, dark);
        }

    @Unique
        private void vinylcraft$drawDiscmanSlotIcon(
                GuiGraphicsExtractor graphics,
                int slotX,
                int slotY) {

        int dark = 0xFF5F5F5F;

        /*
        * Silueta exterior
        */
        graphics.fill(
                slotX + 4,
                slotY + 4,
                slotX + 13,
                slotY + 13,
                dark
        );

        /*
        * Vacío interior para sugerir la tapa / ventana.
        */
        graphics.fill(
                slotX + 6,
                slotY + 6,
                slotX + 11,
                slotY + 10,
                0xFFC6C6C6
        );

        /*
        * Disco interior.
        */
        graphics.fill(
                slotX + 7,
                slotY + 7,
                slotX + 10,
                slotY + 10,
                dark
        );

        /*
        * Botón pequeño.
        */
        graphics.fill(
                slotX + 10,
                slotY + 11,
                slotX + 12,
                slotY + 12,
                dark
        );
        }

    @Unique
        private void vinylcraft$drawVinylSlotIcon(
                GuiGraphicsExtractor graphics,
                int slotX,
                int slotY) {

        int dark = 0xFF5F5F5F;

        /*
        * Forma circular pixel-art.
        */
        graphics.fill(
                slotX + 7,
                slotY + 4,
                slotX + 11,
                slotY + 5,
                dark
        );

        graphics.fill(
                slotX + 5,
                slotY + 5,
                slotX + 13,
                slotY + 6,
                dark
        );

        graphics.fill(
                slotX + 4,
                slotY + 6,
                slotX + 14,
                slotY + 10,
                dark
        );

        graphics.fill(
                slotX + 5,
                slotY + 10,
                slotX + 13,
                slotY + 11,
                dark
        );

        graphics.fill(
                slotX + 7,
                slotY + 11,
                slotX + 11,
                slotY + 12,
                dark
        );

        /*
        * Agujero central.
        */
        graphics.fill(
                slotX + 8,
                slotY + 7,
                slotX + 10,
                slotY + 9,
                0xFFC6C6C6
        );
        }
}
