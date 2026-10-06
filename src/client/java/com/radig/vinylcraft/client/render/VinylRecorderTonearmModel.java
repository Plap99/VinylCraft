package com.radig.vinylcraft.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class VinylRecorderTonearmModel {

    /*
     * Pivote del brazo sobre el pedestal compacto del grabador.
     */
    public static final float PIVOT_X = 13.0F;
    public static final float PIVOT_Y = 14.20F;
    public static final float PIVOT_Z = 10.70F;

    public static LayerDefinition createLayer() {
        return LayerDefinition.create(createMesh(), 32, 32);
    }

    public static MeshDefinition createMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition tonearmRoot =
                root.addOrReplaceChild(
                        "tonearm_root",
                        CubeListBuilder.create(),
                        PartPose.offset(PIVOT_X, PIVOT_Y, PIVOT_Z)
                );

        tonearmRoot.addOrReplaceChild(
                "arm",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(
                                -0.30F,
                                -0.28F,
                                -6.35F,
                                0.60F,
                                0.56F,
                                6.35F
                        ),
                PartPose.rotation(
                        0.0F,
                        (float) Math.toRadians(-18.0F),
                        0.0F
                )
        );

        tonearmRoot.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 8)
                        .addBox(
                                -0.75F,
                                -0.30F,
                                -7.10F,
                                1.25F,
                                0.62F,
                                1.05F
                        ),
                PartPose.rotation(
                        0.0F,
                        (float) Math.toRadians(-18.0F),
                        0.0F
                )
        );

        tonearmRoot.addOrReplaceChild(
                "needle",
                CubeListBuilder.create()
                        .texOffs(0, 12)
                        .addBox(
                                -0.15F,
                                -0.34F,
                                -7.30F,
                                0.16F,
                                0.30F,
                                0.34F
                        ),
                PartPose.rotation(
                        0.0F,
                        (float) Math.toRadians(-18.0F),
                        0.0F
                )
        );

        return mesh;
    }

    private final ModelPart root;

    public VinylRecorderTonearmModel(ModelPart root) {
        this.root = root;
    }

    public ModelPart root() {
        return root;
    }
}
