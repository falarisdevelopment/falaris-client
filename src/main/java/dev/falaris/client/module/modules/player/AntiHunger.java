package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import net.minecraft.client.MinecraftClient;

public final class AntiHunger extends PlayerModule {
    private final BooleanSetting stopSprint = setting(new BooleanSetting("Stop Sprint", "Disable sprinting when hunger is low.", true));
    private final BooleanSetting stopJump = setting(new BooleanSetting("Stop Jump", "Release jump when hunger is low.", true));
    private final BooleanSetting stopSwimming = setting(new BooleanSetting("Stop Swimming", "Disable sprint-swimming when hunger is low.", true));
    private final DoubleSetting foodThreshold = setting(new DoubleSetting("Food Threshold", "Food level at or below which conservation starts.", 14.0, 1.0, 20.0));
    private final BooleanSetting onlySurvival = setting(new BooleanSetting("Only Survival", "Ignore creative and spectator modes.", true));

    public AntiHunger() {
        super("AntiHunger", "Reduces hunger-draining movement when food is low.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) {
            return;
        }
        if (onlySurvival.enabled() && !client.interactionManager.getCurrentGameMode().isSurvivalLike()) {
            return;
        }
        if (client.player.getHungerManager().getFoodLevel() > foodThreshold.get()) {
            return;
        }

        if (stopSprint.enabled()) {
            client.player.setSprinting(false);
            client.options.sprintKey.setPressed(false);
        }
        if (stopJump.enabled()) {
            client.options.jumpKey.setPressed(false);
        }
        if (stopSwimming.enabled() && client.player.isSwimming()) {
            client.player.setSprinting(false);
        }
    }
}
