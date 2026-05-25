package dev.falaris.client.command;

import dev.falaris.client.command.commands.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.*;

public final class CommandManager {
    private static CommandManager instance;
    private final List<Command> commands = new ArrayList<>();

    public CommandManager() {
        instance = this;
    }

    public void registerAll() {
        register(new ToggleCommand());
        register(new BindCommand());
        register(new FriendCommand());
        register(new PanicCommand());
        register(new VClipCommand());
        register(new HClipCommand());
        register(new HelpCommand());
        register(new PresetCommand());
    }

    public void register(Command cmd) {
        commands.add(cmd);
    }

    public boolean execute(String message) {
        if (!message.startsWith(".")) return false;
        String[] parts = message.substring(1).split("\\s+");
        if (parts.length == 0) return false;

        String name = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        for (Command cmd : commands) {
            if (cmd.getName().equals(name) || cmd.getAliases().contains(name)) {
                try {
                    cmd.execute(args);
                } catch (Exception e) {
                    var c = MinecraftClient.getInstance();
                    if (c.player != null) {
                        c.player.sendMessage(Text.literal("§cError executing command: " + e.getMessage()), false);
                    }
                }
                return true;
            }
        }
        return false;
    }

    public List<Command> getCommands() { return List.copyOf(commands); }
    public static CommandManager getInstance() { return instance; }
}
