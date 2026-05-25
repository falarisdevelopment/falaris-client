package dev.falaris.client.util;

import net.minecraft.client.MinecraftClient;

import java.util.Locale;

public final class ServerDetectionUtil {
    private static String cachedServerIP = "";
    private static String detectedAC = "Unknown";
    private static boolean detected;

    public static void detect() {
        var client = MinecraftClient.getInstance();
        var entry = client.getCurrentServerEntry();
        if (entry == null) return;
        String address = entry.address.toLowerCase(Locale.ROOT);
        if (address.equals(cachedServerIP)) return;
        cachedServerIP = address;
        detectedAC = identify(address);
        detected = true;
    }

    private static String identify(String address) {
        if (address.contains("minemen") || address.contains("mcmen")) return "Grim";
        if (address.contains("pvplegacy")) return "Grim";
        if (address.contains("catpvp")) return "Grim";
        if (address.contains("playpvp") || address.contains("pvp.club")) return "Grim";
        if (address.contains("hypixel")) return "Watchdog";
        if (address.contains("minerep")) return "Vulcan";
        if (address.contains("gomme") || address.contains("gowme")) return "Grim";
        if (address.contains("cubecraft")) return "Vulcan";
        if (address.contains("blocksmc")) return "Vulcan";
        if (address.contains("universocraft")) return "Grim";
        if (address.contains("jartex")) return "Grim";
        if (address.contains("vanity")) return "Grim";
        if (address.contains("lunar") || address.contains("lcc")) return "Grim";
        if (address.contains("centauri")) return "Grim";
        if (address.contains("vulcan") || address.contains("funcraft")) return "Vulcan";
        if (address.contains("spartan")) return "Spartan";
        if (address.contains("matrix") || address.contains("matriix")) return "Matrix";
        if (address.contains("negativity")) return "Negativity";
        if (address.contains("karhu")) return "Karhu";
        if (address.contains("verus")) return "Verus";
        if (address.contains("ncp") || address.contains("no-cheat-plus")) return "NoCheatPlus";
        return "Unknown";
    }

    public static String getDetectedAC() {
        return detectedAC;
    }

    public static boolean isGrim() {
        return detectedAC.equals("Grim");
    }

    public static boolean isVulcan() {
        return detectedAC.equals("Vulcan");
    }

    public static boolean isWatchdog() {
        return detectedAC.equals("Watchdog");
    }

    public static String getServerIP() {
        return cachedServerIP;
    }

    public static boolean hasDetected() {
        return detected;
    }

    public static void reset() {
        cachedServerIP = "";
        detectedAC = "Unknown";
        detected = false;
    }
}
