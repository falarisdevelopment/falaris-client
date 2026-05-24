package dev.falaris.client.mixin;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.module.Module;
import dev.falaris.client.module.modules.render.DamageIndicator;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackEntity", at = @At("TAIL"))
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        Optional<Module> found = FalarisClient.getInstance().getModuleManager().find("damage-indicator");
        if (found.isPresent()) {
            DamageIndicator di = (DamageIndicator) found.get();
            di.recordHit(target, 1.0f);
        }
    }
}
