package dev.falaris.client.mixin;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.module.Module;
import dev.falaris.client.module.modules.combat.Reach;
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
    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        double reachSq = 36.0;
        Optional<Module> found = FalarisClient.getInstance().getModuleManager().find("reach");
        if (found.isPresent() && found.get().isEnabled()) {
            Reach reach = (Reach) found.get();
            reachSq = reach.getAttackReach() * reach.getAttackReach();
        }
        if (player.squaredDistanceTo(target) > reachSq) {
            ci.cancel();
        }
    }
}
