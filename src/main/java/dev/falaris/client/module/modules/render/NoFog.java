package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;

public final class NoFog extends RenderModule {
    public NoFog() {
        super("NoFog", "Removes or reduces fog effects.");
    }

    @Override
    protected void onRenderTick(MinecraftClient client) {
    }
}
