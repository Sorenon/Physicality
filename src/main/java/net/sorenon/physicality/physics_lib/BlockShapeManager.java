package net.sorenon.physicality.physics_lib;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.math.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

public class BlockShapeManager {

    public final HashMap<Vec3, BoxCollisionShape> boxShapes = new HashMap<>();

    @Nullable
    public CollisionShape getCollisionShape(BlockState blockState, BlockPos pos, BlockGetter level) {
        var collisionShape = blockState.getCollisionShape(level, pos);
        if (collisionShape.isEmpty()) {
            return null;
        }

        List<AABB> aabbs = collisionShape.toAabbs();
        var shape = new CompoundCollisionShape();

        for (var aabb : aabbs) {
            var size = new Vec3(aabb.getXsize(), aabb.getYsize(), aabb.getZsize());
            var center = aabb.getCenter();
            shape.addChildShape(
                    boxShapes.computeIfAbsent(size, vec3 -> new BoxCollisionShape(new Vector3f((float) size.x() / 2, (float) size.y() / 2, (float) size.z() / 2))),
                    (float) center.x, (float) center.y, (float) center.z
            );
        }


        return shape;
    }
}
