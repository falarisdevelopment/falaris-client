package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;

public final class AutoRespawn extends PlayerModule {
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks to wait before respawning.", 10, 0, 60));
    private final BooleanSetting autoRelease = setting(new BooleanSetting("Auto Release", "Click respawn button automatically.", true));

    private int tickCounter;

    public AutoRespawn() {
        super("AutoRespawn", "Automatically respawns after death.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        tickCounter = 0;
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null) return;

        if (client.currentScreen instanceof DeathScreen) {
            tickCounter++;
            if (tickCounter >= delay.get() && autoRelease.enabled()) {
                client.player.requestRespawn();
                client.setScreen(null);
                tickCounter = 0;
            }
        } else {
            tickCounter = 0;
        }
    }
}
