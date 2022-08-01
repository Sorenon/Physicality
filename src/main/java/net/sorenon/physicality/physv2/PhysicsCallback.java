package net.sorenon.physicality.physv2;

public interface PhysicsCallback {
    void preStep(long callbackAddr, long wantedBlocksAddr, int wantedBlocksLen);
}
