package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
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
    private final ModeSetting bypass = setting(new ModeSetting("Bypass", "Anticheat bypass method.", "Vanilla", "Vanilla", "Legit", "Grim", "Grim2344", "Grim2371", "Vulcan", "AAC"));

    private final Random random = new Random();
    private int velocityCancelTicks;
    private int knockbackTick;
    private boolean wasHurt;

    // Grim2344 (C06) state
    private boolean grimC06Pending;
    private int grimC06Ticks;

    // Grim2371 (Block-place) state
    private boolean grim2371Pending;
    private int grim2371Ticks;

    public Velocity() {
        super("Velocity", "Modifies incoming knockback velocity.");
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) return;
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
        boolean justGotHit = isHurt && !wasHurt;

        if (isHurt) knockbackTick++;
        wasHurt = isHurt;

        String b = bypass.get();

        // --- Grim2344: C06 bypass ---
        if (b.equals("Grim2344")) {
            handleGrim2344(client, justGotHit, hasVelocity);
            return;
        }

        // --- Grim2371: Block-place bypass ---
        if (b.equals("Grim2371")) {
            handleGrim2371(client, justGotHit, hasVelocity);
            return;
        }

        if (!isHurt || !hasVelocity) return;
        if (!applySelf.enabled() && client.player.isInsideWall()) return;
        if (velocityCancelTicks <= 0 && cancelTicks.get() > 0) {
            velocityCancelTicks = cancelTicks.get();
        }

        Vec3d vel = client.player.getVelocity();
        double hMul = horizontal.get();
        double vMul = vertical.get();

        if (b.equals("Grim") || b.equals("Legit")) {
            double jitter = b.equals("Grim") ? 0.10 : 0.05;
            double fadeH, fadeV;
            if (knockbackTick <= 1) {
                fadeH = hMul * 0.5 + 0.5;
                fadeV = vMul * 0.5 + 0.5;
            } else {
                fadeH = hMul;
                fadeV = vMul;
            }
            double h = fadeH * (1.0 - jitter + random.nextDouble() * jitter * 2.0);
            double v = fadeV * (1.0 - jitter + random.nextDouble() * jitter * 2.0);
            client.player.setVelocity(vel.x * h, vel.y * v, vel.z * h);
            return;
        }

        Vec3d nv = vel;
        if (mode.is("Cancel")) {
            nv = Vec3d.ZERO;
        } else if (mode.is("Reduce")) {
            nv = new Vec3d(vel.x * hMul, vel.y * vMul, vel.z * hMul);
        } else if (mode.is("Override")) {
            nv = new Vec3d(vel.x * Math.max(0.05, hMul), vel.y * Math.max(0.05, vMul), vel.z * Math.max(0.05, hMul));
        } else if (mode.is("Push Only") && Math.abs(vel.y) < 0.1) {
            nv = new Vec3d(vel.x * hMul, vel.y, vel.z * hMul);
        }
        client.player.setVelocity(nv);
    }

    private void handleGrim2344(MinecraftClient client, boolean justGotHit, boolean hasVelocity) {
        if (justGotHit && hasVelocity) {
            client.player.setVelocity(0, 0, 0);
            grimC06Pending = true;
            grimC06Ticks = 0;
        }
        if (grimC06Pending) {
            grimC06Ticks++;
            if (grimC06Ticks >= 1 && client.getNetworkHandler() != null) {
                for (int i = 0; i < 4; i++) {
                    client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        client.player.getX(), client.player.getY(), client.player.getZ(),
                        client.player.isOnGround(), client.player.horizontalCollision
                    ));
                }
                BlockPos bp = client.player.getBlockPos();
                Direction dir = client.player.getHorizontalFacing().getOpposite();
                client.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, bp, dir
                ));
                grimC06Pending = false;
            }
        }
    }

    private void handleGrim2371(MinecraftClient client, boolean justGotHit, boolean hasVelocity) {
        if (justGotHit && hasVelocity) {
            client.player.setVelocity(0, 0, 0);
            grim2371Pending = true;
            grim2371Ticks = 0;
        }
        if (grim2371Pending) {
            grim2371Ticks++;
            if (grim2371Ticks >= 2 && client.getNetworkHandler() != null) {
                var hitResult = client.crosshairTarget;
                if (hitResult instanceof net.minecraft.util.hit.BlockHitResult bhr) {
                    var result = client.interactionManager.interactBlock(client.player, net.minecraft.util.Hand.MAIN_HAND, bhr);
                    if (result.isAccepted()) {
                        client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                    }
                }
                client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    client.player.getYaw(), 90f,
                    client.player.isOnGround(), client.player.horizontalCollision
                ));
                grim2371Pending = false;
            }
        }
    }
}
