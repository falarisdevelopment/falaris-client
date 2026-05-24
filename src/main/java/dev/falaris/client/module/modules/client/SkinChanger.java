package dev.falaris.client.module.modules.client;

import dev.falaris.client.gui.skin.SkinChangerScreen;
import dev.falaris.client.module.Category;
import dev.falaris.client.module.Module;
import net.minecraft.client.MinecraftClient;

public final class SkinChanger extends Module {
    public SkinChanger() {
        super("SkinChanger", "Search any player's skin and apply it to yourself.", Category.CLIENT);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new SkinChangerScreen());
        setEnabled(false);
    }
}
