package dev.falaris.client.module.modules.movement;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public final class NoFall extends MovementModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Fall damage prevention mode.", "Packet", "Packet", "Motion", "Ground"));
    private final DoubleSetting minFallDistance = setting(new DoubleSetting("Min Fall Distance", "Distance before activating.", 2.5, 0.5, 20.0));
    private final IntegerSetting packetDelay = setting(new IntegerSetting("Packet Delay", "Ticks between on-ground packets.", 2, 1, 20));
    private final IntegerSetting packetJitter = setting(new IntegerSetting("Packet Jitter", "Random extra ticks between packets.", 1, 0, 10));
    private final BooleanSetting resetFallDistance = setting(new BooleanSetting("Reset Fall Distance", "Reset client fall distance.", true));
    private final BooleanSetting antiGlitch = setting(new BooleanSetting("Anti Glitch", "Extra checks to prevent damage from position desync.", true));

    private int groundTicks = 0;

    public NoFall() {
        super("NoFall", "Reduces or prevents fall damage entirely.");
    }

    @Override
    protected void onMovementTick(MinecraftClient client) {
        if (client.player == null) return;

        if (client.player.fallDistance < minFallDistance.get()) {
            groundTicks = 0;
            return;
        }

        if (mode.is("Motion")) {
            client.player.setVelocity(client.player.getVelocity().x,
                    Math.max(client.player.getVelocity().y, -0.1),
                    client.player.getVelocity().z);
        } else if (mode.is("Ground")) {
            client.player.setOnGround(true);
            // Send periodic ground packets to stay synced
            if (client.player.networkHandler != null && groundTicks++ % 3 == 0) {
                client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, client.player.horizontalCollision));
            }
        } else if (client.player.networkHandler != null && ready(packetDelay.get(), packetJitter.get())) {
            client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, client.player.horizontalCollision));
        }

        if (resetFallDistance.enabled()) {
            client.player.fallDistance = 0.0f;
        }

        if (antiGlitch.enabled() && client.player.isOnGround()) {
            groundTicks = 0;
            client.player.fallDistance = 0.0f;
        }
    }
}
