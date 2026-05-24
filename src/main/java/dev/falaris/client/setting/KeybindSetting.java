package dev.falaris.client.setting;

public final class KeybindSetting extends Setting<Integer> {
    private final int defaultValue;

    public KeybindSetting(String name, String description, int defaultValue) {
        super(name, description, defaultValue);
        this.defaultValue = defaultValue;
    }

    public int getDefault() {
        return defaultValue;
    }
}
