package dev.falaris.client.module;

import dev.falaris.client.FalarisClient;

import java.util.ArrayList;
import java.util.List;

public final class IgnoresManager {
    private final List<String> ignores = new ArrayList<>();

    public void add(String name) {
        String lower = name.toLowerCase();
        if (!ignores.contains(lower)) {
            ignores.add(lower);
            save();
        }
    }

    public void remove(String name) {
        ignores.remove(name.toLowerCase());
        save();
    }

    public boolean isIgnored(String name) {
        return ignores.contains(name.toLowerCase());
    }

    public List<String> getAll() {
        return List.copyOf(ignores);
    }

    public void setIgnores(List<String> list) {
        ignores.clear();
        for (String s : list) {
            ignores.add(s.toLowerCase());
        }
    }

    private void save() {
        FalarisClient.getInstance().getConfigManager().saveIgnores(this);
    }
}
