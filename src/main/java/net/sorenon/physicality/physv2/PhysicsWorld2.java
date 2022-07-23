package net.sorenon.physicality.physv2;

import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class PhysicsWorld2 {

    private final int handle;
    private final PhysicsCallbackImpl callback;

    public PhysicsWorld2(Level level) {
        this.callback = new PhysicsCallbackImpl(level);
        this.handle = PhysJNI.createPhysicsWorld(level, callback);
    }

    public void step(float deltaTime) {
        if (PhysJNI.step(handle, deltaTime) < 0) {
            throw new RuntimeException();
        }
    }

    public long addBody(float x, float y, float z) {
        try (var stack = MemoryStack.stackPush()) {
            var bodyHandle = stack.callocLong(1);
            if (PhysJNI.addPhysicsBody(handle, x, y, z, MemoryUtil.memAddress(bodyHandle)) < 0) {
                throw new RuntimeException();
            }
            return bodyHandle.get();
        }
    }

    public void getBodyPosition(long bodyHandle, Vector3f position) {
        if (PhysJNI.getBodyPosition(handle, bodyHandle, position) < 0) {
            throw new RuntimeException();
        }
    }

    public void getRenderTransform(long bodyHandle, Vector3f position, Quaternionf orientation) {
        if (PhysJNI.getBodyRenderTransform(handle, bodyHandle, position, orientation) < 0) {
            throw new RuntimeException();
        }
    }
}
