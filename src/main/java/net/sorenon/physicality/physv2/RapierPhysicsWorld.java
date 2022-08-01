package net.sorenon.physicality.physv2;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.HashSet;

public class RapierPhysicsWorld implements PhysicsCallback {

    private final int handle;
    private final Level level;

    private final HashSet<BlockPos> trackingBlocks = new HashSet<>();

    public RapierPhysicsWorld(Level level) {
        this.level = level;
        this.handle = PhysJNI.createPhysicsWorld(level, this);
    }

    public void step(float deltaTime) {
        if (PhysJNI.step(handle, deltaTime) < 0) {
            throw new RuntimeException();
        }
    }

    public long addBody(float x, float y, float z) {
        try (var stack = MemoryStack.stackPush()) {
            var bodyHandle = stack.callocLong(1);
            if (PhysJNI.addPhysicsBody(handle, x, y, z, MemoryUtil.memAddress(bodyHandle)) < 0) {
                throw new RuntimeException();
            }
            return bodyHandle.get();
        }
    }

    public void getBodyPosition(long bodyHandle, Vector3f position) {
        if (PhysJNI.getBodyPosition(handle, bodyHandle, position) < 0) {
            throw new RuntimeException();
        }
    }

    public void getRenderTransform(long bodyHandle, Vector3f position, Quaternionf orientation) {
        if (PhysJNI.getBodyRenderTransform(handle, bodyHandle, position, orientation) < 0) {
            throw new RuntimeException();
        }
    }

    private FloatBuffer blockShapeBuffer = MemoryUtil.memCallocFloat(0);

    @Override
    public void preStep(long callbackContext, long wantedBlocksAddr, int wantedBlocksLen) {
        try {
            var wantedPositions = MemoryUtil.memIntBuffer(wantedBlocksAddr, wantedBlocksLen * 3);

            for (int i = 0; i < wantedBlocksLen; i++) {
                int x = wantedPositions.get();
                int y = wantedPositions.get();
                int z = wantedPositions.get();

                BlockPos blockPos = new BlockPos(x, y, z);

                this.trackingBlocks.add(blockPos);

                var collider = level.getBlockState(blockPos).getCollisionShape(level, blockPos);
                if (collider.isEmpty()) {
                    continue;
                }

                collider.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
                    if (blockShapeBuffer.position() >= blockShapeBuffer.limit()) {
                        var oldBuffer = blockShapeBuffer;
                        blockShapeBuffer = MemoryUtil.memCallocFloat(oldBuffer.limit() + 6);
                        MemoryUtil.memCopy(oldBuffer, blockShapeBuffer);
                        blockShapeBuffer.position(oldBuffer.position());
                    }

                    blockShapeBuffer.put((float) minX);
                    blockShapeBuffer.put((float) minY);
                    blockShapeBuffer.put((float) minZ);
                    blockShapeBuffer.put((float) maxX);
                    blockShapeBuffer.put((float) maxY);
                    blockShapeBuffer.put((float) maxZ);
                });

                PhysJNI.sendBlockInfo(callbackContext, i, MemoryUtil.memAddress0(blockShapeBuffer), blockShapeBuffer.position() / 6);
                blockShapeBuffer.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setBlockDirty(BlockPos pos) {
        if (trackingBlocks.contains(pos)) {
            var collider = level.getBlockState(pos).getCollisionShape(level, pos);

            collider.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
                if (blockShapeBuffer.position() >= blockShapeBuffer.limit()) {
                    var oldBuffer = blockShapeBuffer;
                    blockShapeBuffer = MemoryUtil.memCallocFloat(oldBuffer.limit() + 6);
                    MemoryUtil.memCopy(oldBuffer, blockShapeBuffer);
                    blockShapeBuffer.position(oldBuffer.position());
                }

                blockShapeBuffer.put((float) minX);
                blockShapeBuffer.put((float) minY);
                blockShapeBuffer.put((float) minZ);
                blockShapeBuffer.put((float) maxX);
                blockShapeBuffer.put((float) maxY);
                blockShapeBuffer.put((float) maxZ);
            });

            PhysJNI.blockUpdated(handle, pos.getX(), pos.getY(), pos.getZ(), MemoryUtil.memAddress0(blockShapeBuffer), blockShapeBuffer.position() / 6);
            blockShapeBuffer.clear();
        }
    }
}
