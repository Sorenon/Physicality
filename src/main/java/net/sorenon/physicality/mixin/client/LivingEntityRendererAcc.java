package net.sorenon.physicality.mixin.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAcc {

    @Nullable
    @Invoker
    RenderType callGetRenderType(LivingEntity entity, boolean showBody, boolean translucent, boolean showOutline);
}
