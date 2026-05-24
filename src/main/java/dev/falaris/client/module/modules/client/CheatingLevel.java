package dev.falaris.client.module.modules.client;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.event.EventPriority;
import dev.falaris.client.event.events.ClientTickEvent;
import dev.falaris.client.module.Category;
import dev.falaris.client.module.Module;
import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import dev.falaris.client.setting.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.List;

public final class CheatingLevel extends Module {
    private final ModeSetting level = setting(new ModeSetting("Level", "Your cheating level. Auto-configures all modules.", "Closet", "Closet", "Anarchy", "Blatant"));
    private final BooleanSetting autoConfig = setting(new BooleanSetting("Auto Config", "Auto-apply module settings when level changes.", true));

    private String previousLevel = "Closet";

    public CheatingLevel() {
        super("CheatingLevel", "Auto-configures all modules for your chosen playstyle (Closet/Anarchy/Blatant).", Category.CLIENT);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        applyLevel();
        track(eventBus().subscribe(ClientTickEvent.class, EventPriority.NORMAL, event -> {
            if (event.client().player == null) return;
            String cur = level.get();
            if (!cur.equals(previousLevel) && autoConfig.enabled()) {
                previousLevel = cur;
                applyLevel();
                event.client().player.sendMessage(net.minecraft.text.Text.literal(
                    "§7[§bfalaris§7] §fConfigured modules for §b" + cur + "§f mode."), false);
            }
        }));
    }

    public String getLevel() { return level.get(); }
    public boolean isCloset() { return level.is("Closet"); }
    public boolean isAnarchy() { return level.is("Anarchy"); }
    public boolean isBlatant() { return level.is("Blatant"); }

    private List<Module> getModules() {
        return FalarisClient.getInstance().getModuleManager().getModules();
    }

    @SuppressWarnings("unchecked")
    private void setSetting(Module mod, String name, Object value) {
        if (mod == null) return;
        for (Setting<?> s : mod.getSettings()) {
            if (s.getName().equals(name)) {
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
    }

    private void enableIfExists(String id) {
        getModules().stream().filter(m -> m.getId().equals(id)).findFirst().ifPresent(m -> {
            if (!m.isEnabled()) m.setEnabled(true);
        });
    }

    private void disableIfExists(String id) {
        getModules().stream().filter(m -> m.getId().equals(id)).findFirst().ifPresent(m -> {
            if (m.isEnabled()) m.setEnabled(false);
        });
    }

    private Module find(String id) {
        return getModules().stream().filter(m -> m.getId().equals(id)).findFirst().orElse(null);
    }

    private void applyLevel() {
        if (!autoConfig.enabled()) return;
        String lvl = level.get();
        previousLevel = lvl;
        switch (lvl) {
            case "Closet" -> applyCloset();
            case "Anarchy" -> applyAnarchy();
            case "Blatant" -> applyBlatant();
        }
        FalarisClient.getInstance().getModuleManager().save();
    }

    private void applyCloset() {
        enableIfExists("kill-aura");
        enableIfExists("auto-sprint");
        enableIfExists("auto-totem");
        enableIfExists("auto-armor");
        enableIfExists("array-list");
        enableIfExists("fullbright");

        enableIfExists("reach");
        setSetting(find("reach"), "Attack Reach", 0.2);
        setSetting(find("reach"), "Block Reach", 0.2);
        setSetting(find("reach"), "Bypass", "Vanilla");

        enableIfExists("trigger-bot");
        enableIfExists("aim-assist");

        enableIfExists("velocity");
        setSetting(find("velocity"), "Mode", "Reduce");
        setSetting(find("velocity"), "Horizontal", 0.60);
        setSetting(find("velocity"), "Vertical", 0.60);
        setSetting(find("velocity"), "Bypass", "Legit");

        enableIfExists("no-slowdown");
        setSetting(find("no-slowdown"), "Mode", "Grim");

        enableIfExists("hitboxes");
        setSetting(find("hitboxes"), "Expand", 0.1);

        enableIfExists("name-tags");
        enableIfExists("armor-hud");
        enableIfExists("inventory-hud");

        enableIfExists("no-fall");
        enableIfExists("safe-walk");

        disableIfExists("flight");
        disableIfExists("speed");
        disableIfExists("esp");
        disableIfExists("tracers");
        disableIfExists("scaffold");
        disableIfExists("nuker");
        disableIfExists("crystal-aura");
        disableIfExists("auto-mace");
    }

    private void applyAnarchy() {
        enableIfExists("kill-aura");
        setSetting(find("kill-aura"), "Range", 4.0);
        setSetting(find("kill-aura"), "Bypass", "Vanilla");

        enableIfExists("reach");
        setSetting(find("reach"), "Attack Reach", 0.8);
        setSetting(find("reach"), "Block Reach", 0.5);

        enableIfExists("velocity");
        setSetting(find("velocity"), "Mode", "Reduce");
        setSetting(find("velocity"), "Horizontal", 0.30);
        setSetting(find("velocity"), "Vertical", 0.30);

        enableIfExists("hitboxes");
        setSetting(find("hitboxes"), "Expand", 0.3);

        enableIfExists("crystal-aura");
        enableIfExists("auto-mace");
        enableIfExists("auto-sprint");
        enableIfExists("auto-totem");
        enableIfExists("auto-armor");
        enableIfExists("trigger-bot");

        enableIfExists("esp");
        enableIfExists("tracers");
        enableIfExists("array-list");
        enableIfExists("armor-hud");
        enableIfExists("inventory-hud");
        enableIfExists("name-tags");
        enableIfExists("fullbright");

        enableIfExists("elytra-fly");
        enableIfExists("no-fall");
        enableIfExists("safe-walk");
        disableIfExists("flight");

        enableIfExists("no-slowdown");
        setSetting(find("no-slowdown"), "Mode", "Vanilla");
        enableIfExists("anti-bot");
    }

    private void applyBlatant() {
        enableIfExists("kill-aura");
        setSetting(find("kill-aura"), "Range", 6.0);
        setSetting(find("kill-aura"), "Through Walls", true);
        setSetting(find("kill-aura"), "Bypass", "Vanilla");

        enableIfExists("crystal-aura");
        enableIfExists("anchor-aura");
        enableIfExists("auto-mace");
        enableIfExists("bed-aura");
        enableIfExists("auto-city");
        enableIfExists("offhand-swap");

        enableIfExists("reach");
        setSetting(find("reach"), "Attack Reach", 3.0);
        setSetting(find("reach"), "Block Reach", 3.0);

        enableIfExists("velocity");
        setSetting(find("velocity"), "Mode", "Cancel");
        setSetting(find("velocity"), "Push Immunity", true);

        enableIfExists("hitboxes");
        setSetting(find("hitboxes"), "Expand", 1.0);
        enableIfExists("criticals");

        enableIfExists("flight");
        enableIfExists("elytra-fly");
        enableIfExists("no-fall");
        enableIfExists("scaffold");

        enableIfExists("esp");
        enableIfExists("tracers");
        enableIfExists("name-tags");
        enableIfExists("array-list");
        enableIfExists("armor-hud");
        enableIfExists("inventory-hud");
        enableIfExists("fullbright");

        enableIfExists("no-slowdown");
        setSetting(find("no-slowdown"), "Mode", "Vanilla");
        enableIfExists("auto-totem");
        enableIfExists("auto-armor");
        enableIfExists("auto-sprint");
        enableIfExists("auto-tool");

        enableIfExists("nuker");
        enableIfExists("anti-bot");
    }
}
