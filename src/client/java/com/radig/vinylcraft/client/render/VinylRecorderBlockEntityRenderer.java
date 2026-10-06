package com.radig.vinylcraft.client.render;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.radig.vinylcraft.block.VinylRecorderBlock;
import com.radig.vinylcraft.block.entity.VinylRecorderBlockEntity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

public class VinylRecorderBlockEntityRenderer
        implements BlockEntityRenderer<
                VinylRecorderBlockEntity,
                VinylRecorderRenderState> {

    private static final int FULL_BRIGHT = 0x00F000F0;

    public static final ModelLayerLocation TONEARM_LAYER =
            new ModelLayerLocation(
                    Identifier.fromNamespaceAndPath(
                            "vinylcraft",
                            "vinyl_recorder_tonearm"
                    ),
                    "main"
            );

    private final ItemModelResolver itemModelResolver;
    private final VinylRecorderTonearmModel tonearmModel;
    private final TextureAtlasSprite tonearmSprite;
    private final TextureAtlasSprite recLightSprite;

    public VinylRecorderBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {

        this.itemModelResolver = context.itemModelResolver();

        ModelPart tonearmRoot = context.bakeLayer(TONEARM_LAYER);
        this.tonearmModel = new VinylRecorderTonearmModel(tonearmRoot);

        Identifier blockAtlas = Identifier.fromNamespaceAndPath(
                "minecraft",
                "textures/atlas/blocks.png"
        );

        this.tonearmSprite = context.sprites().get(
                new SpriteId(
                        blockAtlas,
                        Identifier.fromNamespaceAndPath(
                                "vinylcraft",
                                "block/vinyl_recorder_tonearm"
                        )
                )
        );

        this.recLightSprite = context.sprites().get(
                new SpriteId(
                        blockAtlas,
                        Identifier.fromNamespaceAndPath(
                                "vinylcraft",
                                "block/vinyl_recorder_rec_light"
                        )
                )
        );
    }

    @Override
    public VinylRecorderRenderState createRenderState() {
        return new VinylRecorderRenderState();
    }

    @Override
    public void extractRenderState(
            VinylRecorderBlockEntity blockEntity,
            VinylRecorderRenderState state,
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

        state.recording = blockEntity.isRecording();
        state.hasVinyl = blockEntity.hasVinyl();
        state.facing = blockEntity.getBlockState()
                .getValue(VinylRecorderBlock.FACING);
        state.tonearmPosition = blockEntity.getTonearmPosition(tickProgress);

        float animatedRecordingTicks =
                blockEntity.getRecordingTicks()
                        + (state.recording ? tickProgress : 0.0F);

        state.discRotation =
                animatedRecordingTicks * 12.0F;

        /*
         * Pulso lento del piloto REC: un ciclo completo cada 2 segundos.
         * El factor nunca llega a apagarse por completo; sólo "respira"
         * mientras el disco está siendo grabado.
         */
        state.recPulse =
                0.65F
                        + 0.35F
                        * (
                            0.5F
                                    + 0.5F
                                    * (float) Math.sin(
                                            animatedRecordingTicks
                                                    * Math.PI
                                                    / 20.0D
                                    )
                        );

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
            VinylRecorderRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            CameraRenderState cameraState) {

        float blockRotation = switch (state.facing) {
            case EAST -> -90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };

        matrices.pushPose();
        matrices.translate(0.5F, 0.0F, 0.5F);
        matrices.mulPose(Axis.YP.rotationDegrees(blockRotation));
        matrices.translate(-0.5F, 0.0F, -0.5F);

        /*
         * Vinilo real: se renderiza el ItemStack, igual que en el Player.
         * Ya no usamos un quad transparente independiente.
         */
        if (state.hasVinyl) {
            matrices.pushPose();

            matrices.translate(
                    7.5F / 16.0F,
                    13.76F / 16.0F,
                    7.5F / 16.0F
            );

            matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
            matrices.mulPose(Axis.ZP.rotationDegrees(state.discRotation));

            matrices.scale(0.42F, 0.42F, 0.42F);

            state.vinyl.submit(
                    matrices,
                    queue,
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0
            );

            matrices.popPose();
        }

        /*
         * Único indicador frontal dinámico: REC.
         */
        if (state.recording && state.hasVinyl) {
            /*
             * El botón físico REC está centrado en Y=6.70.
             * Alineamos el piloto a esa misma altura y lo hacemos un poco
             * más grande para que el frente quede simétrico.
             */
            float pulseScale =
                    1.0F + (state.recPulse - 0.65F) * 0.10F;

            float lightSize =
                    (1.70F * pulseScale) / 16.0F;

            int alpha =
                    Math.max(
                            0,
                            Math.min(
                                    255,
                                    Math.round(145.0F + 110.0F * state.recPulse)
                            )
                    );

            submitFrontQuad(
                    queue,
                    matrices,
                    recLightSprite,
                    12.45F / 16.0F,
                    6.70F / 16.0F,
                    0.90F / 16.0F,
                    lightSize,
                    lightSize,
                    FULL_BRIGHT,
                    alpha
            );
        }

        /*
         * Brazo dinámico.
         */
        float pivotX = VinylRecorderTonearmModel.PIVOT_X / 16.0F;
        float pivotY = VinylRecorderTonearmModel.PIVOT_Y / 16.0F;
        float pivotZ = VinylRecorderTonearmModel.PIVOT_Z / 16.0F;

        matrices.translate(pivotX, pivotY, pivotZ);
        matrices.mulPose(
                Axis.YP.rotationDegrees(
                        26.0F * state.tonearmPosition
                )
        );
        matrices.translate(-pivotX, -pivotY, -pivotZ);

        queue.submitModelPart(
                tonearmModel.root(),
                matrices,
                RenderTypes.entityCutout(tonearmSprite.atlasLocation()),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                tonearmSprite
        );

        matrices.popPose();
    }

    private void submitFrontQuad(
            SubmitNodeCollector queue,
            PoseStack matrices,
            TextureAtlasSprite sprite,
            float centerX,
            float centerY,
            float z,
            float width,
            float height,
            int light,
            int alpha) {

        float minX = centerX - width / 2.0F;
        float maxX = centerX + width / 2.0F;
        float minY = centerY - height / 2.0F;
        float maxY = centerY + height / 2.0F;

        queue.submitCustomGeometry(
                matrices,
                RenderTypes.entityTranslucentEmissive(
                        sprite.atlasLocation()
                ),
                (pose, consumer) -> {

                    consumer.addVertex(pose, minX, maxY, z)
                            .setColor(255, 255, 255, alpha)
                            .setUv(sprite.getU0(), sprite.getV0())
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light)
                            .setNormal(0.0F, 0.0F, -1.0F);

                    consumer.addVertex(pose, maxX, maxY, z)
                            .setColor(255, 255, 255, alpha)
                            .setUv(sprite.getU1(), sprite.getV0())
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light)
                            .setNormal(0.0F, 0.0F, -1.0F);

                    consumer.addVertex(pose, maxX, minY, z)
                            .setColor(255, 255, 255, alpha)
                            .setUv(sprite.getU1(), sprite.getV1())
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light)
                            .setNormal(0.0F, 0.0F, -1.0F);

                    consumer.addVertex(pose, minX, minY, z)
                            .setColor(255, 255, 255, alpha)
                            .setUv(sprite.getU0(), sprite.getV1())
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light)
                            .setNormal(0.0F, 0.0F, -1.0F);
                }
        );
    }
}
