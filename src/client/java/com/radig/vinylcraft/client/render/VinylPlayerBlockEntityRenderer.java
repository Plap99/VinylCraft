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
    private final TextureAtlasSprite playButtonSprite;
    private final TextureAtlasSprite pauseButtonSprite;
    private final TextureAtlasSprite stopButtonSprite;
    private final TextureAtlasSprite playIconSprite;
    private final TextureAtlasSprite pauseIconSprite;
    private final TextureAtlasSprite stopIconSprite;

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

        this.playButtonSprite = context.sprites().get(
                new SpriteId(blockAtlas, Identifier.fromNamespaceAndPath(
                        "vinylcraft", "block/play_button"))
        );
        this.pauseButtonSprite = context.sprites().get(
                new SpriteId(blockAtlas, Identifier.fromNamespaceAndPath(
                        "vinylcraft", "block/pause_button"))
        );
        this.stopButtonSprite = context.sprites().get(
                new SpriteId(blockAtlas, Identifier.fromNamespaceAndPath(
                        "vinylcraft", "block/stop_button"))
        );

        /*
         * Iconos independientes de los botones.
         *
         * Se dibujan como una pequeña superficie cuadrada y transparente
         * sobre cada botón. De este modo el símbolo conserva su proporción
         * y NO depende del UV automático del diminuto ModelPart.
         */
        this.playIconSprite = context.sprites().get(
                new SpriteId(blockAtlas, Identifier.fromNamespaceAndPath(
                        "vinylcraft", "block/play_icon"))
        );

        this.pauseIconSprite = context.sprites().get(
                new SpriteId(blockAtlas, Identifier.fromNamespaceAndPath(
                        "vinylcraft", "block/pause_icon"))
        );

        this.stopIconSprite = context.sprites().get(
                new SpriteId(blockAtlas, Identifier.fromNamespaceAndPath(
                        "vinylcraft", "block/stop_icon"))
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
                buttonModel.playPause(),
                matrices,
                RenderTypes.entityCutout(actionSprite.atlasLocation()),
                FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                actionSprite
        );

        queue.submitModelPart(
                buttonModel.stop(),
                matrices,
                RenderTypes.entityCutout(stopButtonSprite.atlasLocation()),
                FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                stopButtonSprite
        );

        /*
         * Iconos superiores.
         *
         * Cada símbolo usa una textura transparente 16x16 sobre una
         * superficie CUADRADA independiente. Así ▶, Ⅱ y ■ no se deforman
         * por la proporción 2.5:1 del botón físico.
         *
         * En el PLAY invertimos U porque, visto desde el frente del modelo
         * NORTH, el +X local aparece a la izquierda del jugador. Con el UV
         * espejado, ▶ queda visualmente apuntando hacia la derecha.
         */
        submitButtonIcon(
                queue,
                matrices,
                state.playing ? pauseIconSprite : playIconSprite,
                3.625F,
                1.10F,
                !state.playing
        );

        submitButtonIcon(
                queue,
                matrices,
                stopIconSprite,
                5.625F,
                1.10F,
                false
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
     * ICONOS DE LOS BOTONES
     * =====================================================
     *
     * El botón mide 1.25 x 0.50 unidades de modelo y por eso una
     * textura cuadrada aplicada directamente al cubo se deforma.
     *
     * En su lugar dibujamos un pequeño plano CUADRADO sobre el centro
     * del botón y le asignamos el sprite completo 0..1.
     *
     * El fondo del PNG es transparente: solamente aparece el símbolo.
     */
    private void submitButtonIcon(
            SubmitNodeCollector queue,
            PoseStack matrices,
            TextureAtlasSprite sprite,
            float centerXModel,
            float centerZModel,
            boolean mirrorU) {

        /*
         * Tamaño físico del símbolo en unidades del modelo 0..16.
         *
         * 0.38 cabe dentro de los 0.50 de profundidad del botón,
         * pero sigue siendo suficientemente grande para verse.
         */
        final float halfSize =
                0.19F / 16.0F;

        final float centerX =
                centerXModel / 16.0F;

        final float centerZ =
                centerZModel / 16.0F;

        /*
         * El botón termina en Y = 5.45.
         *
         * Dejamos una pequeña separación real para evitar z-fighting,
         * sin hacer que el icono parezca flotar.
         */
        final float y =
                5.52F / 16.0F;

        final float minX = centerX - halfSize;
        final float maxX = centerX + halfSize;
        final float minZ = centerZ - halfSize;
        final float maxZ = centerZ + halfSize;

        final float uLeft =
                mirrorU
                        ? sprite.getU1()
                        : sprite.getU0();

        final float uRight =
                mirrorU
                        ? sprite.getU0()
                        : sprite.getU1();

        final float vTop = sprite.getV0();
        final float vBottom = sprite.getV1();

        queue.submitCustomGeometry(
                matrices,
                RenderTypes.entityTranslucentEmissive(
                        sprite.atlasLocation()
                ),
                (pose, consumer) -> {

                    consumer.addVertex(pose, minX, y, minZ)
                            .setColor(255, 255, 255, 255)
                            .setUv(uLeft, vTop)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(FULL_BRIGHT)
                            .setNormal(0.0F, 1.0F, 0.0F);

                    consumer.addVertex(pose, maxX, y, minZ)
                            .setColor(255, 255, 255, 255)
                            .setUv(uRight, vTop)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(FULL_BRIGHT)
                            .setNormal(0.0F, 1.0F, 0.0F);

                    consumer.addVertex(pose, maxX, y, maxZ)
                            .setColor(255, 255, 255, 255)
                            .setUv(uRight, vBottom)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(FULL_BRIGHT)
                            .setNormal(0.0F, 1.0F, 0.0F);

                    consumer.addVertex(pose, minX, y, maxZ)
                            .setColor(255, 255, 255, 255)
                            .setUv(uLeft, vBottom)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(FULL_BRIGHT)
                            .setNormal(0.0F, 1.0F, 0.0F);
                }
        );
    }

}
