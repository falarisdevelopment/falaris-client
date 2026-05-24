package dev.falaris.client.module.modules.misc;

import com.mojang.authlib.GameProfile;
import dev.falaris.client.event.EventPriority;
import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public final class Freecam extends MiscModule {
    private final DoubleSetting speed = setting(new DoubleSetting("Speed", "Freecam horizontal speed.", 0.75, 0.05, 5.0));
    private final DoubleSetting verticalSpeed = setting(new DoubleSetting("Vertical Speed", "Freecam climb/drop speed.", 0.55, 0.05, 3.0));
    private final BooleanSetting freezePlayer = setting(new BooleanSetting("Freeze Player", "Keep the real player motionless while freecam is active.", true));
    private final BooleanSetting copyRotation = setting(new BooleanSetting("Copy Rotation", "Start with the player's current yaw and pitch.", true));

    private OtherClientPlayerEntity camera;
    private Entity previousCamera;

    public Freecam() {
        super("Freecam", "Detaches the camera into a client-only movable viewpoint.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        previousCamera = client.getCameraEntity();
        camera = new OtherClientPlayerEntity(client.world, new GameProfile(UUID.randomUUID(), "FalarisFreecam"));
        camera.copyPositionAndRotation(client.player);
        if (!copyRotation.enabled()) {
            camera.setYaw(0.0f);
            camera.setPitch(0.0f);
        }
        camera.noClip = true;
        client.setCameraEntity(camera);

        track(eventBus().subscribe(dev.falaris.client.event.events.RenderWorldEvent.class, EventPriority.NORMAL, event -> {
            if (camera != null && client.player != null) {
                camera.setYaw(client.player.getYaw());
                camera.setPitch(client.player.getPitch());
                camera.noClip = true;
            }
        }));
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.setCameraEntity(previousCamera == null ? client.player : previousCamera);
        }
        camera = null;
        previousCamera = null;
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.player == null || camera == null) {
            return;
        }

        camera.lastX = camera.getX();
        camera.lastY = camera.getY();
        camera.lastZ = camera.getZ();
        camera.lastYaw = camera.getYaw();
        camera.lastPitch = camera.getPitch();

        Vec3d velocity = inputVelocity(client);
        camera.setVelocity(velocity);
        camera.move(MovementType.SELF, velocity);
        camera.noClip = true;

        if (freezePlayer.enabled()) {
            client.player.setVelocity(Vec3d.ZERO);
            client.player.setOnGround(true);
        }
    }

    private Vec3d inputVelocity(MinecraftClient client) {
        double forward = client.options.forwardKey.isPressed() ? 1.0 : 0.0;
        forward -= client.options.backKey.isPressed() ? 1.0 : 0.0;
        double strafe = client.options.rightKey.isPressed() ? 1.0 : 0.0;
        strafe -= client.options.leftKey.isPressed() ? 1.0 : 0.0;
        double y = 0.0;
        y += client.options.jumpKey.isPressed() ? verticalSpeed.get() : 0.0;
        y -= client.options.sneakKey.isPressed() ? verticalSpeed.get() : 0.0;

        if (forward == 0.0 && strafe == 0.0) {
            return new Vec3d(0.0, y, 0.0);
        }

        double length = Math.sqrt(forward * forward + strafe * strafe);
        forward /= length;
        strafe /= length;
        double yaw = Math.toRadians(camera.getYaw());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        return new Vec3d((strafe * cos - forward * sin) * speed.get(), y, (forward * cos + strafe * sin) * speed.get());
    }
}
