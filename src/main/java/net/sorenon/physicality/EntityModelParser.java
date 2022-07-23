package net.sorenon.physicality;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

public class EntityModelParser {

    private final EntityRenderDispatcher entityRenderDispatcher;

    public EntityModelParser() {
        this.entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
    }

    public <E extends LivingEntity> void parseEntityModel(E entity, float tickDelta) {
        EntityRenderer<? super E> renderer = entityRenderDispatcher.getRenderer(entity);
        if (renderer instanceof LivingEntityRenderer livingEntityRenderer && livingEntityRenderer.getModel() instanceof HierarchicalModel model) {
            PoseStack poseStack = new PoseStack();

            //noinspection unchecked
            livingEntityRenderer.render(
                    entity,
                    Mth.lerp(tickDelta, entity.yRotO, entity.getYRot()),
                    0,
                    poseStack,
                    new NullBufferSource(),
                    this.entityRenderDispatcher.getPackedLightCoords(entity, tickDelta)
            );
        }
    }

    private static class NullBufferSource implements MultiBufferSource {

        @Override
        public VertexConsumer getBuffer(RenderType layer) {
            return null;
        }
    }

    private static class NullVertexConsumer implements VertexConsumer {

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        @Override
        public void endVertex() {

        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {

        }

        @Override
        public void unsetDefaultColor() {

        }
    }
}
