package net.sorenon.physicality.mixin.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.sorenon.physicality.PhysicalityModClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class LevelMixin {

    @Shadow @Final public boolean isClientSide;

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("RETURN"))
    void onSetBlock(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        if (this.isClientSide && cir.getReturnValue()) {
            //TODO activate inactive debris
            var physicsWorld = PhysicalityModClient.INSTANCE.physicsWorld;
            var body = physicsWorld.blocks.remove(pos);
            if (body != null) {
                physicsWorld.physicsSpace.remove(body);
            }
        }
    }
}
