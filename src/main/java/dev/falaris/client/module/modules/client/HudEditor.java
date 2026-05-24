package dev.falaris.client.module.modules.client;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.gui.click.HudEditorScreen;
import dev.falaris.client.module.Category;
import dev.falaris.client.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;

public final class HudEditor extends Module {
    public HudEditor() {
        super("HUD Editor", "Opens the HUD editor to resize and move on-screen modules.", Category.CLIENT);
    }

    @Override
    protected void onEnable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof ChatScreen || client.currentScreen instanceof HudEditorScreen) {
            setEnabled(false);
            return;
        }
        client.setScreen(new HudEditorScreen(FalarisClient.getInstance().getModuleManager()));
        setEnabled(false);
    }
}
