package com.radig.vinylcraft.client.render;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.radig.vinylcraft.block.VinylPlayerBlock;
import com.radig.vinylcraft.block.entity.VinylPlayerBlockEntity;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

public class VinylPlayerBlockEntityRenderer
        implements BlockEntityRenderer<
                VinylPlayerBlockEntity,
                VinylPlayerRenderState> {

    private final ItemModelResolver itemModelResolver;

    public VinylPlayerBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {

        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public VinylPlayerRenderState createRenderState() {
        return new VinylPlayerRenderState();
    }

    @Override
    public void extractRenderState(
            VinylPlayerBlockEntity blockEntity,
            VinylPlayerRenderState state,
            float tickProgress,
            Vec3 cameraPos,
            @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {

        BlockEntityRenderer.super.extractRenderState(
                blockEntity,
                state,
                tickProgress,
                cameraPos,
                crumblingOverlay
        );

        state.hasVinyl = blockEntity.hasVinyl();
        state.playing = blockEntity.isPlaying();

        state.vinylRotation =
        (blockEntity.getPlaybackTicks()
                + (state.playing ? tickProgress : 0.0F))
                * 10.0F;

        // Guardamos también hacia dónde está orientado
        // este reproductor en particular.
        state.facing = blockEntity.getBlockState()
                .getValue(VinylPlayerBlock.FACING);

        if (state.hasVinyl) {

            itemModelResolver.updateForTopItem(
                    state.vinyl,
                    blockEntity.getVinyl(),
                    ItemDisplayContext.FIXED,
                    blockEntity.getLevel(),
                    null,
                    0
            );

        } else {

            state.vinyl.clear();
        }
    }

    @Override
    public void submit(
            VinylPlayerRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState) {

        if (!state.hasVinyl) {
            return;
        }

        matrices.pushPose();

        double x;
        double z;

        switch (state.facing) {
            case EAST -> {
                x = 9.5078125 / 16.0;
                z = 6.4921875 / 16.0;
            }
            case SOUTH -> {
                x = 9.5078125 / 16.0;
                z = 9.5078125 / 16.0;
            }
            case WEST -> {
                x = 6.4921875 / 16.0;
                z = 9.5078125 / 16.0;
            }
            default -> { // NORTH
                x = 6.4921875 / 16.0;
                z = 6.4921875 / 16.0;
            }
        }

        matrices.translate(
                x,
                5.60 / 16.0,
                z
        );

        // El vinilo siempre queda horizontal.
        matrices.mulPose(
                com.mojang.math.Axis.XP.rotationDegrees(90.0F)
        );

        // Gira el vinilo sobre su propio centro.
        matrices.mulPose(
                com.mojang.math.Axis.ZP.rotationDegrees(state.vinylRotation)
        );

        matrices.scale(
                0.42F,
                0.42F,
                0.42F
        );

        state.vinyl.submit(
                matrices,
                queue,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );

        matrices.popPose();
    }
}