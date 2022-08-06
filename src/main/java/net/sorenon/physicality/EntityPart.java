package net.sorenon.physicality;

import net.minecraft.client.model.geom.ModelPart;
import net.sorenon.physicality.mixin.client.ModelPartAcc;
import org.joml.Vector3f;
import oshi.util.tuples.Pair;

import java.util.ArrayList;
import java.util.List;

public class EntityPart {
    public final ModelPart modelPart;
    public final Vector3f centerOffset;
    public final Vector3f renderScale;

    public EntityPart(ModelPart modelPart) {
        this.modelPart = modelPart;
        this.centerOffset = new Vector3f();
        this.renderScale = new Vector3f(1, 1, 1);

        calculateCenter(((ModelPartAcc) (Object) modelPart).getCubes(), this.centerOffset);
    }

    public static void calculateCenter(List<ModelPart.Cube> cubes, Vector3f out) {
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

        out.set(
                +minX + ((maxX - minX) / 2),
                +minY + ((maxY - minY) / 2),
                +minZ + ((maxZ - minZ) / 2)
        );

        out.div(16);
    }

    public void calculateCubes(ArrayList<Pair<Vector3f, Vector3f>> cubesOut) {
        var cubes = ((ModelPartAcc) (Object) modelPart).getCubes();
        if (cubes.size() == 1) {
            var cube = cubes.get(0);
            var sizeOut = new Vector3f();

            sizeOut.set(
                    ((cube.maxX - cube.minX) / 2),
                    ((cube.maxY - cube.minY) / 2),
                    ((cube.maxZ - cube.minZ) / 2)
            );
            sizeOut.div(16);
            cubesOut.add(new Pair<>(sizeOut, new Vector3f()));
        }

        for (var cube : cubes) {
            var size = new Vector3f(
                    ((cube.maxX - cube.minX) / 2),
                    ((cube.maxY - cube.minY) / 2),
                    ((cube.maxZ - cube.minZ) / 2)
            );
            size.div(16);

            var pos = new Vector3f(
                    +cube.minX + ((cube.maxX - cube.minX) / 2),
                    +cube.minY + ((cube.maxY - cube.minY) / 2),
                    +cube.minZ + ((cube.maxZ - cube.minZ) / 2)
            );
            pos.div(16);
            pos.sub(this.centerOffset);

            cubesOut.add(new Pair<>(size, pos));
        }
    }
}
