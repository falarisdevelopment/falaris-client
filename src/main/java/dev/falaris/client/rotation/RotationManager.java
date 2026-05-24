package dev.falaris.client.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.util.Optional;
import java.util.Random;

public final class RotationManager {
    private Rotation target;
    private int remainingTicks;
    private float maxStep = 12.0f;
    private final Random random = new Random();

    public void rotateTo(float yaw, float pitch, int ticks) {
        this.target = new Rotation(yaw, MathHelper.clamp(pitch, -90.0f, 90.0f));
        this.remainingTicks = Math.max(1, ticks);
    }

    public void rotateToSilent(float yaw, float pitch, int ticks) {
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

        float jitter = 0.2f + random.nextFloat() * 0.5f;
        float yawStep = MathHelper.clamp(yawDelta / remainingTicks, -maxStep, maxStep) + (random.nextFloat() - 0.5f) * jitter;
        float pitchStep = MathHelper.clamp(pitchDelta / remainingTicks, -maxStep, maxStep) + (random.nextFloat() - 0.5f) * jitter;

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
