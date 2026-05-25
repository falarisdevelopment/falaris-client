package dev.falaris.client.command.commands;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.command.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class PresetCommand extends Command {
    public PresetCommand() {
        super("preset", "List, load, or save presets.", "p");
    }

    @Override
    public void execute(String[] args) {
        var pm = FalarisClient.getInstance().getPresetsManager();
        var cm = FalarisClient.getInstance().getModuleManager();
        if (args.length == 0) {
            message("Presets: " + String.join(", ", pm.getPresetNames()));
            return;
        }
        if (args[0].equalsIgnoreCase("save") && args.length >= 2) {
            pm.savePreset(args[1], cm);
            message("Saved preset: " + args[1]);
        } else if (args[0].equalsIgnoreCase("load") && args.length >= 2) {
            if (!pm.hasPreset(args[1])) {
                message("Preset \"" + args[1] + "\" not found.");
                return;
            }
            pm.loadPreset(args[1], cm);
            message("Loaded preset: " + args[1]);
        } else if (pm.hasPreset(args[0])) {
            pm.loadPreset(args[0], cm);
            message("Loaded preset: " + args[0]);
        } else {
            message("Usage: .preset <name>  |  .preset (save|load) <name>");
        }
    }

    private void message(String msg) {
        var c = MinecraftClient.getInstance();
        if (c.player != null) c.player.sendMessage(Text.literal("§7[§bFalaris§7] §f" + msg), false);
    }
}
