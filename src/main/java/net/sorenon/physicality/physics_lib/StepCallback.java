package net.sorenon.physicality.physics_lib;

import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.sorenon.physicality.PhysicalityModClient;

public class StepCallback implements PhysicsTickListener {

    private final BoundingBox boundingBox = new BoundingBox();
    private final Vector3f vector3f = new Vector3f();
    private final PhysicsWorld physicsWorld;

    public StepCallback(PhysicsWorld physicsWorld) {
        this.physicsWorld = physicsWorld;
    }

    @Override
    public void prePhysicsTick(PhysicsSpace space, float timeStep) {
        var level = Minecraft.getInstance().level;
        assert level != null;

        for (var debris : PhysicalityModClient.INSTANCE.debrisList) {
            var body = debris.rigidBody;
            if (body.getMass() == 0) {
                continue;
            }
            if (body.isStatic()) {
                continue;
            }
            if (!body.isActive()) {
                continue;
            }
            body.boundingBox(boundingBox);
            boundingBox.getMin(vector3f);
            int minX = (int) Math.floor(vector3f.x - 0.001);
            int minY = (int) Math.floor(vector3f.y - 0.001);
            int minZ = (int) Math.floor(vector3f.z - 0.001);
            boundingBox.getMax(vector3f);
            int maxX = (int) Math.floor(vector3f.x + 0.001);
            int maxY = (int) Math.floor(vector3f.y + 0.001);
            int maxZ = (int) Math.floor(vector3f.z + 0.001);

            var blockPos = new BlockPos.MutableBlockPos();

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        blockPos.set(x, y, z);

                        if (!physicsWorld.blocks.containsKey(blockPos)) {
                            physicsWorld.addBlock(blockPos.immutable(), level, space);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void physicsTick(PhysicsSpace space, float timeStep) {

    }
}
