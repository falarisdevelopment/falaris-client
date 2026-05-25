package dev.falaris.client.command.commands;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.command.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

public final class BindCommand extends Command {
    private static final Map<String, Integer> KEY_MAP = Map.ofEntries(
        Map.entry("NONE", 0), Map.entry("UNBIND", 0),
        Map.entry("A", GLFW.GLFW_KEY_A), Map.entry("B", GLFW.GLFW_KEY_B), Map.entry("C", GLFW.GLFW_KEY_C),
        Map.entry("D", GLFW.GLFW_KEY_D), Map.entry("E", GLFW.GLFW_KEY_E), Map.entry("F", GLFW.GLFW_KEY_F),
        Map.entry("G", GLFW.GLFW_KEY_G), Map.entry("H", GLFW.GLFW_KEY_H), Map.entry("I", GLFW.GLFW_KEY_I),
        Map.entry("J", GLFW.GLFW_KEY_J), Map.entry("K", GLFW.GLFW_KEY_K), Map.entry("L", GLFW.GLFW_KEY_L),
        Map.entry("M", GLFW.GLFW_KEY_M), Map.entry("N", GLFW.GLFW_KEY_N), Map.entry("O", GLFW.GLFW_KEY_O),
        Map.entry("P", GLFW.GLFW_KEY_P), Map.entry("Q", GLFW.GLFW_KEY_Q), Map.entry("R", GLFW.GLFW_KEY_R),
        Map.entry("S", GLFW.GLFW_KEY_S), Map.entry("T", GLFW.GLFW_KEY_T), Map.entry("U", GLFW.GLFW_KEY_U),
        Map.entry("V", GLFW.GLFW_KEY_V), Map.entry("W", GLFW.GLFW_KEY_W), Map.entry("X", GLFW.GLFW_KEY_X),
        Map.entry("Y", GLFW.GLFW_KEY_Y), Map.entry("Z", GLFW.GLFW_KEY_Z),
        Map.entry("0", GLFW.GLFW_KEY_0), Map.entry("1", GLFW.GLFW_KEY_1), Map.entry("2", GLFW.GLFW_KEY_2),
        Map.entry("3", GLFW.GLFW_KEY_3), Map.entry("4", GLFW.GLFW_KEY_4), Map.entry("5", GLFW.GLFW_KEY_5),
        Map.entry("6", GLFW.GLFW_KEY_6), Map.entry("7", GLFW.GLFW_KEY_7), Map.entry("8", GLFW.GLFW_KEY_8),
        Map.entry("9", GLFW.GLFW_KEY_9),
        Map.entry("F1", GLFW.GLFW_KEY_F1), Map.entry("F2", GLFW.GLFW_KEY_F2), Map.entry("F3", GLFW.GLFW_KEY_F3),
        Map.entry("F4", GLFW.GLFW_KEY_F4), Map.entry("F5", GLFW.GLFW_KEY_F5), Map.entry("F6", GLFW.GLFW_KEY_F6),
        Map.entry("F7", GLFW.GLFW_KEY_F7), Map.entry("F8", GLFW.GLFW_KEY_F8), Map.entry("F9", GLFW.GLFW_KEY_F9),
        Map.entry("F10", GLFW.GLFW_KEY_F10), Map.entry("F11", GLFW.GLFW_KEY_F11), Map.entry("F12", GLFW.GLFW_KEY_F12),
        Map.entry("LSHIFT", GLFW.GLFW_KEY_LEFT_SHIFT), Map.entry("RSHIFT", GLFW.GLFW_KEY_RIGHT_SHIFT),
        Map.entry("LCONTROL", GLFW.GLFW_KEY_LEFT_CONTROL), Map.entry("RCONTROL", GLFW.GLFW_KEY_RIGHT_CONTROL),
        Map.entry("LALT", GLFW.GLFW_KEY_LEFT_ALT), Map.entry("RALT", GLFW.GLFW_KEY_RIGHT_ALT),
        Map.entry("TAB", GLFW.GLFW_KEY_TAB), Map.entry("SPACE", GLFW.GLFW_KEY_SPACE),
        Map.entry("ENTER", GLFW.GLFW_KEY_ENTER), Map.entry("ESCAPE", GLFW.GLFW_KEY_ESCAPE),
        Map.entry("GRAVE", GLFW.GLFW_KEY_GRAVE_ACCENT), Map.entry("TILDE", GLFW.GLFW_KEY_GRAVE_ACCENT),
        Map.entry("MINUS", GLFW.GLFW_KEY_MINUS), Map.entry("EQUAL", GLFW.GLFW_KEY_EQUAL),
        Map.entry("LBRACKET", GLFW.GLFW_KEY_LEFT_BRACKET), Map.entry("RBRACKET", GLFW.GLFW_KEY_RIGHT_BRACKET),
        Map.entry("BACKSLASH", GLFW.GLFW_KEY_BACKSLASH), Map.entry("SEMICOLON", GLFW.GLFW_KEY_SEMICOLON),
        Map.entry("APOSTROPHE", GLFW.GLFW_KEY_APOSTROPHE), Map.entry("COMMA", GLFW.GLFW_KEY_COMMA),
        Map.entry("PERIOD", GLFW.GLFW_KEY_PERIOD), Map.entry("SLASH", GLFW.GLFW_KEY_SLASH),
        Map.entry("UP", GLFW.GLFW_KEY_UP), Map.entry("DOWN", GLFW.GLFW_KEY_DOWN),
        Map.entry("LEFT", GLFW.GLFW_KEY_LEFT), Map.entry("RIGHT", GLFW.GLFW_KEY_RIGHT),
        Map.entry("INSERT", GLFW.GLFW_KEY_INSERT), Map.entry("DELETE", GLFW.GLFW_KEY_DELETE),
        Map.entry("HOME", GLFW.GLFW_KEY_HOME), Map.entry("END", GLFW.GLFW_KEY_END),
        Map.entry("PAGEUP", GLFW.GLFW_KEY_PAGE_UP), Map.entry("PAGEDOWN", GLFW.GLFW_KEY_PAGE_DOWN)
    );

    public BindCommand() {
        super("bind", "Bind/unbind a key to a module.", "b");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            message("Usage: .bind <module> <key>  or  .bind <module> none");
            return;
        }
        var opt = FalarisClient.getInstance().getModuleManager().find(args[0]);
        if (opt.isEmpty()) {
            message("Module \"" + args[0] + "\" not found.");
            return;
        }
        var module = opt.get();
        Integer key = KEY_MAP.get(args[1].toUpperCase());
        if (key == null) {
            message("Unknown key: " + args[1]);
            return;
        }
        module.setKeyCode(key);
        FalarisClient.getInstance().getConfigManager().save(FalarisClient.getInstance().getModuleManager());
        if (key == 0) {
            message("Unbound " + module.getName());
        } else {
            message("Bound " + module.getName() + " to " + args[1].toUpperCase());
        }
    }

    private void message(String msg) {
        var c = MinecraftClient.getInstance();
        if (c.player != null) c.player.sendMessage(Text.literal("§7[§bFalaris§7] §f" + msg), false);
    }
}
