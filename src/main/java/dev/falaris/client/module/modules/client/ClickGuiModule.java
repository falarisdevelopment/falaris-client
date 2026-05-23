package dev.falaris.client.module.modules.client;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.module.Category;
import dev.falaris.client.module.Module;
import org.lwjgl.glfw.GLFW;

public final class ClickGuiModule extends Module {
    public ClickGuiModule() {
        super("Click GUI", "Opens the Falaris ClickGUI.", Category.CLIENT);
        setKeyCode(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    protected void onEnable() {
        FalarisClient.getInstance().openClickGui();
        setEnabled(false);
    }
}
