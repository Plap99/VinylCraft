package com.radig.vinylcraft.client.render;

import java.nio.file.Path;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.radig.vinylcraft.block.AlbumFrameBlock;
import com.radig.vinylcraft.block.entity.AlbumFrameBlockEntity;
import com.radig.vinylcraft.client.library.AlbumCoverResolver;
import com.radig.vinylcraft.client.library.AlbumCoverTextureManager;
import com.radig.vinylcraft.music.AlbumData;
import com.radig.vinylcraft.music.ModAlbums;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Render de la portada mural. Sólo la pieza origen dibuja el quad completo. */
public final class AlbumFrameBlockEntityRenderer
        implements BlockEntityRenderer<AlbumFrameBlockEntity, AlbumFrameRenderState> {

    public AlbumFrameBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AlbumFrameRenderState createRenderState() {
        return new AlbumFrameRenderState();
    }

    @Override
    public void extractRenderState(
            AlbumFrameBlockEntity blockEntity,
            AlbumFrameRenderState state,
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

        state.origin = blockEntity.isOrigin();
        state.wallSize = blockEntity.getWallSize();
        state.facing = blockEntity.getBlockState().getValue(AlbumFrameBlock.FACING);
        state.coverTexture = null;

        if (!state.origin) {
            return;
        }

        AlbumData album = ModAlbums.get(blockEntity.getAlbumId());

        if (album == null || album.coverFile() == null || album.coverFile().isBlank()) {
            return;
        }

        try {
            var cover = new AlbumCoverResolver.CoverResult(
                    AlbumCoverResolver.SourceType.FILE,
                    Path.of(album.coverFile()),
                    null
            );

            state.coverTexture = AlbumCoverTextureManager.getTexture(cover);
        } catch (Exception ignored) {
            state.coverTexture = null;
        }
    }

    @Override
    public void submit(
            AlbumFrameRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState) {

        Identifier texture = state.coverTexture;

        if (!state.origin || texture == null) {
            return;
        }

        float size = Math.max(1, Math.min(10, state.wallSize));
        Direction facing = state.facing;

        queue.submitCustomGeometry(
                matrices,
                RenderTypes.entityCutout(texture),
                (pose, consumer) -> {

                    switch (facing) {
                        case SOUTH -> {
                            float z = 0.012F;
                            vertex(consumer, pose, 0.0F, size, z, 0.0F, 0.0F, 0, 0, 1, state.lightCoords);
                            vertex(consumer, pose, size, size, z, 1.0F, 0.0F, 0, 0, 1, state.lightCoords);
                            vertex(consumer, pose, size, 0.0F, z, 1.0F, 1.0F, 0, 0, 1, state.lightCoords);
                            vertex(consumer, pose, 0.0F, 0.0F, z, 0.0F, 1.0F, 0, 0, 1, state.lightCoords);
                        }
                        case NORTH -> {
                            float z = 0.988F;
                            vertex(consumer, pose, 1.0F, size, z, 0.0F, 0.0F, 0, 0, -1, state.lightCoords);
                            vertex(consumer, pose, 1.0F - size, size, z, 1.0F, 0.0F, 0, 0, -1, state.lightCoords);
                            vertex(consumer, pose, 1.0F - size, 0.0F, z, 1.0F, 1.0F, 0, 0, -1, state.lightCoords);
                            vertex(consumer, pose, 1.0F, 0.0F, z, 0.0F, 1.0F, 0, 0, -1, state.lightCoords);
                        }
                        case EAST -> {
                            float x = 0.012F;
                            vertex(consumer, pose, x, size, 1.0F, 0.0F, 0.0F, 1, 0, 0, state.lightCoords);
                            vertex(consumer, pose, x, size, 1.0F - size, 1.0F, 0.0F, 1, 0, 0, state.lightCoords);
                            vertex(consumer, pose, x, 0.0F, 1.0F - size, 1.0F, 1.0F, 1, 0, 0, state.lightCoords);
                            vertex(consumer, pose, x, 0.0F, 1.0F, 0.0F, 1.0F, 1, 0, 0, state.lightCoords);
                        }
                        case WEST -> {
                            float x = 0.988F;
                            vertex(consumer, pose, x, size, 0.0F, 0.0F, 0.0F, -1, 0, 0, state.lightCoords);
                            vertex(consumer, pose, x, size, size, 1.0F, 0.0F, -1, 0, 0, state.lightCoords);
                            vertex(consumer, pose, x, 0.0F, size, 1.0F, 1.0F, -1, 0, 0, state.lightCoords);
                            vertex(consumer, pose, x, 0.0F, 0.0F, 0.0F, 1.0F, -1, 0, 0, state.lightCoords);
                        }
                        default -> {
                        }
                    }
                }
        );
    }

    private static void vertex(
            com.mojang.blaze3d.vertex.VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            float nx,
            float ny,
            float nz,
            int light) {

        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 160;
    }
}
