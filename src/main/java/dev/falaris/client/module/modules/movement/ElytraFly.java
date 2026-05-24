package dev.falaris.client.module.modules.movement;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.math.Vec3d;

public final class ElytraFly extends MovementModule {
    private final DoubleSetting speed = setting(new DoubleSetting("Speed", "Forward elytra flight speed.", 1.6, 0.1, 6.0));
    private final DoubleSetting verticalSpeed = setting(new DoubleSetting("Vertical Speed", "Manual climb/drop speed.", 0.6, 0.05, 3.0));
    private final DoubleSetting glide = setting(new DoubleSetting("Glide", "Downward glide when no vertical key is held.", -0.02, -0.2, 0.2));
    private final BooleanSetting autoStart = setting(new BooleanSetting("Auto Start", "Starts fall flying when airborne.", true));
    private final IntegerSetting startDelay = setting(new IntegerSetting("Start Delay", "Ticks between start-flying packets.", 10, 1, 40));
    private final IntegerSetting startJitter = setting(new IntegerSetting("Start Jitter", "Random extra ticks for auto start.", 4, 0, 20));
    private final BooleanSetting autoMoveForward = setting(new BooleanSetting("Auto Move Forward", "Automatically move forward when no movement keys pressed.", false));

    public ElytraFly() {
        super("ElytraFly", "Controlled powered movement while using an elytra.");
    }

    @Override
    protected void onMovementTick(MinecraftClient client) {
        if (client.player == null) return;

        if (autoStart.enabled() && !client.player.isOnGround() && !client.player.isGliding() && ready(startDelay.get(), startJitter.get())) {
            client.player.networkHandler.sendPacket(new ClientCommandC2SPacket(client.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        }

        if (!client.player.isGliding()) return;

        Vec3d look = client.player.getRotationVector().normalize();
        double y = glide.get();
        if (client.options.jumpKey.isPressed()) {
            y = verticalSpeed.get();
        } else if (client.options.sneakKey.isPressed()) {
            y = -verticalSpeed.get();
        }

        Vec3d input = MovementUtil.inputVelocity(client.player, speed.get(), false, client);
        if (input.horizontalLengthSquared() <= 0.0001) {
            if (autoMoveForward.enabled()) {
                input = new Vec3d(look.x * speed.get(), 0.0, look.z * speed.get());
            } else {
                input = Vec3d.ZERO;
            }
        }

        client.player.setVelocity(input.x, y, input.z);
        client.player.fallDistance = 0.0f;
    }
}
