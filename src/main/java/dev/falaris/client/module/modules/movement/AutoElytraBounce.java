package dev.falaris.client.module.modules.movement;

import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;

public final class AutoElytraBounce extends MovementModule {
    private final DoubleSetting boostVertical = setting(new DoubleSetting("Boost Vertical", "Upward velocity when bouncing.", 1.5, 0.5, 4.0));
    private final DoubleSetting boostHorizontal = setting(new DoubleSetting("Boost Horizontal", "Forward velocity when bouncing.", 1.0, 0.0, 3.0));
    private final IntegerSetting minHeight = setting(new IntegerSetting("Min Height", "Minimum Y level to bounce.", 60, 0, 256));

    private boolean wasBouncing;

    public AutoElytraBounce() {
        super("AutoElytraBounce", "Automatically bounces upward when flying with elytra near the ground.");
    }

    @Override
    protected void onMovementTick(MinecraftClient client) {
        if (client.player == null) return;

        if (client.player.getAbilities().flying) {
            wasBouncing = false;
            return;
        }

        if (client.player.getY() > minHeight.get()) return;

        if (client.player.horizontalCollision || client.player.verticalCollision) {
            if (!wasBouncing) {
                client.player.setVelocity(
                    client.player.getVelocity().x * 0.5 + client.player.getRotationVector().x * boostHorizontal.get(),
                    boostVertical.get(),
                    client.player.getVelocity().z * 0.5 + client.player.getRotationVector().z * boostHorizontal.get()
                );
                wasBouncing = true;
            }
        } else {
            wasBouncing = false;
        }
    }
}
