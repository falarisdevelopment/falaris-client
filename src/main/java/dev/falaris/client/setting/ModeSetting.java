package dev.falaris.client.setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ModeSetting extends Setting<String> {
    private final List<String> modes = new ArrayList<>();

    public ModeSetting(String name, String description, String defaultValue, String... modes) {
        super(name, description, defaultValue);
        this.modes.addAll(Arrays.asList(modes));
        if (!this.modes.contains(defaultValue)) {
            throw new IllegalArgumentException("Default mode must be present in mode list.");
        }
    }

    @Override
    public void set(String value) {
        if (!modes.contains(value)) {
            throw new IllegalArgumentException("Unknown mode: " + value);
        }
        super.set(value);
    }

    public boolean is(String value) {
        return get().equalsIgnoreCase(value);
    }

    public void setModes(String... modes) {
        this.modes.clear();
        this.modes.addAll(java.util.Arrays.asList(modes));
        if (!this.modes.contains(get())) {
            set(this.modes.get(0));
        }
    }

    public List<String> modes() {
        return java.util.Collections.unmodifiableList(modes);
    }
}
