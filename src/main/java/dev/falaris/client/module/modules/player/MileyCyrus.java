package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;

public final class MileyCyrus extends PlayerModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Twerk style.", "Sneak", "Sneak", "Spin", "Both"));
    private final IntegerSetting speed = setting(new IntegerSetting("Speed", "Ticks between toggles.", 2, 1, 10));

    private int tickCounter;
    private boolean toggle;

    public MileyCyrus() {
        super("MileyCyrus", "Rapidly toggles sneak for a twerking effect.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null) return;

        tickCounter++;
        if (tickCounter < speed.get()) return;
        tickCounter = 0;

        toggle = !toggle;

        if (mode.is("Sneak") || mode.is("Both")) {
            client.player.setSneaking(toggle);
        }

        if (mode.is("Spin") || mode.is("Both")) {
            client.player.setYaw(client.player.getYaw() + 15.0f);
            client.player.setHeadYaw(client.player.getYaw());
        }
    }
}
