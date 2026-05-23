package dev.falaris.client.module.modules.movement;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import net.minecraft.client.MinecraftClient;

public final class AutoWalk extends MovementModule {
    private final BooleanSetting sprint = setting(new BooleanSetting("Sprint", "Sprint while auto-walking.", true));
    private final BooleanSetting stopOnScreen = setting(new BooleanSetting("Stop On Screen", "Pause while a GUI screen is open.", true));
    private final BooleanSetting steer = setting(new BooleanSetting("Steer", "Use the current camera yaw as direction.", true));
    private final DoubleSetting speed = setting(new DoubleSetting("Speed", "Optional direct movement speed.", 0.0, 0.0, 1.5));

    public AutoWalk() {
        super("AutoWalk", "Keeps the player moving forward.");
    }

    @Override
    protected void onMovementTick(MinecraftClient client) {
        if (client.player == null || stopOnScreen.enabled() && client.currentScreen != null) {
            return;
        }

        client.options.forwardKey.setPressed(true);
        client.player.setSprinting(sprint.enabled());

        if (steer.enabled()) {
            rotations().rotateTo(client.player.getYaw(), client.player.getPitch(), 1);
        }
        if (speed.get() > 0.0) {
            MovementUtil.setHorizontalSpeed(client.player, speed.get(), client);
        }
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.options.forwardKey.setPressed(false);
    }
}
