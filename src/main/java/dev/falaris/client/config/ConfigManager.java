package dev.falaris.client.config;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.alt.AltAccount;
import dev.falaris.client.module.FriendsManager;
import dev.falaris.client.module.IgnoresManager;
import dev.falaris.client.module.Module;
import dev.falaris.client.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ConfigManager {
    private final Path configDirectory;
    private final Path modulesFile;
    private final Path altsFile;
    private final Path friendsFile;
    private final Path ignoresFile;

    public ConfigManager(String modId) {
        this.configDirectory = FabricLoader.getInstance().getConfigDir().resolve(modId);
        this.modulesFile = configDirectory.resolve("modules.json");
        this.altsFile = configDirectory.resolve("alts.json");
        this.friendsFile = configDirectory.resolve("friends.json");
        this.ignoresFile = configDirectory.resolve("ignores.json");
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

    public void loadFriends(FriendsManager friendsManager) {
        try {
            if (!Files.exists(friendsFile)) return;
            String raw = Files.readString(friendsFile).trim();
            if (raw.isEmpty() || raw.equals("{}")) return;
            JsonObject root = JsonObject.parse(raw);
            root.stringValue("list").ifPresent(data -> {
                if (!data.isEmpty()) {
                    friendsManager.setFriends(List.of(data.split(",")));
                }
            });
        } catch (IOException e) {
            FalarisClient.LOGGER.warn("Failed to load friends.", e);
        }
    }

    public void saveFriends(FriendsManager friendsManager) {
        try {
            Files.createDirectories(configDirectory);
            Map<String, Object> root = new java.util.LinkedHashMap<>();
            root.put("list", String.join(",", friendsManager.getAll()));
            Files.writeString(friendsFile, JsonObject.stringify(root));
        } catch (IOException e) {
            FalarisClient.LOGGER.warn("Failed to save friends.", e);
        }
    }

    public List<AltAccount> loadAlts() {
        try {
            Files.createDirectories(configDirectory);

            if (!Files.exists(altsFile)) {
                return List.of();
            }

            String json = Files.readString(altsFile);
            JsonObject root = JsonObject.parse(json);
            JsonObject alts = root.object("alts").orElseGet(JsonObject::new);

            List<AltAccount> result = new ArrayList<>();
            for (Map.Entry<String, Object> entry : alts.values().entrySet()) {
                Object rawValue = entry.getValue();
                if (!(rawValue instanceof Map<?, ?> map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                JsonObject altObject = new JsonObject((Map<String, Object>) map);
                String username = altObject.stringValue("username").orElse("");
                String uuid = altObject.stringValue("uuid").orElse("");
                boolean favorite = altObject.booleanValue("favorite").orElse(false);
                long lastUsed = altObject.longValue("last_used").orElse(0L);
                if (username.isEmpty()) {
                    continue;
                }
                result.add(new AltAccount(entry.getKey(), username, uuid, favorite, lastUsed));
            }
            return result;
        } catch (IOException | IllegalArgumentException exception) {
            FalarisClient.LOGGER.warn("Failed to load alts, using empty list.", exception);
            return List.of();
        }
    }

    public void saveAlts(List<AltAccount> alts) {
        try {
            Files.createDirectories(configDirectory);

            Map<String, Object> altsData = new LinkedHashMap<>();
            for (AltAccount alt : alts) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("username", alt.getUsername());
                if (!alt.getUuid().isBlank()) {
                    entry.put("uuid", alt.getUuid());
                }
                entry.put("favorite", alt.isFavorite());
                entry.put("last_used", alt.getLastUsed());
                altsData.put(alt.getId(), entry);
            }

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("alts", altsData);

            Files.writeString(altsFile, JsonObject.stringify(root));
        } catch (IOException exception) {
            FalarisClient.LOGGER.warn("Failed to save alts.", exception);
        }
    }

    public void loadIgnores(IgnoresManager ignoresManager) {
        try {
            if (!Files.exists(ignoresFile)) return;
            String raw = Files.readString(ignoresFile).trim();
            if (raw.isEmpty() || raw.equals("{}")) return;
            JsonObject root = JsonObject.parse(raw);
            root.stringValue("list").ifPresent(data -> {
                if (!data.isEmpty()) {
                    ignoresManager.setIgnores(List.of(data.split(",")));
                }
            });
        } catch (IOException e) {
            FalarisClient.LOGGER.warn("Failed to load ignores.", e);
        }
    }

    public void saveIgnores(IgnoresManager ignoresManager) {
        try {
            Files.createDirectories(configDirectory);
            Map<String, Object> root = new java.util.LinkedHashMap<>();
            root.put("list", String.join(",", ignoresManager.getAll()));
            Files.writeString(ignoresFile, JsonObject.stringify(root));
        } catch (IOException e) {
            FalarisClient.LOGGER.warn("Failed to save ignores.", e);
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
