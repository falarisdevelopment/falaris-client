package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.math.Vec3d;

public final class InvWalk extends PlayerModule {
    private final BooleanSetting onlyInventory = setting(new BooleanSetting("Only Inventory", "Only move in inventory screens, not chat.", true));
    private final BooleanSetting allowSprint = setting(new BooleanSetting("Allow Sprint", "Allow sprinting while in inventory.", true));
    private final DoubleSetting speedMultiplier = setting(new DoubleSetting("Speed Multiplier", "Movement speed while in screens.", 0.6, 0.1, 1.0));

    public InvWalk() {
        super("InvWalk", "Walk around while your inventory or containers are open.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null) return;
        if (client.currentScreen == null) return;
        if (onlyInventory.enabled() && !(client.currentScreen instanceof HandledScreen)) return;
        if (client.currentScreen instanceof ChatScreen) return;

        simulateMovement(client.player, client);
    }

    private void simulateMovement(ClientPlayerEntity player, MinecraftClient client) {
        double forward = 0, sideways = 0;
        if (client.options.forwardKey.isPressed()) forward++;
        if (client.options.backKey.isPressed()) forward--;
        if (client.options.leftKey.isPressed()) sideways++;
        if (client.options.rightKey.isPressed()) sideways--;

        if (forward == 0 && sideways == 0) return;

        float yaw = player.getYaw();
        double speed = 0.1 * speedMultiplier.get();

        double sx = -Math.sin(Math.toRadians(yaw)) * forward + Math.cos(Math.toRadians(yaw)) * sideways;
        double sz = Math.cos(Math.toRadians(yaw)) * forward + Math.sin(Math.toRadians(yaw)) * sideways;

        Vec3d vel = player.getVelocity();
        player.setVelocity(sx * speed, vel.y, sz * speed);

        if (allowSprint.enabled() && forward > 0) {
            player.setSprinting(true);
        }
    }
}
