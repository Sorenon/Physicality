package net.sorenon.physicality.mixin.client;

import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AgeableListModel.class)
public interface AgeableListModelAcc {

    @Invoker
    Iterable<ModelPart> callHeadParts();

    @Invoker
    Iterable<ModelPart> callBodyParts();
}
