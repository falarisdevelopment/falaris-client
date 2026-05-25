package dev.falaris.client.module.modules.misc;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.ArrayList;
import java.util.List;

public final class FakeLag extends MiscModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Lag behavior.", "Delay", "Delay", "Pulse", "BlinkCombat"));
    private final IntegerSetting holdTicks = setting(new IntegerSetting("Hold Ticks", "Ticks to hold packets.", 10, 2, 100));
    private final IntegerSetting pulseInterval = setting(new IntegerSetting("Pulse Interval", "Ticks between bursts.", 15, 2, 60));
    private final BooleanSetting toggleOnDisconnect = setting(new BooleanSetting("Toggle on Disconnect", "Disable on logout.", true));

    // BlinkCombat settings
    private final IntegerSetting combatFlushDelay = setting(new IntegerSetting("Combat Flush Delay", "Ticks after attack to flush.", 5, 0, 20));

    private final List<PlayerMoveC2SPacket> heldPackets = new ArrayList<>();
    private int tickCounter;
    private int combatHoldTicks;

    public FakeLag() {
        super("FakeLag", "Delays movement packets to create a lag-back / blink combat effect.");
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) { heldPackets.clear(); return; }

        if (mode.is("BlinkCombat")) {
            handleBlinkCombat(client);
            return;
        }

        tickCounter++;
        boolean flush = switch (mode.get()) {
            case "Delay" -> tickCounter >= holdTicks.get();
            case "Pulse" -> tickCounter >= pulseInterval.get();
            default -> false;
        };
        if (flush && !heldPackets.isEmpty()) { sendPackets(); tickCounter = 0; }
    }

    private void handleBlinkCombat(MinecraftClient client) {
        tickCounter++;
        boolean inCombat = client.player.hurtTime > 0 || isAttacking();

        if (inCombat) {
            combatHoldTicks = holdTicks.get();
        }

        if (combatHoldTicks > 0) {
            combatHoldTicks--;
            if (combatHoldTicks <= 0 && !heldPackets.isEmpty()) {
                sendPackets();
            }
        } else if (tickCounter >= holdTicks.get() && !heldPackets.isEmpty()) {
            sendPackets();
            tickCounter = 0;
        }
    }

    private boolean isAttacking() {
        var ka = FalarisClient.getInstance().getModuleManager().find("killaura");
        return ka.isPresent() && ka.get().isEnabled();
    }

    public boolean shouldHoldPacket() {
        if (!isEnabled()) return false;
        return switch (mode.get()) {
            case "BlinkCombat" -> combatHoldTicks > 0 || tickCounter < holdTicks.get();
            case "Delay" -> tickCounter < holdTicks.get();
            case "Pulse" -> true;
            default -> false;
        };
    }

    public void holdPacket(PlayerMoveC2SPacket packet) { heldPackets.add(packet); }

    private void sendPackets() {
        if (heldPackets.isEmpty()) return;
        var nh = MinecraftClient.getInstance().getNetworkHandler();
        if (nh == null) return;
        for (PlayerMoveC2SPacket p : heldPackets) nh.sendPacket(p);
        heldPackets.clear();
    }

    @Override protected void onEnable() { super.onEnable(); heldPackets.clear(); tickCounter = 0; combatHoldTicks = 0; }
    @Override protected void onDisable() { sendPackets(); super.onDisable(); }

    public int getHeldPackets() { return heldPackets.size(); }
}
