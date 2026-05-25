package dev.falaris.client.module.modules.movement;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public final class NoFall extends MovementModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Fall damage prevention mode.", "Packet", "Packet", "Motion", "Ground"));
    private final DoubleSetting minFallDistance = setting(new DoubleSetting("Min Fall Distance", "Distance before activating.", 2.5, 0.5, 20.0));
    private final BooleanSetting resetFallDistance = setting(new BooleanSetting("Reset Fall Distance", "Reset client fall distance.", true));

    public NoFall() {
        super("NoFall", "Prevents fall damage entirely.");
    }

    @Override
    protected void onMovementTick(MinecraftClient client) {
        if (client.player == null) return;
        if (client.player.fallDistance < minFallDistance.get() || client.player.isOnGround()) {
            if (client.player.isOnGround() && resetFallDistance.enabled()) client.player.fallDistance = 0.0f;
            return;
        }

        switch (mode.get()) {
            case "Packet" -> {
                if (client.player.networkHandler != null) {
                    client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, client.player.horizontalCollision));
                }
            }
            case "Motion" -> {
                client.player.setVelocity(client.player.getVelocity().x, 0.0, client.player.getVelocity().z);
            }
            case "Ground" -> {
                client.player.setOnGround(true);
                if (client.player.networkHandler != null) {
                    client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, client.player.horizontalCollision));
                }
            }
        }

        if (resetFallDistance.enabled()) {
            client.player.fallDistance = 0.0f;
        }
    }
}
