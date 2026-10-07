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

    /*
     * Fila superior de pilotos:
     * VERDE | AMARILLO | ROJO
     */
    private static final float GREEN_X = 3.45F / 16.0F;
    private static final float YELLOW_X = 8.00F / 16.0F;
    private static final float RED_X = 12.55F / 16.0F;
    private static final float LIGHT_Y = 9.55F / 16.0F;
    private static final float LIGHT_Z = 0.18F / 16.0F;
    private static final float LIGHT_SIZE = 0.92F / 16.0F;

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
    private final TextureAtlasSprite indicatorSprite;

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

        /*
         * Sprite vanilla seguro. Los colores reales se aplican por vértice.
         * Así los pilotos no dependen de texturas dinámicas propias dentro
         * del atlas y evitamos el checker rosa/negro.
         */
        this.indicatorSprite = context.sprites().get(
                new SpriteId(
                        blockAtlas,
                        Identifier.fromNamespaceAndPath(
                                "minecraft",
                                "block/white_concrete"
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

        state.greenLight = blockEntity.isCompletedLightOn();
        state.yellowLight = blockEntity.shouldShowYellowLight();
        state.yellowHardBlink = blockEntity.shouldHardBlinkYellow();
        state.redProcessLight = blockEntity.shouldShowRedProcessLight();
        state.redErrorLight = blockEntity.isWrongCloneTargetInserted();

        long worldTicks =
                blockEntity.getLevel() == null
                        ? 0L
                        : blockEntity.getLevel().getGameTime();

        state.lightAnimationTicks = worldTicks + tickProgress;

        float mechanicalTicks =
                blockEntity.getMechanicalProcessTicks()
                        + (
                            blockEntity.isMechanicalProcessActive()
                                    ? tickProgress
                                    : 0.0F
                        );

        state.discRotation = mechanicalTicks * 12.0F;

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

        submitIndicatorLights(state, matrices, queue);

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

    private void submitIndicatorLights(
            VinylRecorderRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue) {

        /*
         * VERDE: proceso terminado. Fijo hasta retirar el disco.
         */
        if (state.greenLight) {
            submitFrontQuad(
                    queue,
                    matrices,
                    indicatorSprite,
                    GREEN_X,
                    LIGHT_Y,
                    LIGHT_Z,
                    LIGHT_SIZE,
                    LIGHT_SIZE,
                    FULL_BRIGHT,
                    255,
                    45,
                    255,
                    70,
                    true
            );
        }

        /*
         * AMARILLO:
         * - leyendo original -> pulso suave.
         * - original listo / esperando virgen -> ON/OFF muy evidente.
         */
        if (state.yellowLight) {
            int alpha;

            if (state.yellowHardBlink) {
                boolean on =
                        ((int) (state.lightAnimationTicks / 10.0F)) % 2 == 0;
                alpha = on ? 255 : 0;
            } else {
                float pulse =
                        0.5F
                                + 0.5F
                                * (float) Math.sin(
                                        state.lightAnimationTicks
                                                * Math.PI
                                                / 10.0D
                                );
                alpha = Math.round(95.0F + 160.0F * pulse);
            }

            if (alpha > 0) {
                submitFrontQuad(
                        queue,
                        matrices,
                        indicatorSprite,
                        YELLOW_X,
                        LIGHT_Y,
                        LIGHT_Z,
                        LIGHT_SIZE,
                        LIGHT_SIZE,
                        FULL_BRIGHT,
                        alpha,
                        255,
                        210,
                        35,
                        true
                );
            }
        }

        /*
         * ROJO:
         * - grabación normal / escritura del clon -> pulso.
         * - disco grabado incorrecto como destino -> rojo fijo.
         */
        if (state.redProcessLight || state.redErrorLight) {
            int alpha = 255;

            if (!state.redErrorLight) {
                float pulse =
                        0.5F
                                + 0.5F
                                * (float) Math.sin(
                                        state.lightAnimationTicks
                                                * Math.PI
                                                / 10.0D
                                );
                alpha = Math.round(110.0F + 145.0F * pulse);
            }

            submitFrontQuad(
                    queue,
                    matrices,
                    indicatorSprite,
                    RED_X,
                    LIGHT_Y,
                    LIGHT_Z,
                    LIGHT_SIZE,
                    LIGHT_SIZE,
                    FULL_BRIGHT,
                    alpha,
                    255,
                    30,
                    30,
                    true
            );
        }
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
            int alpha,
            int red,
            int green,
            int blue,
            boolean emissive) {

        float minX = centerX - width / 2.0F;
        float maxX = centerX + width / 2.0F;
        float minY = centerY - height / 2.0F;
        float maxY = centerY + height / 2.0F;

        queue.submitCustomGeometry(
                matrices,
                emissive
                        ? RenderTypes.entityTranslucentEmissive(
                                sprite.atlasLocation()
                        )
                        : RenderTypes.entityTranslucent(
                                sprite.atlasLocation()
                        ),
                (pose, consumer) -> {

                    consumer.addVertex(pose, minX, maxY, z)
                            .setColor(red, green, blue, alpha)
                            .setUv(sprite.getU0(), sprite.getV0())
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light)
                            .setNormal(0.0F, 0.0F, -1.0F);

                    consumer.addVertex(pose, maxX, maxY, z)
                            .setColor(red, green, blue, alpha)
                            .setUv(sprite.getU1(), sprite.getV0())
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light)
                            .setNormal(0.0F, 0.0F, -1.0F);

                    consumer.addVertex(pose, maxX, minY, z)
                            .setColor(red, green, blue, alpha)
                            .setUv(sprite.getU1(), sprite.getV1())
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light)
                            .setNormal(0.0F, 0.0F, -1.0F);

                    consumer.addVertex(pose, minX, minY, z)
                            .setColor(red, green, blue, alpha)
                            .setUv(sprite.getU0(), sprite.getV1())
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light)
                            .setNormal(0.0F, 0.0F, -1.0F);
                }
        );
    }
}
