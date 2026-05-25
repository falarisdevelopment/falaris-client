package dev.falaris.client.command.commands;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.command.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class ToggleCommand extends Command {
    public ToggleCommand() {
        super("toggle", "Toggle a module on/off.", "t");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            message("Usage: .toggle <module>");
            return;
        }
        var opt = FalarisClient.getInstance().getModuleManager().find(args[0]);
        if (opt.isEmpty()) {
            message("Module \"" + args[0] + "\" not found.");
            return;
        }
        var module = opt.get();
        module.toggle();
        message("Toggled " + module.getName() + " " + (module.isEnabled() ? "ON" : "OFF"));
    }

    private void message(String msg) {
        var c = MinecraftClient.getInstance();
        if (c.player != null) c.player.sendMessage(Text.literal("§7[§bFalaris§7] §f" + msg), false);
    }
}
