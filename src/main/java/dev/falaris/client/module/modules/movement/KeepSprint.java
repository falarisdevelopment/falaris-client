package dev.falaris.client.module.modules.movement;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import net.minecraft.client.MinecraftClient;

public final class KeepSprint extends MovementModule {
    private final BooleanSetting cancelVelocity = setting(new BooleanSetting("Cancel Velocity", "Cancel knockback when sprinting.", true));
    private final DoubleSetting motionMultiplier = setting(new DoubleSetting("Motion Multiplier", "Keep sprint momentum multiplier.", 0.8, 0.0, 1.0));

    public KeepSprint() {
        super("KeepSprint", "Prevents sprint from being cancelled after attacking.");
    }

    @Override
    protected void onMovementTick(MinecraftClient client) {
        if (client.player == null) return;

        if (!client.player.isSprinting() && client.player.getVelocity().horizontalLengthSquared() > 0.001) {
            client.player.setSprinting(true);
        }

        if (cancelVelocity.enabled() && client.player.hurtTime > 0 && client.player.isSprinting()) {
            var vel = client.player.getVelocity();
            client.player.setVelocity(vel.x * motionMultiplier.get(), vel.y, vel.z * motionMultiplier.get());
        }
    }
}
