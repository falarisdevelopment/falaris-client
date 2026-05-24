package dev.falaris.client.mixin;

import dev.falaris.client.module.modules.player.NoSlowdown;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "travel", at = @At("HEAD"))
    private void onTravelHead(CallbackInfo ci) {
        if (!NoSlowdown.shouldCancelSlowdown()) return;
        //noinspection ConstantValue
        if (!((Object) this instanceof ClientPlayerEntity)) return;
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        if (player.isUsingItem()) {
            // Pre-counter the 0.2x slowdown that travel() will apply
            player.setVelocity(player.getVelocity().multiply(5.0, 1.0, 5.0));
        }
    }
}
