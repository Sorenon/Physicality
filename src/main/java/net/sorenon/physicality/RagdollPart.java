package net.sorenon.physicality;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.sorenon.physicality.mixin.client.ModelPartAcc;
import net.sorenon.physicality.physics_lib.PhysicsWorld;
import net.sorenon.physicality.physics_lib.jni.PhysJNI;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RagdollPart {

    public EntityPart entityPart;

    private Vector3f translation;
    private Quaternionf orientation;
    private RenderType renderType;

    private long body;
    private PhysicsWorld physicsWorld;

    public RagdollPart(ModelPart modelPart, RenderType renderType, PhysicsWorld physicsWorld) {
        var size = new Vector3f();
        this.entityPart = new EntityPart(modelPart, size);
        this.renderType = renderType;

//        poses.scale(-1.0F, -1.0F, 1.0F);
//        poses.translate(0.0, -1.501F, 0.0);

        var partPose = modelPart.getInitialPose();
        this.translation = new Vector3f(partPose.x / 16f, partPose.y / 16f, partPose.z / 16f);
        //Go home Mojang you're drunk
        this.translation.mul(-1, -1, 1);
        this.translation.add(0.0f, 1.501F, 0.0f);

        this.translation.add(this.entityPart.centerOffset);

        this.orientation = new Quaternionf();

//        if (partPose.zRot != 0.0F) {
            orientation.rotateAxis(partPose.zRot + (float)Math.toRadians(180f), new Vector3f(0, 0, 1));
//        }

        if (partPose.yRot != 0.0F) {
            orientation.rotateAxis(partPose.yRot, new Vector3f(0, 1, 0));
        }

        if (partPose.xRot != 0.0F) {
            orientation.rotateAxis(partPose.xRot, new Vector3f(1, 0, 0));
        }

        this.body = physicsWorld.addCuboid(translation, orientation, size);
        this.physicsWorld = physicsWorld;
    }

    public void render(PoseStack poses, MultiBufferSource vertexConsumers) {
        poses.pushPose();

        physicsWorld.getRenderTransform(this.body, this.translation, this.orientation);


        poses.translate(this.translation.x, this.translation.y, this.translation.z);
        poses.translate(-this.entityPart.centerOffset.x, -this.entityPart.centerOffset.y, -this.entityPart.centerOffset.z);
        poses.mulPose(new Quaternion(this.orientation.x, this.orientation.y, this.orientation.z, this.orientation.w));


        var partAcc = (ModelPartAcc) (Object) this.entityPart.modelPart;
        var pose = poses.last();
        partAcc.callCompile(pose, vertexConsumers.getBuffer(this.renderType), LightTexture.FULL_SKY, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);

        poses.popPose();
    }
}
