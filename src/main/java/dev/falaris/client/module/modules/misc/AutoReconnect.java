package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

public final class AutoReconnect extends MiscModule {
    private final IntegerSetting delaySeconds = setting(new IntegerSetting("Delay Seconds", "Seconds to wait before reconnecting.", 5, 1, 120));
    private final IntegerSetting jitterSeconds = setting(new IntegerSetting("Jitter Seconds", "Random extra seconds before reconnecting.", 2, 0, 60));
    private final BooleanSetting multiplayerOnly = setting(new BooleanSetting("Multiplayer Only", "Only reconnect to multiplayer servers.", true));

    private ServerInfo lastServer;

    public AutoReconnect() {
        super("AutoReconnect", "Reconnects to the last multiplayer server after disconnect screens.");
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.world != null && client.getCurrentServerEntry() != null) {
            lastServer = client.getCurrentServerEntry();
        }
        if (!(client.currentScreen instanceof DisconnectedScreen) || lastServer == null) {
            return;
        }
        if (multiplayerOnly.enabled() && client.isInSingleplayer()) {
            return;
        }
        if (!ready(delaySeconds.get() * 20, jitterSeconds.get() * 20)) {
            return;
        }

        ConnectScreen.connect(new TitleScreen(), client, ServerAddress.parse(lastServer.address), lastServer, false, null);
    }
}
