package dev.falaris.client.module.modules.client;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.module.Category;
import dev.falaris.client.module.Module;

public class DiscordRpc extends Module {
    public DiscordRpc() {
        super("DiscordRPC", "Shows your game status on Discord.", Category.CLIENT);
    }

    @Override
    public void onEnable() {
        FalarisClient.LOGGER.warn("DiscordRPC is not available. Add discord-rpc dependency to build.gradle.kts to enable.");
    }

    @Override
    public void onDisable() {
    }
}
