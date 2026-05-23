package dev.falaris.client.module.modules.movement;

import dev.falaris.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;

public final class AutoSprint extends MovementModule {
    private final BooleanSetting omnidirectional = setting(new BooleanSetting("Omnidirectional", "Sprint while moving in any direction.", false));
    private final BooleanSetting requireForward = setting(new BooleanSetting("Require Forward", "Only sprint when forward is held.", true));
    private final BooleanSetting stopInWater = setting(new BooleanSetting("Stop In Water", "Avoid sprinting while submerged.", false));

    public AutoSprint() {
        super("AutoSprint", "Automatically toggles sprint while moving.");
    }

    @Override
    protected void onMovementTick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        if (stopInWater.enabled() && client.player.isSubmergedInWater()) {
            client.player.setSprinting(false);
            return;
        }

        boolean movingForward = client.options.forwardKey.isPressed();
        boolean movingAny = movingForward || client.options.backKey.isPressed() || client.options.leftKey.isPressed() || client.options.rightKey.isPressed();
        boolean shouldSprint = omnidirectional.enabled() ? movingAny : movingForward;
        if (requireForward.enabled()) {
            shouldSprint = movingForward;
        }

        client.player.setSprinting(shouldSprint && !client.player.isSneaking() && !client.player.horizontalCollision);
    }
}
