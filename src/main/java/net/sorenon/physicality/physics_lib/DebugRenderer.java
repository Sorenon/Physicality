package net.sorenon.physicality.physics_lib;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.sorenon.physicality.physics_lib.jni.DebugRenderCallback;
import net.sorenon.physicality.physics_lib.jni.PhysJNI;

public class DebugRenderer implements DebugRenderCallback {

    private VertexConsumer vertexConsumer;
    private PoseStack.Pose cameraPose;

    public void render(PhysicsWorld physicsWorld, PoseStack.Pose cameraPose, MultiBufferSource multiBufferSource) {
        this.vertexConsumer = multiBufferSource.getBuffer(RenderType.LINES);
        this.cameraPose = cameraPose;

        PhysJNI.debugRender(physicsWorld.handle, this);
    }

    //TODO batch draw calls
    @Override
    public void renderLine(float fromX,
                           float fromY,
                           float fromZ,
                           float toX,
                           float toY,
                           float toZ,
                           float r,
                           float g,
                           float b,
                           float a) {
        float dx = fromX - toX;
        float dy = fromY - toY;
        float dz = fromZ - toZ;
        float t = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= t;
        dy /= t;
        dz /= t;
        this.vertexConsumer.vertex(this.cameraPose.pose(), fromX, fromY, fromZ).color(r, g, b, a).normal(this.cameraPose.normal(), dx, dy, dz).endVertex();
        this.vertexConsumer.vertex(this.cameraPose.pose(), toX, toY, toZ).color(r, g, b, a).normal(this.cameraPose.normal(), dx, dy, dz).endVertex();
    }
}
