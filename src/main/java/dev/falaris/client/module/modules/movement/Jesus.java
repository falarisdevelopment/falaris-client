package dev.falaris.client.module.modules.movement;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class Jesus extends MovementModule {
    private final DoubleSetting surfaceLift = setting(new DoubleSetting("Surface Lift", "Upward velocity while in liquid.", 0.08, 0.01, 0.5));
    private final DoubleSetting surfaceSpeed = setting(new DoubleSetting("Surface Speed", "Horizontal speed on liquid.", 0.28, 0.05, 1.5));
    private final BooleanSetting water = setting(new BooleanSetting("Water", "Affect water.", true));
    private final BooleanSetting lava = setting(new BooleanSetting("Lava", "Affect lava.", false));
    private final BooleanSetting allowSneak = setting(new BooleanSetting("Allow Sneak", "Sneaking lets you sink.", true));

    public Jesus() {
        super("Jesus", "Helps the player stay on liquid surfaces.");
    }

    @Override
    protected void onMovementTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        if (allowSneak.enabled() && client.options.sneakKey.isPressed()) {
            return;
        }

        BlockPos pos = client.player.getBlockPos();
        boolean liquid = water.enabled() && client.world.getFluidState(pos).isOf(Fluids.WATER);
        liquid = liquid || lava.enabled() && client.world.getFluidState(pos).isOf(Fluids.LAVA);
        if (!liquid) {
            return;
        }

        Vec3d input = MovementUtil.inputVelocity(client.player, surfaceSpeed.get(), false, client);
        client.player.setVelocity(input.x, surfaceLift.get(), input.z);
        client.player.fallDistance = 0.0f;
    }
}
