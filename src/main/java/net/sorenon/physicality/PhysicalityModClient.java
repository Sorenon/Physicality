package net.sorenon.physicality;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.sorenon.physicality.mixin.client.AgeableListModelAcc;
import net.sorenon.physicality.mixin.client.LivingEntityRendererAcc;
import net.sorenon.physicality.physics_lib.DebugRenderer;
import net.sorenon.physicality.physics_lib.PhysicsWorld;
import net.sorenon.physicality.physics_lib.jni.PhysJNI;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class PhysicalityModClient implements ClientModInitializer {

    public static PhysicalityModClient INSTANCE;

    public PhysicsWorld physicsWorld;

    public HashSet<Debris> debrisList = new HashSet<>();

    private KeyMapping key;

    private List<RagdollPart> ragdollPartList = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        INSTANCE = this;


        key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.physicality.ragdoll", // The translation key of the keybinding's name
                InputConstants.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_R, // The keycode of the key
                "category.physicality.test" // The translation key of the keybinding's category.
        ));

        ClientTickEvents.START_WORLD_TICK.register(world -> {
            if (this.physicsWorld == null) {
                this.physicsWorld = new PhysicsWorld(world);
            }

            debrisList.removeIf(debris -> {
                debris.decayTicks -= 1;
                if (debris.decayTicks <= 0) {
                    //TODO
//                    physicsWorld.physicsSpace.remove(debris.rigidBody);
                    return true;
                }
                return false;
            });
        });

        var debugRenderer = new DebugRenderer();

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            var poses = context.matrixStack();
            var cam = context.camera().getPosition();
            poses.pushPose();
            poses.translate(-cam.x, -cam.y, -cam.z);

            for (var debris : debrisList) {
                debris.render(context.world(), context.matrixStack(), context.consumers());
            }

            while (key.consumeClick()) {
                var fakeCow = new Cow(EntityType.COW, context.world());
                var entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
                var cowRenderer = entityRenderDispatcher.getRenderer(fakeCow);

                if (cowRenderer instanceof LivingEntityRenderer livingRenderer) {
                    var model = livingRenderer.getModel();

                    var renderType = ((LivingEntityRendererAcc) livingRenderer).callGetRenderType(fakeCow, true, false, false);
                    if (renderType != null) {
                        if (model instanceof AgeableListModelAcc ageableListModel) {
                            var bodyParts = ageableListModel.callBodyParts();
                            for (var part : bodyParts) {
                                this.ragdollPartList.add(new RagdollPart(part, renderType, this.physicsWorld));
                            }
                            bodyParts = ageableListModel.callHeadParts();
                            for (var part : bodyParts) {
                                this.ragdollPartList.add(new RagdollPart(part, renderType, this.physicsWorld));
                            }
                        }
                    }
                }
            }

            for (var ragdollPart : this.ragdollPartList) {
                ragdollPart.render(poses, context.consumers());
            }

            if (this.physicsWorld != null) {
                debugRenderer.render(this.physicsWorld, poses.last(), context.consumers());
            }

//            poses.pushPose();
//            poses.translate(10, 0, 10);

            //Mojang????
//            poses.scale(-1.0F, -1.0F, 1.0F);
//            poses.translate(0.0, -1.501F, 0.0);
//
//            if (cowRenderer instanceof LivingEntityRenderer livingRenderer) {
//                var model = livingRenderer.getModel();
//
//                var renderType = ((LivingEntityRendererAcc) livingRenderer).callGetRenderType(fakeCow, true, false, false);
//                if (renderType != null) {
//                    var vertexConsumer = context.consumers().getBuffer(renderType);
//
//                    if (model instanceof AgeableListModelAcc ageableListModel) {
//                        var bodyParts = ageableListModel.callBodyParts();
//                        for (var part : bodyParts) {
//                            poses.pushPose();
//                            var entityPart = new EntityPart(part);
//                            var partPose = part.getInitialPose();
//
//                            poses.translate(partPose.x / 16, partPose.y / 16, partPose.z / 16);
//                            poses.translate(entityPart.centerOffset.x, entityPart.centerOffset.y, entityPart.centerOffset.z);
//
//                            //Actual needed translation
//                            poses.translate(-entityPart.centerOffset.x, -entityPart.centerOffset.y, -entityPart.centerOffset.z);
//
//                            if (partPose.zRot != 0.0F) {
//                                poses.mulPose(Vector3f.ZP.rotation(partPose.zRot));
//                            }
//
//                            if (partPose.yRot != 0.0F) {
//                                poses.mulPose(Vector3f.YP.rotation(partPose.yRot));
//                            }
//
//                            if (partPose.xRot != 0.0F) {
//                                poses.mulPose(Vector3f.XP.rotation(partPose.xRot));
//                            }
//                            var partAcc = (ModelPartAcc) (Object) entityPart.modelPart;
//                            var pose = poses.last();
//                            partAcc.callCompile(pose, vertexConsumer, LightTexture.FULL_SKY, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
//                            poses.popPose();
//                        }
//                    }
//                }
//            }

//            poses.popPose();

//            for (var body : this.physicsWorld.physicsSpace.getRigidBodyList()) {
//                var transform = body.getTransform(new Transform());
//                var pos = transform.getTranslation();
//                var rotation = transform.getRotation();
//
////                var dispatcher = Minecraft.getInstance().getBlockRenderer();
//
//                poses.pushPose();
//                BlockPos blockPos = new BlockPos(pos.x, pos.y, pos.z);
//                poses.translate(pos.x, pos.y, pos.z);
//
//                poses.mulPose(new Quaternion(rotation.getX(), rotation.getY(), rotation.getZ(), rotation.getW()));
//
//                var buffer = context.consumers().getBuffer(RenderType.LINES);
//                var points = DebugShapeFactory.debugVertices(body.getCollisionShape(), 1);
//                var pose = poses.last().pose();
//
//                points.flip();
//                while (points.hasRemaining()) {
//                    float x = points.get();
//                    float y = points.get();
//                    float z = points.get();
//
//                    buffer.vertex(pose, x, y, z)
//                            .color(255, 10, 10, 255)
//                            .normal(0, 0, 0)
//                            .endVertex();
//                }
//                if (points.limit() % 2 != 0) {
//                    buffer.vertex(pose, 0, 0, 0)
//                            .color(255, 10, 10, 255)
//                            .normal(0, 0, 0)
//                            .endVertex();
//                }
//
////                poses.translate(-0.05f, -0.05f, -0.05f);
////                poses.scale(1.1f, 1.1f, 1.1f);
////                dispatcher
////                        .getModelRenderer()
////                        .tesselateBlock(
////                                context.world(),
////                                dispatcher.getBlockModel(Blocks.GLASS.defaultBlockState()),
////                                Blocks.GLASS.defaultBlockState(),
////                                blockPos,
////                                poses,
////                                context.consumers().getBuffer(ItemBlockRenderTypes.getMovingBlockRenderType(Blocks.GLASS.defaultBlockState())),
////                                false,
////                                RandomSource.create(),
////                                0,
////                                OverlayTexture.NO_OVERLAY
////                        );
//                poses.popPose();
//            }

            poses.translate(0, 80, 0);
            drawDebris(poses, context.consumers(), Blocks.BRICKS.defaultBlockState(), context.world(), new BlockPos(0, 80, 0));

            poses.popPose();
        });
    }

    public void addDebris(ClientLevel clientLevel, BlockPos pos, BlockState state) {
        var collisionShape = state.getShape(clientLevel, pos);
        if (collisionShape.isEmpty()) {
            return;
        }

        long bodyHandle = physicsWorld.addBody(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
        var debris = new Debris(bodyHandle, 10000, state, pos);

        this.debrisList.add(debris);


        //        collisionShape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
//            var min = new Vector3f((float) minX, (float) minY, (float) minZ);
//            var max = new Vector3f((float) maxX, (float) maxY, (float) maxZ);
//            var center = max.add(min).divideLocal(2);
//            min.subtractLocal(center);
//            max.subtractLocal(center);
//
//            var convexHull = new ConvexHull(
//                    ImmutableList.of(
//                            min,
//                            max,
//                            new Vector3f(
//                                    max.x,
//                                    min.y,
//                                    min.z
//                            ),
//                            new Vector3f(
//                                    max.x,
//                                    max.y,
//                                    min.z
//                            ),
//                            new Vector3f(
//                                    max.x,
//                                    min.y,
//                                    max.z
//                            ),
//                            new Vector3f(
//                                    min.x,
//                                    max.y,
//                                    min.z
//                            ),
//                            new Vector3f(
//                                    min.x,
//                                    max.y,
//                                    max.z
//                            ),
//                            new Vector3f(
//                                    min.x,
//                                    min.y,
//                                    max.z
//                            )
//                    )
//            );
//
////            var plane = new Plane(new Vector3f(0, 0, 1), 0);
//
////            convexHull = convexHull.slice(plane, 0.1f);
//
//            var shape = new HullCollisionShape(convexHull.points());
//
//            shape.setScale(0.9f);
//            var ball = new PhysicsRigidBody(shape, 1.0f);
//            ball.setPhysicsLocation(new Vector3f(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f));
//            ball.setLinearVelocity(new Vector3f((clientLevel.random.nextFloat() - 0.5f) / 2, 0, (clientLevel.random.nextFloat() - 0.5f) / 2));
//            var debris = new Debris(ball, 100, state, pos);
//
//            this.physicsWorld.physicsSpace.add(debris.rigidBody);
//            this.debrisList.add(debris);
//        });
    }

    public void drawDebris(PoseStack poseStack,
                           MultiBufferSource multiBufferSource,
                           BlockState blockState,
                           Level level,
                           BlockPos pos) {
        var pose = poseStack.last().pose();
        var normal = poseStack.last().normal();
        var buffer = multiBufferSource.getBuffer(ItemBlockRenderTypes.getMovingBlockRenderType(blockState));

        int light = LightTexture.pack(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos));

        buffer.vertex(pose, 0, 0, 0)
                .color(255, 255, 255, 255)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0, 1, 0)
                .endVertex();

        buffer.vertex(pose, 0, 0, 0)
                .color(255, 255, 255, 255)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0, 1, 0)
                .endVertex();

        buffer.vertex(pose, 0, 0, 1)
                .color(255, 255, 255, 255)
                .uv(0, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0, 1, 0)
                .endVertex();

        buffer.vertex(pose, 1, 0, 1)
                .color(255, 255, 255, 255)
                .uv(1, 1)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0, 1, 0)
                .endVertex();
    }
}
