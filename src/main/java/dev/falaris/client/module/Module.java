package dev.falaris.client.module;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.event.EventBus;
import dev.falaris.client.event.Subscription;
import dev.falaris.client.setting.Setting;

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
