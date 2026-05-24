package dev.falaris.client.module.modules.movement;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public final class Speed extends MovementModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Speed mode.", "Hop", "Hop", "Strafe", "Ground", "Legit"));
    private final DoubleSetting speed = setting(new DoubleSetting("Speed", "Movement speed multiplier.", 1.5, 1.0, 5.0));
    private final DoubleSetting acceleration = setting(new DoubleSetting("Acceleration", "Smooth speed ramp (0 = instant).", 0.25, 0.0, 1.0));
    private final BooleanSetting autoJump = setting(new BooleanSetting("Auto Jump", "Auto-jump to maintain hop speed.", true));
    private final DoubleSetting jumpHeight = setting(new DoubleSetting("Jump Height", "Extra jump motion for hop mode.", 0.42, 0.3, 0.6));
    private final ModeSetting bypass = setting(new ModeSetting("Bypass", "Anticheat bypass.", "Vanilla", "Vanilla", "Grim", "Vulcan", "NCP"));

    private final Random random = new Random();
    private double currentSpeed;
    private int hopTicks;

    public Speed() {
        super("Speed", "Increases movement speed with hop, strafe, and ground modes.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        currentSpeed = 0;
        hopTicks = 0;
    }

    @Override
    protected void onMovementTick(MinecraftClient client) {
        if (client.player == null) return;

        Vec3d input = MovementUtil.inputVelocity(client.player, 1.0, false, client);
        boolean moving = input.horizontalLengthSquared() > 0.001;

        if (!moving) {
            currentSpeed *= 0.8;
            return;
        }

        double target = speed.get();
        if (acceleration.get() > 0.01) {
            currentSpeed += (target - currentSpeed) * acceleration.get();
        } else {
            currentSpeed = target;
        }

        String bp = bypass.get();
        double jitter = 1.0;
        if (bp.equals("Grim")) jitter = 0.95 + random.nextDouble() * 0.10;
        else if (bp.equals("Vulcan")) jitter = 0.90 + random.nextDouble() * 0.15;
        else if (bp.equals("NCP")) jitter = 0.85 + random.nextDouble() * 0.20;

        double moveSpeed = currentSpeed * jitter;
        Vec3d vel = MovementUtil.inputVelocity(client.player, moveSpeed, false, client);

        switch (mode.get()) {
            case "Hop" -> {
                if (client.player.isOnGround() && autoJump.enabled()) {
                    client.player.jump();
                    client.player.setVelocity(vel.x, jumpHeight.get(), vel.z);
                } else if (!client.player.isOnGround()) {
                    double y = client.player.getVelocity().y;
                    client.player.setVelocity(vel.x, y, vel.z);
                }
                hopTicks++;
            }
            case "Strafe" -> {
                client.player.setVelocity(vel.x, client.player.getVelocity().y, vel.z);
                if (client.player.isOnGround() && autoJump.enabled()) {
                    client.player.jump();
                }
            }
            case "Ground" -> {
                if (client.player.isOnGround()) {
                    client.player.setVelocity(vel.x, client.player.getVelocity().y, vel.z);
                }
            }
            case "Legit" -> {
                client.player.setSprinting(true);
                Vec3d v = client.player.getVelocity();
                double mult = Math.min(moveSpeed, 1.2);
                client.player.setVelocity(v.x * mult, v.y, v.z * mult);
            }
        }
    }
}
