package dev.falaris.client.mixin;

import dev.falaris.client.module.modules.combat.Hitboxes;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "getTargetingMargin", at = @At("RETURN"), cancellable = true)
    private void onGetTargetingMargin(CallbackInfoReturnable<Float> cir) {
        float expansion = Hitboxes.getExpansion();
        if (expansion != 0.0f) {
            cir.setReturnValue(cir.getReturnValue() + expansion);
        }
    }
}
