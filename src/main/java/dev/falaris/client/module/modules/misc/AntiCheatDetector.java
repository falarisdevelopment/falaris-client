package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;

import java.util.ArrayList;
import java.util.List;

public final class AntiCheatDetector extends MiscModule {
    private final ModeSetting displayMode = setting(new ModeSetting("Display Mode", "How to show detection info.", "HUD", "HUD", "Chat", "Both"));
    private final BooleanSetting logPackets = setting(new BooleanSetting("Log Packets", "Log suspected anticheat packets to chat.", false));

    private String detectedAC = "Unknown";
    private String confidence = "Low";
    private final List<String> flags = new ArrayList<>();
    private int teleportCount;
    private int movePacketCount;
    private long lastTeleportTime;
    private int suspiciousTeleports;
    private boolean hasSpoofedBrand;

    public AntiCheatDetector() {
        super("AntiCheatDetector", "Scans server behavior to detect active anticheat.");
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        if (client.getNetworkHandler() == null) return;

        flags.clear();

        String brand = client.getNetworkHandler().getBrand();
        if (brand != null) {
            if (brand.toLowerCase().contains("grim") || brand.toLowerCase().contains("grimac")) {
                flags.add("Brand: GrimAC");
            }
            if (brand.toLowerCase().contains("paper")) {
                flags.add("Brand: Paper (may have anticheats)");
            }
            if (brand.toLowerCase().contains("purpur")) {
                flags.add("Brand: Purpur");
            }
        }

        if (teleportCount > 5 && (System.currentTimeMillis() - lastTeleportTime) < 3000) {
            suspiciousTeleports++;
            if (suspiciousTeleports > 3) {
                flags.add("Frequent position corrections (Grim/Vulcan)");
            }
        }

        if (client.player.getVelocity().lengthSquared() > 0.01) {
            movePacketCount++;
        }

        if (flags.isEmpty()) {
            detectedAC = "Vanilla/Unknown";
            confidence = "Low";
        } else {
            StringBuilder sb = new StringBuilder();
            for (String f : flags) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(f);
            }
            detectedAC = sb.toString();
            confidence = suspiciousTeleports > 3 ? "High" : "Medium";
        }
    }

    public String getDetectedAC() {
        return detectedAC;
    }

    public String getConfidence() {
        return confidence;
    }

    public void onPacketReceived(Object packet) {
        if (!isEnabled()) return;
        if (packet instanceof PlayerPositionLookS2CPacket) {
            teleportCount++;
            lastTeleportTime = System.currentTimeMillis();
        }
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        teleportCount = 0;
        movePacketCount = 0;
        suspiciousTeleports = 0;
        detectedAC = "Scanning...";
    }
}
