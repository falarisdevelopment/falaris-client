package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.ArrayList;
import java.util.List;

public final class Blink extends MiscModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Blink behavior.", "Blink", "Blink", "Pulse"));
    private final IntegerSetting pulseInterval = setting(new IntegerSetting("Pulse Interval", "Ticks between packet bursts (Pulse mode).", 10, 2, 60));
    private final BooleanSetting renderBlink = setting(new BooleanSetting("Render Blink", "Show ghost player at held position.", true));
    private final BooleanSetting toggleOnDisconnect = setting(new BooleanSetting("Toggle on Disconnect", "Disable when logging out.", true));

    private final List<PlayerMoveC2SPacket> heldPackets = new ArrayList<>();
    private int tickCounter;

    public Blink() {
        super("Blink", "Holds movement packets then sends them in bulk to desync your position. Also known as AntiDesync/FakeLag.");
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) return;

        if (mode.is("Pulse")) {
            tickCounter++;
            if (tickCounter >= pulseInterval.get() && !heldPackets.isEmpty()) {
                sendPackets();
                tickCounter = 0;
            }
        }
    }

    public boolean shouldHoldPacket() {
        return isEnabled();
    }

    public void holdPacket(PlayerMoveC2SPacket packet) {
        heldPackets.add(packet);
    }

    public void sendPackets() {
        if (heldPackets.isEmpty()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return;
        for (PlayerMoveC2SPacket p : heldPackets) {
            client.getNetworkHandler().sendPacket(p);
        }
        heldPackets.clear();
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        heldPackets.clear();
        tickCounter = 0;
    }

    @Override
    protected void onDisable() {
        sendPackets();
        super.onDisable();
    }

    public int getHeldPackets() {
        return heldPackets.size();
    }
}
