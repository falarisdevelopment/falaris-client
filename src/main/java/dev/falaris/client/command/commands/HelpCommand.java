package dev.falaris.client.command.commands;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.command.Command;
import dev.falaris.client.command.CommandManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "List available commands.", "h", "?");
    }

    @Override
    public void execute(String[] args) {
        var sb = new StringBuilder("§7[§bCommands§7]§f ");
        for (Command cmd : CommandManager.getInstance().getCommands()) {
            sb.append("§b.").append(cmd.getName()).append("§7, ");
        }
        if (sb.length() > 3) sb.setLength(sb.length() - 2);
        message(sb.toString());
    }

    private void message(String msg) {
        var c = MinecraftClient.getInstance();
        if (c.player != null) c.player.sendMessage(Text.literal(msg), false);
    }
}
