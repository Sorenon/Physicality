package net.sorenon.physicality.physics_lib.jni;

import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PhysJNI {
    static {
        NativeLoader.load();
    }

    public static native long createPhysicsWorld(Level level, PhysicsCallback callback);

    public static native int step(long physicsWorld, float deltaTime);

    public static native int addPhysicsBody(long physicsWorld, float x, float y, float z, long outPtr);

    public static native int addCuboid(
            long physicsWorld,
            float x,
            float y,
            float z,
            float ox,
            float oy,
            float oz,
            float ow,
            long shapes_addr,
            int shapes_num,
            long outPtr
    );

    public static native int getBodyPosition(long physicsWorld, long bodyHandle, Vector3f position);

    public static native int getBodyRenderTransform(long physicsWorld,
                                                    long bodyHandle,
                                                    Vector3f position,
                                                    Quaternionf orientation);

    public static native int blockUpdated(long physicalWorld, int x, int y, int z, long addr, long len);

    public static native int sendBlockInfo(long callbackContext, int index, long addr, long len);
}
