package net.sorenon.physicality.physics_lib.jni;

public interface PhysicsCallback {
    void preStep(long callbackAddr, long wantedBlocksAddr, int wantedBlocksLen);
}
