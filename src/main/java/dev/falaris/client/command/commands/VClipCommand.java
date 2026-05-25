package dev.falaris.client.command.commands;

import dev.falaris.client.command.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class VClipCommand extends Command {
    public VClipCommand() {
        super("vclip", "Vertical clip (teleport up/down).", "vc");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            message("Usage: .vclip <blocks>");
            return;
        }
        try {
            double blocks = Double.parseDouble(args[0]);
            var c = MinecraftClient.getInstance();
            if (c.player == null) return;
            c.player.setPosition(c.player.getX(), c.player.getY() + blocks, c.player.getZ());
            message("Clipped " + (blocks >= 0 ? "up" : "down") + " " + Math.abs(blocks) + " blocks.");
        } catch (NumberFormatException e) {
            message("Invalid number: " + args[0]);
        }
    }

    private void message(String msg) {
        var c = MinecraftClient.getInstance();
        if (c.player != null) c.player.sendMessage(Text.literal("§7[§bFalaris§7] §f" + msg), false);
    }
}
