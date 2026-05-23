package dev.falaris.client.config;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.module.Module;
import dev.falaris.client.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ConfigManager {
    private final Path configDirectory;
    private final Path modulesFile;

    public ConfigManager(String modId) {
        this.configDirectory = FabricLoader.getInstance().getConfigDir().resolve(modId);
        this.modulesFile = configDirectory.resolve("modules.json");
    }

    public void load(ModuleManager moduleManager) {
        try {
            Files.createDirectories(configDirectory);

            if (!Files.exists(modulesFile)) {
                save(moduleManager);
                return;
            }

            String json = Files.readString(modulesFile);
            JsonObject root = JsonObject.parse(json);
            JsonObject modules = root.object("modules").orElseGet(JsonObject::new);

            for (Module module : moduleManager.getModules()) {
                Optional<JsonObject> entry = modules.object(module.getId());
                entry.ifPresent(value -> {
                    module.setEnabled(value.booleanValue("enabled").orElse(false));
                    value.intValue("key").ifPresent(module::setKeyCode);
                });
            }
        } catch (IOException | IllegalArgumentException exception) {
            FalarisClient.LOGGER.warn("Failed to load config, using defaults.", exception);
        }
    }

    public void save(ModuleManager moduleManager) {
        try {
            Files.createDirectories(configDirectory);

            Map<String, Object> modules = new LinkedHashMap<>();
            for (Module module : moduleManager.getModules()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("enabled", module.isEnabled());
                entry.put("key", module.getKeyCode());
                modules.put(module.getId(), entry);
            }

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("modules", modules);

            Files.writeString(modulesFile, JsonObject.stringify(root));
        } catch (IOException exception) {
            FalarisClient.LOGGER.warn("Failed to save config.", exception);
        }
    }
}
