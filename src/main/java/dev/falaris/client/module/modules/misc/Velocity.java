package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public final class Velocity extends MiscModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Velocity reduction mode.", "Cancel", "Cancel", "Override", "Push Only", "Reduce"));
    private final DoubleSetting horizontal = setting(new DoubleSetting("Horizontal", "Horizontal velocity multiplier.", 0.0, 0.0, 1.0));
    private final DoubleSetting vertical = setting(new DoubleSetting("Vertical", "Vertical velocity multiplier.", 0.0, 0.0, 1.0));
    private final BooleanSetting applySelf = setting(new BooleanSetting("Apply Self", "Also affect self-explosion knockback.", false));
    private final BooleanSetting pushImmunity = setting(new BooleanSetting("Push Immunity", "Cancel entity push.", true));
    private final BooleanSetting waterImmunity = setting(new BooleanSetting("Water Immunity", "Cancel water push.", false));
    private final BooleanSetting explosionImmunity = setting(new BooleanSetting("Explosion Immunity", "Cancel explosion knockback.", false));
    private final IntegerSetting cancelTicks = setting(new IntegerSetting("Cancel Ticks", "Ticks to cancel velocity after hit.", 5, 0, 20));
    private final ModeSetting bypass = setting(new ModeSetting("Bypass", "Anticheat bypass method.", "Vanilla", "Vanilla", "Legit", "Grim", "Vulcan", "AAC"));

    private final Random random = new Random();
    private int velocityCancelTicks;

    public Velocity() {
        super("Velocity", "Modifies incoming knockback velocity.");
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.player == null) return;
        if (velocityCancelTicks > 0) velocityCancelTicks--;

        if (pushImmunity.enabled()) {
            client.player.setVelocity(client.player.getVelocity().multiply(1.0, 0.0, 1.0));
        }

        if (waterImmunity.enabled() && client.player.isTouchingWater()) {
            Vec3d vel = client.player.getVelocity();
            client.player.setVelocity(vel.x * 0.8, vel.y, vel.z * 0.8);
        }

        boolean isHurt = client.player.hurtTime > 0;
        boolean hasVelocity = client.player.getVelocity().lengthSquared() > 0.005;

        if (!isHurt || !hasVelocity) return;

        if (!applySelf.enabled() && client.player.isInsideWall()) return;

        if (velocityCancelTicks <= 0 && cancelTicks.get() > 0) {
            velocityCancelTicks = cancelTicks.get();
        }

        String b = bypass.get();
        Vec3d vel = client.player.getVelocity();

        if (b.equals("Grim")) {
            double h = 0.75 + random.nextDouble() * 0.20;
            double v = 0.75 + random.nextDouble() * 0.20;
            client.player.setVelocity(vel.x * h, vel.y * v, vel.z * h);
            return;
        }

        if (b.equals("Legit")) {
            double h = 0.85 + random.nextDouble() * 0.10;
            double v = 0.85 + random.nextDouble() * 0.10;
            client.player.setVelocity(vel.x * h, vel.y * v, vel.z * h);
            return;
        }

        if (mode.is("Cancel")) {
            client.player.setVelocity(0, 0, 0);
        } else if (mode.is("Reduce")) {
            client.player.setVelocity(
                    vel.x * horizontal.get(),
                    vel.y * vertical.get(),
                    vel.z * horizontal.get()
            );
        } else if (mode.is("Override")) {
            client.player.setVelocity(
                    vel.x * Math.max(0.05, horizontal.get()),
                    vel.y * Math.max(0.05, vertical.get()),
                    vel.z * Math.max(0.05, horizontal.get())
            );
        } else if (mode.is("Push Only")) {
            if (Math.abs(vel.y) < 0.1) {
                client.player.setVelocity(vel.x * horizontal.get(), vel.y, vel.z * horizontal.get());
            }
        }
    }
}
