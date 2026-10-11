package com.radig.vinylcraft.client.render.becoya;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Modelos simples del Discman, audífonos y cable de Becoya's Module. */
public final class BecoyaAccessoryModel
        extends EntityModel<AvatarRenderState> {

    public BecoyaAccessoryModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createDiscmanLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        /*
         * Discman casi cuadrado para que la textura 2D del inventario lea
         * mejor también en el wearable. Mantiene un leve volumen.
         */
        root.addOrReplaceChild(
                "discman",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(
                                -2.20F,
                                -2.20F,
                                -0.62F,
                                4.4F,
                                4.4F,
                                1.24F
                        )
                        .texOffs(0, 18)
                        .addBox(
                                -1.80F,
                                -1.80F,
                                -0.90F,
                                3.6F,
                                3.6F,
                                0.24F
                        )
                        .texOffs(18, 18)
                        .addBox(
                                1.05F,
                                1.25F,
                                -0.94F,
                                0.56F,
                                0.56F,
                                0.24F
                        ),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 32, 32);
    }

    public static LayerDefinition createHeadphonesLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        /*
         * Diadema y copas algo más abiertas que la cabeza vanilla para que
         * también sean visibles cuando el jugador lleve casco.
         */
        root.addOrReplaceChild(
                "headphones",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(
                                -5.0F,
                                -9.15F,
                                -0.95F,
                                10.0F,
                                1.1F,
                                1.9F
                        )
                        .texOffs(0, 6)
                        .addBox(
                                -5.7F,
                                -7.9F,
                                -1.55F,
                                1.7F,
                                4.5F,
                                3.1F
                        )
                        .texOffs(10, 6)
                        .addBox(
                                4.0F,
                                -7.9F,
                                -1.55F,
                                1.7F,
                                4.5F,
                                3.1F
                        ),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 32, 32);
    }

    public static LayerDefinition createCableLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        /*
         * Cable visible sobre el costado FRONTAL del torso. Sigue al cuerpo,
         * no a la cabeza, para que al mirar a los lados nunca cruce la cara.
         * El tramo inferior se acerca al Discman de la cintura.
         */
        root.addOrReplaceChild(
                "cable",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(
                                4.15F,
                                -1.25F,
                                -2.35F,
                                0.52F,
                                11.9F,
                                0.52F
                        )
                        .texOffs(4, 0)
                        .addBox(
                                2.35F,
                                10.2F,
                                -2.35F,
                                2.30F,
                                0.52F,
                                0.52F
                        ),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 8, 32);
    }
}
