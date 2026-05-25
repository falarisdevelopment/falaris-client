package dev.falaris.client.module.modules.render;

import dev.falaris.client.notification.NotificationManager;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;

public final class Notifications extends RenderModule {
    private final ModeSetting position = setting(new ModeSetting("Position", "Notification position.", "Top Right", "Top Right", "Bottom Right", "Top Left", "Bottom Left"));
    private final ModeSetting animation = setting(new ModeSetting("Animation", "Slide animation style.", "Slide", "Slide", "Fade", "None"));

    public Notifications() {
        super("Notifications", "Shows toast popups when toggling modules.");
    }

    @Override
    protected void onHudRender(net.minecraft.client.gui.DrawContext context, float tickDelta) {
        // Rendering is handled by NotificationManager directly
    }
}
