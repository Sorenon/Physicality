package net.sorenon.physicality.physv2;

import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PhysJNI {
    static {
        System.load("C:\\Users\\soren\\Documents\\Programming\\fabric-example-mod-1.19\\rust\\target\\debug\\mc_phys_jni.dll");
    }

    protected static native long createPhysicsWorld(Level level, PhysicsCallback callback);

    protected static native int step(long physicsWorld, float deltaTime);

    protected static native int addPhysicsBody(long physicsWorld, float x, float y, float z, long outPtr);

    protected static native int getBodyPosition(long physicsWorld, long bodyHandle, Vector3f position);

    protected static native int getBodyRenderTransform(long physicsWorld, long bodyHandle, Vector3f position, Quaternionf orientation);

    protected static native int blockUpdated(long physicalWorld, int x, int y, int z, long addr, long len);

    protected static native int sendBlockInfo(long callbackContext, int index, long addr, long len);
}
