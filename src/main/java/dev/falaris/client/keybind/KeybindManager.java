package dev.falaris.client.keybind;

import dev.falaris.client.module.Module;
import dev.falaris.client.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public final class KeybindManager {
    private final ModuleManager moduleManager;
    private final Set<Integer> pressedKeys = new HashSet<>();

    public KeybindManager(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    public void tick(MinecraftClient client) {
        if (client.getWindow() == null) {
            return;
        }

        long handle = client.getWindow().getHandle();
        for (Module module : moduleManager.getModules()) {
            int keyCode = module.getKeyCode();
            if (keyCode < 0) {
                continue;
            }

            boolean pressed = GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_PRESS;
            if (pressed && pressedKeys.add(keyCode)) {
                module.toggle();
                moduleManager.save();
            } else if (!pressed) {
                pressedKeys.remove(keyCode);
            }
        }
    }
}
