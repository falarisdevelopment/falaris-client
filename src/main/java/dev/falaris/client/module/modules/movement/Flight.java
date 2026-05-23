package dev.falaris.client.module.modules.movement;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public final class Flight extends MovementModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Flight implementation.", "Normal", "Normal", "Packet", "Creative"));
    private final DoubleSetting speed = setting(new DoubleSetting("Speed", "Horizontal flight speed.", 0.75, 0.05, 5.0));
    private final DoubleSetting verticalSpeed = setting(new DoubleSetting("Vertical Speed", "Vertical flight speed.", 0.55, 0.05, 3.0));
    private final IntegerSetting packetInterval = setting(new IntegerSetting("Packet Interval", "Ticks between packet nudges.", 2, 1, 20));
    private final IntegerSetting packetJitter = setting(new IntegerSetting("Packet Jitter", "Random extra ticks for packet mode.", 1, 0, 8));
    private final BooleanSetting antiKick = setting(new BooleanSetting("Anti Kick", "Adds a small downward pulse while hovering.", true));

    private boolean previousAllowFlying;
    private boolean previousFlying;

    public Flight() {
        super("Flight", "Free movement through the air with normal, packet, and creative modes.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            previousAllowFlying = client.player.getAbilities().allowFlying;
            previousFlying = client.player.getAbilities().flying;
        }
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && mode.is("Creative")) {
            client.player.getAbilities().allowFlying = previousAllowFlying;
            client.player.getAbilities().flying = previousFlying;
            client.player.sendAbilitiesUpdate();
        }
    }

    @Override
    protected void onMovementTick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        if (mode.is("Creative")) {
            client.player.getAbilities().allowFlying = true;
            client.player.getAbilities().flying = true;
            client.player.getAbilities().setFlySpeed((float) (speed.get() / 10.0));
            client.player.sendAbilitiesUpdate();
            return;
        }

        Vec3d velocity = MovementUtil.inputVelocity(client.player, speed.get(), false, client);
        double y = 0.0;
        if (client.options.jumpKey.isPressed()) {
            y += verticalSpeed.get();
        }
        if (client.options.sneakKey.isPressed()) {
            y -= verticalSpeed.get();
        }
        if (antiKick.enabled() && y == 0.0 && client.player.age % 20 == 0) {
            y = -0.04;
        }

        client.player.setVelocity(velocity.x, y, velocity.z);
        client.player.fallDistance = 0.0f;

        if (mode.is("Packet") && client.player.networkHandler != null && ready(packetInterval.get(), packetJitter.get())) {
            Vec3d pos = new net.minecraft.util.math.Vec3d(client.player.getX(), client.player.getY(), client.player.getZ()).add(velocity.x, y, velocity.z);
            client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true, client.player.horizontalCollision));
        }
    }
}
