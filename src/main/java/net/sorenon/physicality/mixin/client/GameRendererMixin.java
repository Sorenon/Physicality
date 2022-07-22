package net.sorenon.physicality.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.sorenon.physicality.PhysicalityModClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow @Final private Minecraft minecraft;

    @Shadow public abstract Minecraft getMinecraft();

    @Unique
    private long time = System.currentTimeMillis();

    @Inject(method = "render", at = @At("HEAD"))
    void preRender(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        long newTime = System.currentTimeMillis();

        if (tick && this.minecraft.level != null && !this.getMinecraft().isPaused()) {
            PhysicalityModClient.INSTANCE.physicsWorld.step((newTime - time) / 1000f);
        }

        this.time = newTime;
    }
}
