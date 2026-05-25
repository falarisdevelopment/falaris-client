package dev.falaris.client.command.commands;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.command.Command;
import dev.falaris.client.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class PanicCommand extends Command {
    public PanicCommand() {
        super("panic", "Disable all modules.", "p");
    }

    @Override
    public void execute(String[] args) {
        int count = 0;
        for (Module m : FalarisClient.getInstance().getModuleManager().getModules()) {
            if (m.isEnabled()) { m.setEnabled(false); count++; }
        }
        message("Disabled " + count + " module(s).");
    }

    private void message(String msg) {
        var c = MinecraftClient.getInstance();
        if (c.player != null) c.player.sendMessage(Text.literal("§7[§bFalaris§7] §f" + msg), false);
    }
}
