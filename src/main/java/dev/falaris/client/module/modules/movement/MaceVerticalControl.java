package dev.falaris.client.module.modules.movement;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public final class MaceVerticalControl extends MovementModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Vertical control method.", "Boost", "Boost", "Keep", "Strafing"));
    private final DoubleSetting strength = setting(new DoubleSetting("Strength", "Downward velocity strength.", 0.3, 0.05, 1.0));
    private final DoubleSetting minFallDist = setting(new DoubleSetting("Min Fall Dist", "Minimum fall distance to activate.", 2.0, 0.5, 8.0));
    private final BooleanSetting requireMace = setting(new BooleanSetting("Require Mace", "Only control vertical while holding mace.", true));
    private final BooleanSetting airControl = setting(new BooleanSetting("Air Control", "Allow horizontal movement control in air.", true));
    private final DoubleSetting airSpeed = setting(new DoubleSetting("Air Speed", "Horizontal air control factor.", 0.8, 0.1, 1.0));
    private final BooleanSetting autoSlam = setting(new BooleanSetting("Auto Slam", "Auto-slam down when above target.", false));
    private final DoubleSetting slamRange = setting(new DoubleSetting("Slam Range", "Range to detect targets below for slam.", 6.0, 2.0, 12.0));

    public MaceVerticalControl() {
        super("MaceVerticalControl", "Controls vertical movement for optimal mace slam combos.");
    }

    @Override
    protected void onMovementTick(MinecraftClient client) {
        if (client.player == null) return;

        if (requireMace.enabled() && !client.player.getMainHandStack().isOf(Items.MACE)) return;
        if (client.player.isOnGround()) return;
        if (client.player.fallDistance < minFallDist.get()) return;

        if (mode.is("Boost")) {
            if (client.player.getVelocity().y > -strength.get()) {
                client.player.setVelocity(
                    client.player.getVelocity().x,
                    -strength.get(),
                    client.player.getVelocity().z
                );
            }
        } else if (mode.is("Keep")) {
            double currentVy = client.player.getVelocity().y;
            if (currentVy > -0.1) {
                client.player.setVelocity(
                    client.player.getVelocity().x,
                    Math.min(currentVy, -0.1),
                    client.player.getVelocity().z
                );
            }
        } else if (mode.is("Strafing")) {
            if (client.player.getVelocity().y > -strength.get() * 0.7) {
                client.player.setVelocity(
                    client.player.getVelocity().x * airSpeed.get(),
                    -strength.get() * 0.7,
                    client.player.getVelocity().z * airSpeed.get()
                );
            }
            if (airControl.enabled() && !client.player.isOnGround()) {
                double forward = client.options.forwardKey.isPressed() ? 1.0 : 0.0;
                forward -= client.options.backKey.isPressed() ? 1.0 : 0.0;
                double sideways = client.options.leftKey.isPressed() ? 1.0 : 0.0;
                sideways -= client.options.rightKey.isPressed() ? 1.0 : 0.0;
                if (forward != 0 || sideways != 0) {
                    double speed = airSpeed.get() * 0.3;
                    double length = Math.sqrt(forward * forward + sideways * sideways);
                    forward /= length;
                    sideways /= length;
                    double sin = Math.sin(Math.toRadians(client.player.getYaw()));
                    double cos = Math.cos(Math.toRadians(client.player.getYaw()));
                    double mx = (sideways * cos - forward * sin) * speed;
                    double mz = (forward * cos + sideways * sin) * speed;
                    client.player.setVelocity(mx, client.player.getVelocity().y, mz);
                }
            }
        }

        if (autoSlam.enabled()) {
            client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                client.player.getX(),
                client.player.getY() - 0.2,
                client.player.getZ(),
                false,
                client.player.horizontalCollision
            ));
        }
    }
}
