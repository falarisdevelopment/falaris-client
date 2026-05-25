package dev.falaris.client.command.commands;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.command.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class FriendCommand extends Command {
    public FriendCommand() {
        super("friend", "Add/remove/list friends.", "f");
    }

    @Override
    public void execute(String[] args) {
        var fm = FalarisClient.getInstance().getFriendsManager();
        if (args.length == 0) {
            message("Friends: " + String.join(", ", fm.getAll()));
            return;
        }
        String name = args[0];
        if (fm.isFriend(name)) {
            fm.remove(name);
            message("Removed " + name + " from friends.");
        } else {
            fm.add(name);
            message("Added " + name + " to friends.");
        }
    }

    private void message(String msg) {
        var c = MinecraftClient.getInstance();
        if (c.player != null) c.player.sendMessage(Text.literal("§7[§bFalaris§7] §f" + msg), false);
    }
}
