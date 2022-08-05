package net.sorenon.physicality.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ModelPart.class)
public interface ModelPartAcc {

    @Invoker
    void callCompile(PoseStack.Pose entry, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha);

    @Accessor
    List<ModelPart.Cube> getCubes();
}
