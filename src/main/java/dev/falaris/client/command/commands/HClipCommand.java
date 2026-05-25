package dev.falaris.client.command.commands;

import dev.falaris.client.command.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class HClipCommand extends Command {
    public HClipCommand() {
        super("hclip", "Horizontal clip (teleport forward/backward).", "hc");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            message("Usage: .hclip <blocks>");
            return;
        }
        try {
            double blocks = Double.parseDouble(args[0]);
            var c = MinecraftClient.getInstance();
            if (c.player == null) return;
            double yaw = Math.toRadians(c.player.getYaw());
            double x = -Math.sin(yaw) * blocks;
            double z = Math.cos(yaw) * blocks;
            c.player.setPosition(c.player.getX() + x, c.player.getY(), c.player.getZ() + z);
            message("Clipped forward " + blocks + " blocks.");
        } catch (NumberFormatException e) {
            message("Invalid number: " + args[0]);
        }
    }

    private void message(String msg) {
        var c = MinecraftClient.getInstance();
        if (c.player != null) c.player.sendMessage(Text.literal("§7[§bFalaris§7] §f" + msg), false);
    }
}
