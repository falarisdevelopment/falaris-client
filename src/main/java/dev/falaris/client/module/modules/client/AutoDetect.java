package dev.falaris.client.module.modules.client;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.event.EventPriority;
import dev.falaris.client.event.events.ClientTickEvent;
import dev.falaris.client.module.Category;
import dev.falaris.client.module.Module;
import dev.falaris.client.module.ModuleManager;
import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import dev.falaris.client.setting.Setting;
import dev.falaris.client.util.ServerDetectionUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

public final class AutoDetect extends Module {
    private String lastAddress = "";

    public AutoDetect() {
        super("AutoDetect", "Auto-switches bypass modes based on known anticheat.", Category.CLIENT);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        track(eventBus().subscribe(ClientTickEvent.class, EventPriority.NORMAL, event -> {
            onTick(event.client());
        }));
    }

    private void onTick(MinecraftClient client) {
        if (client.world == null || client.isInSingleplayer()) return;
        ServerInfo entry = client.getCurrentServerEntry();
        if (entry == null) return;
        if (entry.address.equals(lastAddress)) return;
        lastAddress = entry.address;
        ServerDetectionUtil.detect();
        applyModes();
        if (client.player != null) {
            client.player.sendMessage(net.minecraft.text.Text.literal(
                "§7[§bfalaris§7] §aAutoDetect §7→ §f" + ServerDetectionUtil.getDetectedAC() + " §7(" + entry.address + ")"
            ), false);
        }
    }

    private void applyModes() {
        String ac = ServerDetectionUtil.getDetectedAC();
        var mm = FalarisClient.getInstance().getModuleManager();

        setSetting(mm, "killaura", "Bypass", switch (ac) {
            case "Grim" -> "Grim";
            case "Vulcan" -> "Vulcan";
            case "Watchdog" -> "Watchdog";
            default -> "Ghost";
        });

        setSetting(mm, "velocity", "Bypass", switch (ac) {
            case "Grim" -> "Grim2344";
            case "Vulcan", "Watchdog" -> "Legit";
            default -> "Vanilla";
        });

        if (ServerDetectionUtil.isGrim()) {
            setSetting(mm, "timer", "Mode", "Balance");
            setSetting(mm, "reach", "Grim Mode", "On");
        }
    }

    @SuppressWarnings("unchecked")
    private void setSetting(ModuleManager mm, String moduleId, String settingName, Object value) {
        mm.find(moduleId).ifPresent(mod -> {
            for (Setting<?> s : mod.getSettings()) {
                if (s.getName().equals(settingName)) {
                    if (s instanceof DoubleSetting ds && value instanceof Number n) {
                        ds.set(n.doubleValue());
                    } else if (s instanceof IntegerSetting is && value instanceof Number n) {
                        is.set(n.intValue());
                    } else if (s instanceof BooleanSetting bs && value instanceof Boolean b) {
                        bs.set(b);
                    } else if (s instanceof ModeSetting ms && value instanceof String str) {
                        try { ms.set(str); } catch (Exception ignored) {}
                    }
                }
            }
        });
    }

    @Override
    protected void onDisable() {
        lastAddress = "";
        super.onDisable();
    }
}
