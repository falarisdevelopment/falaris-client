package dev.falaris.client.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.util.Optional;

public final class RotationManager {
    private Rotation target;
    private int remainingTicks;
    private float maxStep = 12.0f;

    public void rotateTo(float yaw, float pitch, int ticks) {
        this.target = new Rotation(yaw, MathHelper.clamp(pitch, -90.0f, 90.0f));
        this.remainingTicks = Math.max(1, ticks);
    }

    public void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || target == null) {
            return;
        }

        float yawDelta = MathHelper.wrapDegrees(target.yaw() - player.getYaw());
        float pitchDelta = target.pitch() - player.getPitch();
        float yawStep = MathHelper.clamp(yawDelta / remainingTicks, -maxStep, maxStep);
        float pitchStep = MathHelper.clamp(pitchDelta / remainingTicks, -maxStep, maxStep);

        player.setYaw(player.getYaw() + yawStep);
        player.setPitch(MathHelper.clamp(player.getPitch() + pitchStep, -90.0f, 90.0f));

        remainingTicks--;
        if (remainingTicks <= 0) {
            target = null;
        }
    }

    public Optional<Rotation> getTarget() {
        return Optional.ofNullable(target);
    }

    public float getMaxStep() {
        return maxStep;
    }

    public void setMaxStep(float maxStep) {
        this.maxStep = Math.max(1.0f, maxStep);
    }
}
