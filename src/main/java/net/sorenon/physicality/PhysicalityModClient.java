package net.sorenon.physicality;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.sorenon.physicality.physics_lib.RapierPhysicsWorld;

import java.util.HashSet;

public class PhysicalityModClient implements ClientModInitializer {

    public static PhysicalityModClient INSTANCE;

    public RapierPhysicsWorld rapierPhysicsWorld;

    public HashSet<Debris> debrisList = new HashSet<>();

    @Override
    public void onInitializeClient() {
        INSTANCE = this;

        ClientTickEvents.START_WORLD_TICK.register(world -> {
            if (this.rapierPhysicsWorld == null) {
                this.rapierPhysicsWorld = new RapierPhysicsWorld(world);
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

        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            var poses = context.matrixStack();
            var cam = context.camera().getPosition();
            poses.pushPose();
            poses.translate(-cam.x, -cam.y, -cam.z);

            for (var debris : debrisList) {
                debris.render(context.world(), context.matrixStack(), context.consumers());
            }

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

        long bodyHandle = rapierPhysicsWorld.addBody(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
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
