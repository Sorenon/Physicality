package net.sorenon.physicality.physv2;

public interface PhysicsCallback {

    void preStep(long wantedBlocksAddr, int wantedBlocksLen, long outSlicePtr, long outLen);
}
