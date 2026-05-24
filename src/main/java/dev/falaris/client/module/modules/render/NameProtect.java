package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.StringSetting;
import net.minecraft.client.MinecraftClient;

public final class NameProtect extends RenderModule {
    private final StringSetting customName = setting(new StringSetting("Custom Name", "Fake name to display.", "Protected"));

    private static NameProtect instance;

    public NameProtect() {
        super("NameProtect", "Hides your real username client-side.");
        instance = this;
    }

    public static String getFakeName() {
        if (instance != null && instance.isEnabled()) {
            return instance.customName.get();
        }
        return null;
    }

    @Override
    protected void onRenderTick(MinecraftClient client) {
        if (client.player == null) return;
        if (client.player.hasCustomName()) return;
        client.player.setCustomNameVisible(false);
    }
}
