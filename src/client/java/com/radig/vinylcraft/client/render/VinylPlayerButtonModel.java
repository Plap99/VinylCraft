package com.radig.vinylcraft.client.render;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Controles físicos del Vinyl Player.
 *
 * Paso 6: se movieron del borde superior al panel frontal y se hicieron
 * más grandes para que sean fáciles de ver y de pulsar.
 *
 * Orden: anterior · stop · play/pausa · siguiente.
 */
public class VinylPlayerButtonModel {

    public static final float Y = 1.55F;
    public static final float Z = 0.48F;
    public static final float WIDTH = 2.25F;
    public static final float HEIGHT = 1.25F;
    public static final float DEPTH = 0.58F;

    public static final float PREVIOUS_X = 11.50F;
    public static final float STOP_X = 8.25F;
    public static final float PLAY_X = 5.00F;
    public static final float NEXT_X = 1.75F;

    public static final float ICON_Y = Y + HEIGHT / 2.0F;
    public static final float ICON_Z = Z - 0.07F;

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        addButton(root, "previous", PREVIOUS_X);
        addButton(root, "stop", STOP_X);
        addButton(root, "play_pause", PLAY_X);
        addButton(root, "next", NEXT_X);

        /*
         * 16x16 evita que las UV de las caras laterales del cubo salgan
         * del sprite del botón y muestreen píxeles vecinos del atlas.
         * Ese desbordamiento era el origen más probable del pequeño
         * cuadrito fantasma que aparecía en una esquina de cada control.
         */
        return LayerDefinition.create(mesh, 16, 16);
    }

    private static void addButton(
            PartDefinition root,
            String name,
            float x) {

        root.addOrReplaceChild(
                name,
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(
                                x,
                                Y,
                                Z,
                                WIDTH,
                                HEIGHT,
                                DEPTH
                        ),
                PartPose.ZERO
        );
    }

    private final ModelPart previous;
    private final ModelPart stop;
    private final ModelPart playPause;
    private final ModelPart next;

    public VinylPlayerButtonModel(ModelPart root) {
        this.previous = root.getChild("previous");
        this.stop = root.getChild("stop");
        this.playPause = root.getChild("play_pause");
        this.next = root.getChild("next");
    }

    public ModelPart previous() {
        return previous;
    }

    public ModelPart stop() {
        return stop;
    }

    public ModelPart playPause() {
        return playPause;
    }

    public ModelPart next() {
        return next;
    }
}
