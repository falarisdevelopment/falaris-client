package dev.falaris.client.module.modules.movement;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Vec3d;

public final class BoatFly extends MovementModule {
    private final DoubleSetting speed = setting(new DoubleSetting("Speed", "Horizontal boat speed.", 0.9, 0.05, 5.0));
    private final DoubleSetting verticalSpeed = setting(new DoubleSetting("Vertical Speed", "Boat climb/drop speed.", 0.45, 0.05, 3.0));
    private final BooleanSetting onlyBoats = setting(new BooleanSetting("Only Boats", "Only affect boat entities.", true));
    private final BooleanSetting noGravity = setting(new BooleanSetting("No Gravity", "Disables vehicle gravity client-side.", true));

    public BoatFly() {
        super("BoatFly", "Lets mounted boats move freely through the air.");
    }

    @Override
    protected void onMovementTick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        Entity vehicle = client.player.getVehicle();
        if (vehicle == null || (onlyBoats.enabled() && !(vehicle instanceof BoatEntity))) {
            return;
        }

        Vec3d input = MovementUtil.inputVelocity(client.player, speed.get(), false, client);
        double y = 0.0;
        if (client.options.jumpKey.isPressed()) {
            y += verticalSpeed.get();
        }
        if (client.options.sneakKey.isPressed()) {
            y -= verticalSpeed.get();
        }

        vehicle.setVelocity(input.x, y, input.z);
        vehicle.setNoGravity(noGravity.enabled());
        vehicle.fallDistance = 0.0f;
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.getVehicle() != null) {
            client.player.getVehicle().setNoGravity(false);
        }
    }
}
