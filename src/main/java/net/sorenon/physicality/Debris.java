package net.sorenon.physicality;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Transform;
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

public class Debris {

    public PhysicsRigidBody rigidBody;
    public int decayTicks;
    public BlockState blockState;
    public BlockPos startPos;

    public Debris(PhysicsRigidBody rigidBody, int ticks, BlockState blockState, BlockPos startPos) {
        this.rigidBody = rigidBody;
        this.decayTicks = ticks;
        this.blockState = blockState;
        this.startPos = startPos;
    }

    public void render(ClientLevel level, PoseStack poseStack, MultiBufferSource multiBufferSource) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();

        var transform = this.rigidBody.getTransform(new Transform());
        var pos = transform.getTranslation();
        var rotation = transform.getRotation();

        poseStack.pushPose();
//        BlockPos blockPos = new BlockPos(pos.x, pos.y, pos.z);
        poseStack.translate(pos.x, pos.y, pos.z);

        poseStack.mulPose(new Quaternion(rotation.getX(), rotation.getY(), rotation.getZ(), rotation.getW()));

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
