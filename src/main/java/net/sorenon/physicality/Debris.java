package net.sorenon.physicality;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Debris {

    public long rigidBody;
    public int decayTicks;
    public BlockState blockState;
    public BlockPos startPos;

    public Debris(long rigidBody, int ticks, BlockState blockState, BlockPos startPos) {
        this.rigidBody = rigidBody;
        this.decayTicks = ticks;
        this.blockState = blockState;
        this.startPos = startPos;
    }

    public void render(ClientLevel level, PoseStack poseStack, MultiBufferSource multiBufferSource) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();

//        var transform = this.rigidBody.getTransform(new Transform());
        var pos = new Vector3f(0, 0, 0);
        var orientation = new Quaternionf();
        PhysicalityModClient.INSTANCE.physicsWorld.getRenderTransform(this.rigidBody, pos, orientation);
//        var rotation = transform.getRotation();

        poseStack.pushPose();
//        BlockPos blockPos = new BlockPos(pos.x, pos.y, pos.z);
        poseStack.translate(pos.x, pos.y, pos.z);

        poseStack.mulPose(new Quaternion(orientation.x, orientation.y, orientation.z, orientation.w));

//        poseStack.scale(0.9f, 0.9f, 0.9f);
        poseStack.translate(-0.5, -0.5, -0.5);
        dispatcher
                .getModelRenderer()
                .tesselateBlock(
                        level,
                        dispatcher.getBlockModel(blockState),
                        blockState,
                        startPos,
                        poseStack,
                        multiBufferSource.getBuffer(ItemBlockRenderTypes.getMovingBlockRenderType(blockState)),
                        false,
                        RandomSource.create(),
                        blockState.getSeed(startPos),
                        OverlayTexture.NO_OVERLAY
                );
        poseStack.popPose();
    }
}
