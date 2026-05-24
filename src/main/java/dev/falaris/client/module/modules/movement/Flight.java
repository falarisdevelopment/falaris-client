package dev.falaris.client.module.modules.movement;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public final class Flight extends MovementModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Flight implementation.", "Normal", "Normal", "Packet", "Creative", "Legit"));
    private final DoubleSetting speed = setting(new DoubleSetting("Speed", "Horizontal flight speed.", 0.75, 0.05, 5.0));
    private final DoubleSetting verticalSpeed = setting(new DoubleSetting("Vertical Speed", "Vertical flight speed.", 0.55, 0.05, 3.0));
    private final IntegerSetting packetInterval = setting(new IntegerSetting("Packet Interval", "Ticks between packet nudges.", 2, 1, 20));
    private final IntegerSetting packetJitter = setting(new IntegerSetting("Packet Jitter", "Random extra ticks for packet mode.", 1, 0, 8));
    private final BooleanSetting antiKick = setting(new BooleanSetting("Anti Kick", "Adds a small downward pulse while hovering.", true));
    private final DoubleSetting acceleration = setting(new DoubleSetting("Acceleration", "Speed ramp per tick (0 = instant).", 0.15, 0.0, 1.0));
    private final BooleanSetting glide = setting(new BooleanSetting("Glide", "Gentle downward drift when idle.", true));

    private boolean previousAllowFlying;
    private boolean previousFlying;
    private double currentSpeedX, currentSpeedZ;

    public Flight() {
        super("Flight", "Free movement through the air with normal, packet, creative, and legit modes.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            previousAllowFlying = client.player.getAbilities().allowFlying;
            previousFlying = client.player.getAbilities().flying;
        }
        currentSpeedX = 0;
        currentSpeedZ = 0;
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
        if (client.player == null) return;

        if (mode.is("Creative")) {
            client.player.getAbilities().allowFlying = true;
            client.player.getAbilities().flying = true;
            client.player.getAbilities().setFlySpeed((float) (speed.get() / 10.0));
            client.player.sendAbilitiesUpdate();
            return;
        }

        Vec3d input = MovementUtil.inputVelocity(client.player, speed.get(), false, client);
        double targetX = input.x;
        double targetZ = input.z;

        if (acceleration.get() > 0.01) {
            currentSpeedX += (targetX - currentSpeedX) * acceleration.get();
            currentSpeedZ += (targetZ - currentSpeedZ) * acceleration.get();
            if (Math.abs(currentSpeedX) < 0.001 && Math.abs(targetX) < 0.001) currentSpeedX = 0;
            if (Math.abs(currentSpeedZ) < 0.001 && Math.abs(targetZ) < 0.001) currentSpeedZ = 0;
        } else {
            currentSpeedX = targetX;
            currentSpeedZ = targetZ;
        }

        double y = 0;
        if (client.options.jumpKey.isPressed()) {
            y = verticalSpeed.get();
        } else if (client.options.sneakKey.isPressed()) {
            y = -verticalSpeed.get();
        } else if (glide.enabled() && input.horizontalLengthSquared() < 0.01 && client.player.getVelocity().y > -0.1) {
            y = -0.01;
        }

        if (antiKick.enabled() && y == 0 && client.player.age % 20 == 0) {
            y = -0.04;
        }

        if (mode.is("Legit")) {
            client.player.getAbilities().allowFlying = true;
            client.player.getAbilities().flying = true;
            client.player.getAbilities().setFlySpeed((float) Math.min(speed.get() / 15.0, 0.1));
            client.player.sendAbilitiesUpdate();
        } else {
            client.player.setVelocity(currentSpeedX, y, currentSpeedZ);
            client.player.fallDistance = 0;

            if (mode.is("Packet") && client.player.networkHandler != null && ready(packetInterval.get(), packetJitter.get())) {
                Vec3d pos = new Vec3d(client.player.getX() + currentSpeedX, client.player.getY() + y, client.player.getZ() + currentSpeedZ);
                client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(pos.x, pos.y, pos.z, true, client.player.horizontalCollision));
            }
        }
    }
}
