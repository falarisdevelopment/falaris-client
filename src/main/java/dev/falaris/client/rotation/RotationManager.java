package dev.falaris.client.rotation;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;

import java.util.Optional;
import java.util.Random;

public final class RotationManager {
    public enum Easing { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT }

    private Rotation target;
    private int totalTicks;
    private int remainingTicks;
    private float startYaw, startPitch;
    private float maxStep = 12.0f;
    private Easing easing = Easing.EASE_IN_OUT;
    private final Random random = new Random();

    private Rotation serverTarget;
    private int serverTicks;
    private boolean hasServerRotation;

    public void rotateTo(float yaw, float pitch, int ticks) {
        applyTarget(yaw, pitch, ticks);
    }

    public void rotateToSilent(float yaw, float pitch, int ticks) {
        applyTarget(yaw, pitch, ticks);
    }

    private void applyTarget(float yaw, float pitch, int ticks) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) { target = null; return; }
        this.startYaw = player.getYaw();
        this.startPitch = player.getPitch();
        this.target = new Rotation(yaw, MathHelper.clamp(pitch, -90.0f, 90.0f));
        this.totalTicks = Math.max(1, ticks);
        this.remainingTicks = this.totalTicks;
    }

    public void setServerRotation(float yaw, float pitch, int ticks) {
        this.serverTarget = new Rotation(yaw, MathHelper.clamp(pitch, -90.0f, 90.0f));
        this.serverTicks = Math.max(1, ticks);
        this.hasServerRotation = true;
    }

    public void clearServerRotation() {
        this.hasServerRotation = false;
        this.serverTarget = null;
        this.serverTicks = 0;
    }

    public boolean hasServerRotation() {
        return hasServerRotation;
    }

    public Optional<Rotation> getServerRotation() {
        return Optional.ofNullable(serverTarget);
    }

    public void setEasing(Easing easing) {
        this.easing = easing;
    }

    public void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        if (target != null) {
            float t = remainingTicks / (float) totalTicks;
            float eased = ease(1.0f - t);

            float curYaw = startYaw + MathHelper.wrapDegrees(target.yaw() - startYaw) * eased;
            float curPitch = startPitch + (target.pitch() - startPitch) * eased;

            float yawDelta = MathHelper.wrapDegrees(target.yaw() - player.getYaw());
            float pitchDelta = target.pitch() - player.getPitch();
            float dist = MathHelper.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);

            float jitter = 0.1f + random.nextFloat() * 0.3f;
            float yawStep = MathHelper.clamp(yawDelta / Math.max(1, remainingTicks), -maxStep, maxStep);
            float pitchStep = MathHelper.clamp(pitchDelta / Math.max(1, remainingTicks), -maxStep, maxStep);

            if (dist < 1.0f) {
                yawStep += (random.nextFloat() - 0.5f) * jitter * 0.3f;
                pitchStep += (random.nextFloat() - 0.5f) * jitter * 0.3f;
            }

            player.setYaw(player.getYaw() + yawStep);
            player.setPitch(MathHelper.clamp(player.getPitch() + pitchStep, -90.0f, 90.0f));

            remainingTicks--;
            if (remainingTicks <= 0) target = null;
        }

        if (hasServerRotation && serverTarget != null && client.getNetworkHandler() != null) {
            if (serverTicks > 0) {
                client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    serverTarget.yaw(), serverTarget.pitch(),
                    player.isOnGround(), player.horizontalCollision
                ));
                serverTicks--;
                if (serverTicks <= 0) {
                    hasServerRotation = false;
                    serverTarget = null;
                }
            }
        }
    }

    private float ease(float t) {
        return switch (easing) {
            case LINEAR -> t;
            case EASE_IN -> t * t;
            case EASE_OUT -> t * (2.0f - t);
            case EASE_IN_OUT -> t < 0.5f ? 2.0f * t * t : -1.0f + (4.0f - 2.0f * t) * t;
        };
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
