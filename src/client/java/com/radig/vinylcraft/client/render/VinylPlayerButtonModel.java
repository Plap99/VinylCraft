package com.radig.vinylcraft.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Dynamic front buttons. Rendered full-bright by the block entity renderer. */
public class VinylPlayerButtonModel {

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
                "play_pause",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(3.0F, 5.05F, 0.85F, 1.25F, 0.40F, 0.50F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "stop",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(5.0F, 5.05F, 0.85F, 1.25F, 0.40F, 0.50F),
                PartPose.ZERO
        );

        // Logical UV size deliberately kept small so the tiny physical buttons
        // use most of each 16x16 sprite instead of sampling only a few pixels.
        return LayerDefinition.create(mesh, 2, 2);
    }

    private final ModelPart playPause;
    private final ModelPart stop;

    public VinylPlayerButtonModel(ModelPart root) {
        this.playPause = root.getChild("play_pause");
        this.stop = root.getChild("stop");
    }

    public ModelPart playPause() { return playPause; }
    public ModelPart stop() { return stop; }
}
