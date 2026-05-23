package dev.falaris.client.module;

import dev.falaris.client.config.ConfigManager;
import dev.falaris.client.event.EventBus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class ModuleManager {
    private final EventBus eventBus;
    private final ConfigManager configManager;
    private final List<Module> modules = new ArrayList<>();

    public ModuleManager(EventBus eventBus, ConfigManager configManager) {
        this.eventBus = eventBus;
        this.configManager = configManager;
    }

    public void register(Module module) {
        modules.add(module);
        modules.sort(Comparator.comparing(Module::getName));
    }

    public Optional<Module> find(String idOrName) {
        String normalized = idOrName.toLowerCase(Locale.ROOT);
        return modules.stream()
                .filter(module -> module.getId().equals(normalized) || module.getName().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }

    public List<Module> search(String query, Category category) {
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        return modules.stream()
                .filter(module -> category == null || module.getCategory() == category)
                .filter(module -> normalized.isEmpty()
                        || module.getName().toLowerCase(Locale.ROOT).contains(normalized)
                        || module.getDescription().toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
    }

    public Map<Category, List<Module>> byCategory() {
        Map<Category, List<Module>> grouped = new EnumMap<>(Category.class);
        for (Category category : Category.values()) {
            grouped.put(category, search("", category));
        }
        return grouped;
    }

    public List<Module> getModules() {
        return List.copyOf(modules);
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public void save() {
        configManager.save(this);
    }
}
