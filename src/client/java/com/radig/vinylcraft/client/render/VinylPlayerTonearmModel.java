package com.radig.vinylcraft.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class VinylPlayerTonearmModel {

    /*
     * Pivote REAL del brazo en el modelo de bloque:
     *
     * X = 12.625
     * Y = 6.0
     * Z = 11.3
     */
    public static LayerDefinition createLayer() {
        return LayerDefinition.create(
                createMesh(),
                32,
                32
        );
    }

    public static MeshDefinition createMesh() {

        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        /*
         * Todo el brazo gira alrededor de este punto.
         */
        PartDefinition tonearmRoot =
                root.addOrReplaceChild(
                        "tonearm_root",
                        CubeListBuilder.create(),
                        PartPose.offset(
                                12.625F,
                                6.0F,
                                11.3F
                        )
                );

        /*
         * =====================================================
         * BRAZO
         * =====================================================
         *
         * Original:
         * from [12.25, 5.7, 7]
         * to   [13, 6.3, 11.3]
         *
         * Coordenadas relativas al pivote:
         *
         * X: 12.25 - 12.625 = -0.375
         * Y:  5.70 -  6.000 = -0.300
         * Z:  7.00 - 11.300 = -4.300
         */
        tonearmRoot.addOrReplaceChild(
                "arm",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(
                                -0.375F,
                                -0.3F,
                                -6.75F,
                                0.75F,
                                0.6F,
                                6.75F
                        ),
                PartPose.rotation(
                        0.0F,
                        (float) Math.toRadians(-22.5F),
                        0.0F
                )
        );

        /*
         * =====================================================
         * CABEZAL
         * =====================================================
         *
         * Original:
         * from [11.8, 5.55, 6.45]
         * to   [13.15, 6.15, 7.35]
         */
        tonearmRoot.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 8)
                        .addBox(
                                -0.825F,
                                -0.45F,
                                -7.45F,
                                1.35F,
                                0.6F,
                                0.9F
                        ),
                PartPose.rotation(
                        0.0F,
                        (float) Math.toRadians(-22.5F),
                        0.0F
                )
        );

        /*
         * =====================================================
         * AGUJA
         * =====================================================
         *
         * Original:
         * from [12.28, 5.1, 6.18]
         * to   [12.52, 5.5, 6.55]
         */
        tonearmRoot.addOrReplaceChild(
                "needle",
                CubeListBuilder.create()
                        .texOffs(0, 12)
                        .addBox(
                                -0.345F,
                                -0.9F,
                                -7.75F,
                                0.24F,
                                0.4F,
                                0.37F
                        ),
                PartPose.rotation(
                        0.0F,
                        (float) Math.toRadians(-22.5F),
                        0.0F
                )
        );

        return mesh;
    }

    private final ModelPart root;
    private final ModelPart tonearmRoot;

    public VinylPlayerTonearmModel(ModelPart root) {
        this.root = root;
        this.tonearmRoot =
                root.getChild("tonearm_root");
    }

    public ModelPart root() {
        return root;
    }

    public ModelPart tonearmRoot() {
        return tonearmRoot;
    }
}