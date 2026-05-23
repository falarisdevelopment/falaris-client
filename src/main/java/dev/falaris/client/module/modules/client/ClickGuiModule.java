package dev.falaris.client.module.modules.client;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.module.Category;
import dev.falaris.client.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;

public final class ClickGuiModule extends Module {
    public ClickGuiModule() {
        super("Click GUI", "Opens the Falaris ClickGUI.", Category.CLIENT);
        setKeyCode(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    protected void onEnable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof ChatScreen) {
            setEnabled(false);
            return;
        }

        FalarisClient.getInstance().openClickGui();
        setEnabled(false);
    }
}
