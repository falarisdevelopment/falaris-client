package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.StringSetting;
import dev.falaris.client.setting.KeybindSetting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public final class Macro extends MiscModule {
    private final IntegerSetting macroIndex = setting(new IntegerSetting("Macro Index", "Which macro slot (1-10).", 1, 1, 10));
    private final KeybindSetting macroKey = setting(new KeybindSetting("Macro Key", "Key to activate this macro.", -1));
    private final StringSetting command1 = setting(new StringSetting("Command 1", "First command.", ""));
    private final StringSetting command2 = setting(new StringSetting("Command 2", "Second command.", ""));
    private final StringSetting command3 = setting(new StringSetting("Command 3", "Third command.", ""));
    private final StringSetting command4 = setting(new StringSetting("Command 4", "Fourth command.", ""));
    private final StringSetting command5 = setting(new StringSetting("Command 5", "Fifth command.", ""));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Delay between commands (ms).", 100, 0, 2000));

    private static final List<String>[] MACRO_STORAGE = new ArrayList[11];
    static {
        for (int i = 0; i <= 10; i++) MACRO_STORAGE[i] = new ArrayList<>();
    }

    private long lastExecuteTime;
    private boolean wasPressed;

    public Macro() {
        super("Macro", "Execute custom command chains with a single keybind.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        loadCurrentMacro();
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.player == null) return;

        int key = macroKey.get();
        if (key < 0) return;

        boolean pressed;
        try {
            pressed = org.lwjgl.glfw.GLFW.glfwGetKey(client.getWindow().getHandle(), key) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        } catch (Exception e) {
            return;
        }

        if (pressed && !wasPressed) {
            executeMacro(client);
        }
        wasPressed = pressed;
    }

    private void executeMacro(MinecraftClient client) {
        long now = System.currentTimeMillis();
        if (now - lastExecuteTime < delay.get()) return;
        lastExecuteTime = now;

        int idx = macroIndex.get();
        if (idx < 1 || idx > 10) return;

        List<String> commands = MACRO_STORAGE[idx];
        for (String cmd : commands) {
            if (cmd.isEmpty()) continue;
            if (cmd.startsWith("/")) {
                client.player.networkHandler.sendChatMessage(cmd.substring(1));
            } else {
                client.player.networkHandler.sendChatMessage(cmd);
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void loadCurrentMacro() {
        int idx = macroIndex.get();
        if (idx < 1 || idx > 10) return;
        List<String> cmds = MACRO_STORAGE[idx];
        cmds.clear();
        addIfNotEmpty(cmds, command1.get());
        addIfNotEmpty(cmds, command2.get());
        addIfNotEmpty(cmds, command3.get());
        addIfNotEmpty(cmds, command4.get());
        addIfNotEmpty(cmds, command5.get());
    }

    public void saveCurrentMacro() {
        int idx = macroIndex.get();
        if (idx < 1 || idx > 10) return;
        List<String> cmds = MACRO_STORAGE[idx];
        cmds.clear();
        addIfNotEmpty(cmds, command1.get());
        addIfNotEmpty(cmds, command2.get());
        addIfNotEmpty(cmds, command3.get());
        addIfNotEmpty(cmds, command4.get());
        addIfNotEmpty(cmds, command5.get());
    }

    private void addIfNotEmpty(List<String> list, String s) {
        if (s != null && !s.trim().isEmpty()) {
            list.add(s.trim());
        }
    }
}
