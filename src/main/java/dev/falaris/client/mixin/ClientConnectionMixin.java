package dev.falaris.client.mixin;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.module.Module;
import dev.falaris.client.module.modules.misc.Blink;
import dev.falaris.client.module.modules.misc.FakeLag;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Inject(method = "send", at = @At("HEAD"), cancellable = true)
    private void onSend(Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof PlayerMoveC2SPacket)) return;

        Optional<Module> blink = FalarisClient.getInstance().getModuleManager().find("blink");
        if (blink.isPresent()) {
            Blink b = (Blink) blink.get();
            if (b.shouldHoldPacket()) { b.holdPacket((PlayerMoveC2SPacket) packet); ci.cancel(); return; }
        }

        Optional<Module> fakelag = FalarisClient.getInstance().getModuleManager().find("fakelag");
        if (fakelag.isPresent()) {
            FakeLag f = (FakeLag) fakelag.get();
            if (f.shouldHoldPacket()) { f.holdPacket((PlayerMoveC2SPacket) packet); ci.cancel(); }
        }
    }
}
