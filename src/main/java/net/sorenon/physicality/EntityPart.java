package net.sorenon.physicality;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.sorenon.physicality.mixin.client.ModelPartAcc;
import org.joml.Vector3f;

import java.util.List;

public class EntityPart {
    public final ModelPart modelPart;
    public final Vector3f centerOffset;
    public final Vector3f renderScale;

    public EntityPart(ModelPart modelPart, Vector3f size) {
        this.modelPart = modelPart;
        this.centerOffset = new Vector3f();
        this.renderScale = new Vector3f(1, 1, 1);

        calculateCenter(((ModelPartAcc) (Object) modelPart).getCubes(), size, this.centerOffset);
    }

    public static void calculateCenter(List<ModelPart.Cube> cubes, Vector3f sizeOut, Vector3f out) {
        float maxX = 0;
        float maxY = 0;
        float maxZ = 0;
        float minX = 0;
        float minY = 0;
        float minZ = 0;

        for (var cube : cubes) {
            maxX = Math.max(cube.maxX, maxX);
            minX = Math.min(cube.minX, minX);
            maxY = Math.max(cube.maxY, maxY);
            minY = Math.min(cube.minY, minY);
            maxZ = Math.max(cube.maxZ, maxZ);
            minZ = Math.min(cube.minZ, minZ);
        }

        sizeOut.set(
                ((maxX - minX) / 2),
                ((maxY - minY) / 2),
                ((maxZ - minZ) / 2)
        );
        sizeOut.div(16);

        out.set(
                +minX + ((maxX - minX) / 2),
                +minY + ((maxY - minY) / 2),
                +minZ + ((maxZ - minZ) / 2)
        );

        out.div(16);
    }
}
