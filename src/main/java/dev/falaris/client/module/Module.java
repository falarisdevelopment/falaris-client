package dev.falaris.client.module;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.event.EventBus;
import dev.falaris.client.event.Subscription;
import dev.falaris.client.setting.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public abstract class Module {
    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private final List<Subscription> subscriptions = new ArrayList<>();
    private final List<Setting<?>> settings = new ArrayList<>();
    private final dev.falaris.client.setting.BooleanSetting showNotifications = setting(new dev.falaris.client.setting.BooleanSetting("Notifications", "Show chat message when toggled.", true));
    private boolean enabled;
    private int keyCode = -1;

    protected Module(String name, String description, Category category) {
        this.id = name.toLowerCase(Locale.ROOT).replace(' ', '-');
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public final void toggle() {
        setEnabled(!enabled);
        if (showNotifications.enabled()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(net.minecraft.text.Text.literal("§7[§bfalaris§7] §fToggled §b" + name + " §7(" + (enabled ? "§aON" : "§cOFF") + "§7)"), false);
            }
        }
    }

    public final void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }

        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            subscriptions.forEach(Subscription::unsubscribe);
            subscriptions.clear();
            onDisable();
        }
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    protected final EventBus eventBus() {
        return FalarisClient.getInstance().getEventBus();
    }

    protected final void track(Subscription subscription) {
        subscriptions.add(subscription);
    }

    protected final <T extends Setting<?>> T setting(T setting) {
        settings.add(setting);
        return setting;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }
}
