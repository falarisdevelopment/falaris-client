package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;

public final class AntiAFK extends MiscModule {
    private final ModeSetting action = setting(new ModeSetting("Action", "AFK prevention action.", "Jump", "Jump", "Move", "Look", "Swing"));
    private final IntegerSetting interval = setting(new IntegerSetting("Interval", "Ticks between actions.", 60, 10, 400));

    private int tickCounter;

    public AntiAFK() {
        super("AntiAFK", "Prevents being kicked for inactivity on anarchy servers.");
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.player == null) return;
        tickCounter++;
        if (tickCounter < interval.get()) return;
        tickCounter = 0;

        switch (action.get()) {
            case "Jump" -> {
                if (client.player.isOnGround()) client.player.jump();
            }
            case "Move" -> {
                client.player.setVelocity(
                    client.player.getVelocity().x + (Math.random() - 0.5) * 0.05,
                    client.player.getVelocity().y,
                    client.player.getVelocity().z + (Math.random() - 0.5) * 0.05
                );
            }
            case "Look" -> {
                client.player.setYaw(client.player.getYaw() + (float) (Math.random() * 30 - 15));
                client.player.setPitch(Math.max(-90, Math.min(90, client.player.getPitch() + (float) (Math.random() * 10 - 5))));
            }
            case "Swing" -> client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        }
    }

    @Override protected void onEnable() { super.onEnable(); tickCounter = 0; }
}
