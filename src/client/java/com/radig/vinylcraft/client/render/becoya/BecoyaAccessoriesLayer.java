package com.radig.vinylcraft.client.render.becoya;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.radig.vinylcraft.VinylCraft;

import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

/** Render real de Discman + audífonos + cable sobre el avatar. */
public final class BecoyaAccessoriesLayer
        extends RenderLayer<AvatarRenderState, PlayerModel> {

    public static final ModelLayerLocation DISCMAN_LAYER =
            new ModelLayerLocation(
                    VinylCraft.id("becoya_discman_wearable"),
                    "main"
            );

    public static final ModelLayerLocation HEADPHONES_LAYER =
            new ModelLayerLocation(
                    VinylCraft.id("becoya_headphones"),
                    "main"
            );

    public static final ModelLayerLocation CABLE_LAYER =
            new ModelLayerLocation(
                    VinylCraft.id("becoya_headphone_cable"),
                    "main"
            );

    private static final Identifier DISCMAN_TEXTURE =
            VinylCraft.id("textures/entity/becoya_discman_wearable.png");

    private static final Identifier CABLE_TEXTURE =
            VinylCraft.id("textures/entity/becoya_headphone_cable.png");

    private final BecoyaAccessoryModel discmanModel;
    private final BecoyaAccessoryModel headphonesModel;
    private final BecoyaAccessoryModel cableModel;

    public BecoyaAccessoriesLayer(
            AvatarRenderer<?> renderer,
            EntityModelSet modelSet) {

        super(renderer);

        this.discmanModel =
                new BecoyaAccessoryModel(
                        modelSet.bakeLayer(DISCMAN_LAYER)
                );

        this.headphonesModel =
                new BecoyaAccessoryModel(
                        modelSet.bakeLayer(HEADPHONES_LAYER)
                );

        this.cableModel =
                new BecoyaAccessoryModel(
                        modelSet.bakeLayer(CABLE_LAYER)
                );
    }


    @Override
    public void submit(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int packedLight,
            AvatarRenderState state,
            float limbAngle,
            float limbDistance) {

        FabricRenderState fabricState = (FabricRenderState) (Object) state;
        boolean hasDiscman = fabricState.getDataOrDefault(BecoyaRenderData.HAS_DISCMAN, false);
        boolean hasHeadphones = fabricState.getDataOrDefault(BecoyaRenderData.HAS_HEADPHONES, false);

        if ((!hasDiscman && !hasHeadphones) || state.isInvisible) {
            return;
        }

        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
        PlayerModel parent = getParentModel();

        if (hasHeadphones) {
            String color = fabricState.getDataOrDefault(BecoyaRenderData.HEADPHONES_COLOR, "black");
            Identifier headphonesTexture = VinylCraft.id(
                    "textures/entity/headphones/" + color + ".png"
            );

            poseStack.pushPose();
            parent.head.translateAndRotate(poseStack);
            collector.submitModel(
                    headphonesModel,
                    state,
                    poseStack,
                    headphonesTexture,
                    packedLight,
                    overlay,
                    state.outlineColor,
                    null
            );
            poseStack.popPose();
        }

        /* Cable sólo existe cuando hay Discman + audífonos. Va por el costado/espalda. */
        if (hasDiscman && hasHeadphones) {
            poseStack.pushPose();
            parent.body.translateAndRotate(poseStack);
            collector.submitModel(
                    cableModel,
                    state,
                    poseStack,
                    CABLE_TEXTURE,
                    packedLight,
                    overlay,
                    state.outlineColor,
                    null
            );
            poseStack.popPose();
        }

        if (hasDiscman) {
            poseStack.pushPose();
            parent.body.translateAndRotate(poseStack);
            /*
             * Va al frente de la cintura, en el costado contrario de la
             * mano principal. Lo separamos apenas de la armadura para que
             * se lea como un objeto colgado y no como parte de la skin.
             */
            poseStack.translate(0.12D, 0.58D, -0.28D);
            poseStack.mulPose(Axis.YP.rotationDegrees(-1.5F));
            collector.submitModel(
                    discmanModel,
                    state,
                    poseStack,
                    DISCMAN_TEXTURE,
                    packedLight,
                    overlay,
                    state.outlineColor,
                    null
            );
            poseStack.popPose();
        }
    }
}
