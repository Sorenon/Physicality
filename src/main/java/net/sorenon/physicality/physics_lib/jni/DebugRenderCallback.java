package net.sorenon.physicality.physics_lib.jni;

public interface DebugRenderCallback {
    void renderLine(float fromX,
                    float fromY,
                    float fromZ,
                    float toX,
                    float toY,
                    float toZ,
                    float r,
                    float g,
                    float b,
                    float a);
}
