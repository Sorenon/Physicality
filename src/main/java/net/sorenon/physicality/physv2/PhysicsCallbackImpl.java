package net.sorenon.physicality.physv2;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.lwjgl.system.MemoryUtil;

public class PhysicsCallbackImpl implements PhysicsCallback {

    private final Level level;

    public PhysicsCallbackImpl(Level level) {
        this.level = level;
    }

    @Override
    public void preStep(long wantedBlocksAddr, int wantedBlocksLen, long outSlicePtr, long outLen) {
//        try {
            System.out.println("Callback " + wantedBlocksLen);

            var wantedPositions = MemoryUtil.memIntBuffer(wantedBlocksAddr, wantedBlocksLen * 3);
//        var out = MemoryUtil.memIntBuffer(outSlicePtr, wantedBlocksLen);
//
            BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
//
            while (wantedPositions.hasRemaining()) {
                int x = wantedPositions.get();
                int y = wantedPositions.get();
                int z = wantedPositions.get();

                blockPos.set(x, y, z);

//                System.out.println(blockPos);

//            if (level.getBlockState(blockPos).isCollisionShapeFullBlock(level, blockPos)) {
//                out.put(1);
//            } else {
//                out.put(0);
//            }
            }
//
            int i = 0;
//        } catch (Exception e) {
//            System.out.println(e);
//        }
    }
}
