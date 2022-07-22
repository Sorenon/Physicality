package net.sorenon.physicality.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.sorenon.physicality.PhysicalityModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {

    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    void addDebris(BlockPos pos, BlockState state, CallbackInfo ci) {
        ci.cancel();
        //TODO find a better hook for this
        PhysicalityModClient.INSTANCE.addDebris((ClientLevel)(Object)this, pos, state);
    }
}
