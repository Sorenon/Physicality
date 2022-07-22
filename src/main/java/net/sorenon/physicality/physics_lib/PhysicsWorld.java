package net.sorenon.physicality.physics_lib;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.util.DebugShapeFactory;
import com.jme3.math.Vector3f;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.sorenon.physicality.PhysicalityMod;

import java.util.HashMap;

public class PhysicsWorld {

    public final PhysicsSpace physicsSpace;
    public final StepCallback stepCallback;

    public final BlockShapeManager blockShapeManager = PhysicalityMod.INSTANCE.blockShapeManager;
    public final HashMap<BlockPos, PhysicsRigidBody> blocks = new HashMap<>();
//    public final HashMap<BlockPos, >

    public PhysicsWorld() {
        physicsSpace = new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);
        stepCallback = new StepCallback(this);
        physicsSpace.addTickListener(stepCallback);
    }

    public void step(float deltaTime) {
        physicsSpace.update(deltaTime, 10);
    }

    public void addBlock(BlockPos pos, ClientLevel level, PhysicsSpace space) {
        var state = level.getBlockState(pos);
        if (state.getRenderShape() != RenderShape.MODEL) {
            //TODO
            return;
        }
        var shape = blockShapeManager.getCollisionShape(state, pos, level);

        if (shape == null) {
            return;
        }

        var collObj = new PhysicsRigidBody(shape, 0);
        collObj.setPhysicsLocation(new Vector3f(pos.getX(), pos.getY(), pos.getZ()));
        space.add(collObj);
        blocks.put(pos, collObj);
    }
}
