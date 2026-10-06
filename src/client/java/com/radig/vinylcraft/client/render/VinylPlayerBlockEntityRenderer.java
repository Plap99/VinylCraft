package com.radig.vinylcraft.client.render;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.radig.vinylcraft.block.VinylPlayerBlock;
import com.radig.vinylcraft.block.entity.VinylPlayerBlockEntity;

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

public class VinylPlayerBlockEntityRenderer
        implements BlockEntityRenderer<
                VinylPlayerBlockEntity,
                VinylPlayerRenderState> {

    private final ItemModelResolver itemModelResolver;
    private final VinylPlayerTonearmModel tonearmModel;
    private final TextureAtlasSprite tonearmSprite;
    private final VinylPlayerButtonModel buttonModel;
    private final TextureAtlasSprite previousButtonSprite;
    private final TextureAtlasSprite stopButtonSprite;
    private final TextureAtlasSprite playButtonSprite;
    private final TextureAtlasSprite pauseButtonSprite;
    private final TextureAtlasSprite nextButtonSprite;
    private final TextureAtlasSprite previousIconSprite;
    private final TextureAtlasSprite stopIconSprite;
    private final TextureAtlasSprite playIconSprite;
    private final TextureAtlasSprite pauseIconSprite;
    private final TextureAtlasSprite nextIconSprite;

    private static final int FULL_BRIGHT = 0x00F000F0;

    /*
     * Capa del modelo dinámico del brazo.
     */
    public static final ModelLayerLocation TONEARM_LAYER =
            new ModelLayerLocation(
                    Identifier.fromNamespaceAndPath(
                            "vinylcraft",
                            "vinyl_player_tonearm"
                    ),
                    "main"
            );

    public static final ModelLayerLocation BUTTON_LAYER =
            new ModelLayerLocation(
                    Identifier.fromNamespaceAndPath(
                            "vinylcraft",
                            "vinyl_player_buttons"
                    ),
                    "main"
            );

    public VinylPlayerBlockEntityRenderer(
            BlockEntityRendererProvider.Context context) {

        this.itemModelResolver = context.itemModelResolver();

        /*
         * Obtenemos el modelo que registramos en
         * VinylCraftClient.
         */
        ModelPart tonearmRoot =
                context.bakeLayer(TONEARM_LAYER);

        this.tonearmModel =
                new VinylPlayerTonearmModel(tonearmRoot);

        /*
         * Sprite de la textura del brazo.
         */
        this.tonearmSprite = context.sprites().get(
                new SpriteId(
                        Identifier.fromNamespaceAndPath(
                                "minecraft",
                                "textures/atlas/blocks.png"
                        ),
                        Identifier.fromNamespaceAndPath(
                                "vinylcraft",
                                "block/vinyl_player_tonearm"
                        )
                )
        );

        this.buttonModel =
                new VinylPlayerButtonModel(
                        context.bakeLayer(BUTTON_LAYER)
                );

        Identifier blockAtlas = Identifier.fromNamespaceAndPath(
                "minecraft",
                "textures/atlas/blocks.png"
        );

        this.previousButtonSprite = context.sprites().get(
                new SpriteId(blockAtlas, Identifier.fromNamespaceAndPath(
                        "vinylcraft", "block/previous_button"))
        );
        this.stopButtonSprite = context.sprites().get(
                new SpriteId(blockAtlas, Identifier.fromNamespaceAndPath(
                        "vinylcraft", "block/stop_button"))
        );
        this.playButtonSprite = context.sprites().get(
                new SpriteId(blockAtlas, Identifier.fromNamespaceAndPath(
                        "vinylcraft", "block/play_button"))
        );
        this.pauseButtonSprite = context.sprites().get(
                new SpriteId(blockAtlas, Identifier.fromNamespaceAndPath(
                        "vinylcraft", "block/pause_button"))
        );
        this.nextButtonSprite = context.sprites().get(
                new SpriteId(blockAtlas, Identifier.fromNamespaceAndPath(
                        "vinylcraft", "block/next_button"))
        );

        /*
         * Iconos independientes de los botones.
         *
         * Se dibujan como una pequeña superficie cuadrada y transparente
         * sobre cada botón. De este modo el símbolo conserva su proporción
         * y NO depende del UV automático del diminuto ModelPart.
         */
        this.previousIconSprite = context.sprites().get(
                new SpriteId(blockAtlas, Identifier.fromNamespaceAndPath(
                        "vinylcraft", "block/previous_icon"))
        );

        this.stopIconSprite = context.sprites().get(
                new SpriteId(blockAtlas, Identifier.fromNamespaceAndPath(
                        "vinylcraft", "block/stop_icon"))
        );

        this.playIconSprite = context.sprites().get(
                new SpriteId(blockAtlas, Identifier.fromNamespaceAndPath(
                        "vinylcraft", "block/play_icon"))
        );

        this.pauseIconSprite = context.sprites().get(
                new SpriteId(blockAtlas, Identifier.fromNamespaceAndPath(
                        "vinylcraft", "block/pause_icon"))
        );

        this.nextIconSprite = context.sprites().get(
                new SpriteId(blockAtlas, Identifier.fromNamespaceAndPath(
                        "vinylcraft", "block/next_icon"))
        );
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
        state.paused = blockEntity.isPaused();

        /*
        * Animación interpolada del brazo.
        *
        * Ahora toda la lógica vive en el BlockEntity.
        * El renderer únicamente representa el resultado.
        */
        state.tonearmPosition =
                blockEntity.getTonearmPosition(
                        tickProgress
                );

        state.tonearmLift =
                blockEntity.getTonearmLift(
                        tickProgress
                );

        /*
        * Rotación del vinilo.
        */
        state.vinylRotation =
                (
                    blockEntity.getPlaybackTicks()
                    + (
                        state.playing
                            ? tickProgress
                            : 0.0F
                    )
                )
                * 10.0F;

        /*
         * Orientación del reproductor.
         */
        state.facing = blockEntity.getBlockState()
                .getValue(VinylPlayerBlock.FACING);

        /*
         * Preparamos el modelo del vinilo solamente
         * cuando realmente hay uno colocado.
         */
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

    /*
    * =====================================================
    * BRAZO DEL TOCADISCOS
    * =====================================================
    */

    matrices.pushPose();

    /*
    * Giramos el modelo completo alrededor del centro
    * del bloque.
    *
    * IMPORTANTE:
    *
    * EAST  = -90
    * WEST  = +90
    *
    * Esto coincide con las coordenadas que ya usamos
    * correctamente para el vinilo.
    */
    float blockRotation = switch (state.facing) {

        case EAST ->
                -90.0F;

        case SOUTH ->
                180.0F;

        case WEST ->
                90.0F;

        default ->
                0.0F; // NORTH
    };

    matrices.translate(
            0.5F,
            0.0F,
            0.5F
    );

    matrices.mulPose(
            Axis.YP.rotationDegrees(blockRotation)
    );

    matrices.translate(
            -0.5F,
            0.0F,
            -0.5F
    );

    /*
    * NO invertimos Y.
    *
    * Las coordenadas del ModelPart fueron tomadas
    * directamente del modelo de bloque y ya están
    * dentro del rango 0..16.
    */

    /*
    * Levantar aguja.
    *
    * Ahora que Y ya no está invertida,
    * positivo vuelve a significar "arriba".
    */
    matrices.translate(
            0.0F,
            state.tonearmLift / 16.0F,
            0.0F
    );

    /*
        * Giro propio del brazo alrededor de SU pivote.
        *
        * IMPORTANTE:
        * No modificamos tonearmModel.tonearmRoot().yRot.
        *
        * El modelo del renderer es compartido por todos los
        * reproductores visibles, así que modificar el ModelPart
        * provocaba que varias agujas usaran el mismo ángulo.
        *
        * En su lugar giramos el PoseStack de ESTE reproductor.
        */
        float tonearmAngle =
                28.0F * state.tonearmPosition;

        /*
        * Pivote real del brazo convertido de unidades
        * de modelo (0..16) a unidades de bloque (0..1).
        */
        float pivotX = 12.625F / 16.0F;
        float pivotY = 6.0F / 16.0F;
        float pivotZ = 11.3F / 16.0F;

        /*
        * Vamos al pivote...
        */
        matrices.translate(
                pivotX,
                pivotY,
                pivotZ
        );

        /*
        * ...giramos únicamente este render...
        */
        matrices.mulPose(
                Axis.YP.rotationDegrees(
                        tonearmAngle
                )
        );

        /*
        * ...y regresamos al origen.
        */
        matrices.translate(
                -pivotX,
                -pivotY,
                -pivotZ
        );

    queue.submitModelPart(
            tonearmModel.root(),
            matrices,
            RenderTypes.entityCutout(
                    tonearmSprite.atlasLocation()
            ),
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            tonearmSprite
    );

    matrices.popPose();

        /*
         * =====================================================
         * BOTONES ILUMINADOS
         * =====================================================
         * Verde ▶ cuando está listo/pausado.
         * Amarillo Ⅱ mientras reproduce.
         * Rojo ■ para STOP.
         * FULL_BRIGHT hace que sigan visibles en la oscuridad
         * sin convertirlos en una fuente de luz del mundo.
         */
        matrices.pushPose();

        matrices.translate(0.5F, 0.0F, 0.5F);
        matrices.mulPose(Axis.YP.rotationDegrees(blockRotation));
        matrices.translate(-0.5F, 0.0F, -0.5F);

        TextureAtlasSprite actionSprite =
                state.playing
                        ? pauseButtonSprite
                        : playButtonSprite;

        queue.submitModelPart(
                buttonModel.previous(),
                matrices,
                RenderTypes.entityCutout(previousButtonSprite.atlasLocation()),
                FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                previousButtonSprite
        );

        queue.submitModelPart(
                buttonModel.stop(),
                matrices,
                RenderTypes.entityCutout(stopButtonSprite.atlasLocation()),
                FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                stopButtonSprite
        );

        queue.submitModelPart(
                buttonModel.playPause(),
                matrices,
                RenderTypes.entityCutout(actionSprite.atlasLocation()),
                FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                actionSprite
        );

        queue.submitModelPart(
                buttonModel.next(),
                matrices,
                RenderTypes.entityCutout(nextButtonSprite.atlasLocation()),
                FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                nextButtonSprite
        );

        submitFrontButtonIcon(
                queue,
                matrices,
                previousIconSprite,
                VinylPlayerButtonModel.PREVIOUS_X
                        + VinylPlayerButtonModel.WIDTH / 2.0F,
                VinylPlayerButtonModel.ICON_Y,
                VinylPlayerButtonModel.ICON_Z
        );

        submitFrontButtonIcon(
                queue,
                matrices,
                stopIconSprite,
                VinylPlayerButtonModel.STOP_X
                        + VinylPlayerButtonModel.WIDTH / 2.0F,
                VinylPlayerButtonModel.ICON_Y,
                VinylPlayerButtonModel.ICON_Z
        );

        submitFrontButtonIcon(
                queue,
                matrices,
                state.playing ? pauseIconSprite : playIconSprite,
                VinylPlayerButtonModel.PLAY_X
                        + VinylPlayerButtonModel.WIDTH / 2.0F,
                VinylPlayerButtonModel.ICON_Y,
                VinylPlayerButtonModel.ICON_Z
        );

        submitFrontButtonIcon(
                queue,
                matrices,
                nextIconSprite,
                VinylPlayerButtonModel.NEXT_X
                        + VinylPlayerButtonModel.WIDTH / 2.0F,
                VinylPlayerButtonModel.ICON_Y,
                VinylPlayerButtonModel.ICON_Z
        );

        matrices.popPose();


        /*
         * =====================================================
         * VINILO
         * =====================================================
         */

        if (!state.hasVinyl) {
            return;
        }

        matrices.pushPose();

        double x;
        double z;

        /*
         * Centro del plato según la orientación
         * del bloque.
         */
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

        /*
         * El vinilo queda horizontal.
         */
        matrices.mulPose(
                Axis.XP.rotationDegrees(90.0F)
        );

        /*
         * Rotación durante la reproducción.
         */
        matrices.mulPose(
                Axis.ZP.rotationDegrees(
                        state.vinylRotation
                )
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
    /*
     * =====================================================
     * ICONOS FRONTALES DE LOS BOTONES
     * =====================================================
     *
     * Los controles ahora están en el frente del tocadiscos.
     * Dibujamos el icono como un plano vertical delante del botón
     * para conservar su proporción y evitar que quede recortado.
     */
    private void submitFrontButtonIcon(
            SubmitNodeCollector queue,
            PoseStack matrices,
            TextureAtlasSprite sprite,
            float centerXModel,
            float centerYModel,
            float zModel) {

        final float halfSize =
                0.40F / 16.0F;

        final float centerX =
                centerXModel / 16.0F;

        final float centerY =
                centerYModel / 16.0F;

        final float z =
                zModel / 16.0F;

        final float minX = centerX - halfSize;
        final float maxX = centerX + halfSize;
        final float minY = centerY - halfSize;
        final float maxY = centerY + halfSize;

        /*
         * La cara frontal NORTH se ve con el eje X local invertido desde
         * el jugador. Invertimos U para que ▶, ⏮ y ⏭ no aparezcan
         * espejados visualmente.
         */
        /*
         * Medio píxel hacia dentro evita que el filtrado/mipmapping del
         * atlas tome píxeles de sprites vecinos en los bordes transparentes.
         * Junto con la capa 16x16 del modelo elimina las dos fuentes más
         * probables del pequeño cuadro fantasma observado en los botones.
         */
        final float uInset =
                (sprite.getU1() - sprite.getU0()) / 32.0F;
        final float vInset =
                (sprite.getV1() - sprite.getV0()) / 32.0F;

        final float uLeft = sprite.getU1() - uInset;
        final float uRight = sprite.getU0() + uInset;
        final float vTop = sprite.getV0() + vInset;
        final float vBottom = sprite.getV1() - vInset;

        queue.submitCustomGeometry(
                matrices,
                RenderTypes.entityCutout(
                        sprite.atlasLocation()
                ),
                (pose, consumer) -> {

                    consumer.addVertex(pose, minX, maxY, z)
                            .setColor(255, 255, 255, 255)
                            .setUv(uLeft, vTop)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(FULL_BRIGHT)
                            .setNormal(0.0F, 0.0F, -1.0F);

                    consumer.addVertex(pose, maxX, maxY, z)
                            .setColor(255, 255, 255, 255)
                            .setUv(uRight, vTop)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(FULL_BRIGHT)
                            .setNormal(0.0F, 0.0F, -1.0F);

                    consumer.addVertex(pose, maxX, minY, z)
                            .setColor(255, 255, 255, 255)
                            .setUv(uRight, vBottom)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(FULL_BRIGHT)
                            .setNormal(0.0F, 0.0F, -1.0F);

                    consumer.addVertex(pose, minX, minY, z)
                            .setColor(255, 255, 255, 255)
                            .setUv(uLeft, vBottom)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(FULL_BRIGHT)
                            .setNormal(0.0F, 0.0F, -1.0F);
                }
        );
    }

}
