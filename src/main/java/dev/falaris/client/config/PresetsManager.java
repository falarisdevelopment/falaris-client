package dev.falaris.client.config;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.module.Module;
import dev.falaris.client.module.ModuleManager;
import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import dev.falaris.client.setting.Setting;
import dev.falaris.client.setting.StringSetting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class PresetsManager {
    private final Path presetsFile;
    private Map<String, Map<String, Map<String, Object>>> presets = new LinkedHashMap<>();

    public PresetsManager(String modId) {
        this.presetsFile = FabricLoader.getInstance().getConfigDir().resolve(modId).resolve("presets.json");
    }

    public void load() {
        try {
            if (!Files.exists(presetsFile)) {
                seedDefaults();
                return;
            }
            String json = Files.readString(presetsFile);
            JsonObject root = JsonObject.parse(json);
            for (Map.Entry<String, Object> entry : root.values().entrySet()) {
                if (entry.getValue() instanceof Map<?, ?> presetMap) {
                    Map<String, Map<String, Object>> parsed = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> moduleEntry : presetMap.entrySet()) {
                        if (moduleEntry.getKey() instanceof String moduleId && moduleEntry.getValue() instanceof Map<?, ?> settingsMap) {
                            Map<String, Object> settings = new LinkedHashMap<>();
                            settingsMap.forEach((k, v) -> settings.put(String.valueOf(k), v));
                            parsed.put(moduleId, settings);
                        }
                    }
                    presets.put(entry.getKey(), parsed);
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            FalarisClient.LOGGER.warn("Failed to load presets.", e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(presetsFile.getParent());
            Map<String, Object> root = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Map<String, Object>>> presetEntry : presets.entrySet()) {
                root.put(presetEntry.getKey(), presetEntry.getValue());
            }
            Files.writeString(presetsFile, JsonObject.stringify(root));
        } catch (IOException e) {
            FalarisClient.LOGGER.warn("Failed to save presets.", e);
        }
    }

    public void savePreset(String name, ModuleManager moduleManager) {
        Map<String, Map<String, Object>> presetData = new LinkedHashMap<>();
        for (Module module : moduleManager.getModules()) {
            Map<String, Object> settings = new LinkedHashMap<>();
            settings.put("enabled", module.isEnabled());
            settings.put("key", module.getKeyCode());
            for (Setting<?> setting : module.getSettings()) {
                Map<String, Object> serialized = serializeSetting(setting);
                if (serialized != null) {
                    settings.put(setting.getName(), serialized);
                }
            }
            if (!settings.isEmpty()) {
                presetData.put(module.getId(), settings);
            }
        }
        presets.put(name, presetData);
        save();
    }

    public void loadPreset(String name, ModuleManager moduleManager) {
        Map<String, Map<String, Object>> presetData = presets.get(name);
        if (presetData == null) return;
        for (Module module : moduleManager.getModules()) {
            Map<String, Object> settings = presetData.get(module.getId());
            if (settings == null) continue;
            Object enabledVal = settings.get("enabled");
            if (enabledVal instanceof Boolean enabled) {
                module.setEnabled(enabled);
            }
            Object keyVal = settings.get("key");
            if (keyVal instanceof Number keyNum) {
                module.setKeyCode(keyNum.intValue());
            }
            for (Setting<?> setting : module.getSettings()) {
                Object raw = settings.get(setting.getName());
                if (raw instanceof Map<?, ?> settingMap) {
                    Map<String, Object> deserialized = new LinkedHashMap<>();
                    settingMap.forEach((k, v) -> deserialized.put(String.valueOf(k), v));
                    deserializeSetting(setting, deserialized);
                }
            }
        }
    }

    public java.util.Set<String> getPresetNames() {
        return presets.keySet();
    }

    public boolean hasPreset(String name) {
        return presets.containsKey(name);
    }

    public void deletePreset(String name) {
        presets.remove(name);
        save();
    }

    private static Map<String, Object> serializeSetting(Setting<?> setting) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (setting instanceof BooleanSetting bool) {
            data.put("type", "boolean");
            data.put("value", bool.get());
        } else if (setting instanceof DoubleSetting dbl) {
            data.put("type", "double");
            data.put("value", dbl.get());
        } else if (setting instanceof IntegerSetting integer) {
            data.put("type", "integer");
            data.put("value", integer.get());
        } else if (setting instanceof ModeSetting mode) {
            data.put("type", "mode");
            data.put("value", mode.get());
        } else if (setting instanceof StringSetting str) {
            data.put("type", "string");
            data.put("value", str.get());
        } else {
            return null;
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private static <T> void deserializeSetting(Setting<T> setting, Map<String, Object> data) {
        String type = (String) data.get("type");
        if (type == null) return;
        Object value = data.get("value");
        if (value == null) return;
        switch (type) {
            case "boolean" -> {
                if (setting instanceof BooleanSetting bool && value instanceof Boolean b) {
                    bool.set(b);
                }
            }
            case "double" -> {
                if (setting instanceof DoubleSetting dbl && value instanceof Number n) {
                    dbl.set(n.doubleValue());
                }
            }
            case "integer" -> {
                if (setting instanceof IntegerSetting integer && value instanceof Number n) {
                    integer.set(n.intValue());
                }
            }
            case "mode" -> {
                if (setting instanceof ModeSetting mode && value instanceof String s) {
                    mode.set(s);
                }
            }
            case "string" -> {
                if (setting instanceof StringSetting str && value instanceof String s) {
                    str.set(s);
                }
            }
        }
    }

    private void seedDefaults() {
        Map<String, String[]> defaultSets = new LinkedHashMap<>();
        defaultSets.put("Sword", new String[]{
            "killaura", "triggerbot", "reach", "hitboxes",
            "autosprint", "nofall", "velocity",
            "autototem", "autoarmor", "autosword", "fastuse",
            "arraylist", "armorhud", "esp", "tracers", "fullbright",
            "nametags"
        });
        defaultSets.put("Crystal", new String[]{
            "killaura", "crystalaura", "reach", "hitboxes",
            "velocity", "nofall", "autosprint",
            "autototem", "autoarmor", "fastuse",
            "arraylist", "armorhud", "esp", "tracers", "fullbright"
        });
        defaultSets.put("Pot", new String[]{
            "killaura", "triggerbot", "reach", "hitboxes",
            "autosprint", "nofall", "velocity",
            "autototem", "autoarmor", "autosword", "fastuse", "antihunger",
            "arraylist", "armorhud", "esp", "tracers", "fullbright"
        });
        defaultSets.put("Nethpot", new String[]{
            "killaura", "triggerbot", "reach", "hitboxes",
            "autosprint", "nofall", "velocity",
            "autototem", "autoarmor", "autosword", "fastuse", "antihunger",
            "arraylist", "armorhud", "esp", "tracers", "fullbright",
            "elytrafly"
        });
        defaultSets.put("Mace", new String[]{
            "automace", "reach", "hitboxes",
            "autosprint", "nofall", "velocity",
            "autototem", "autoarmor",
            "arraylist", "armorhud", "esp", "tracers", "fullbright"
        });
        defaultSets.put("Mace (Ghost)", new String[]{
            "automace", "maceassist", "reach",
            "velocity", "nofall", "autosprint",
            "autototem", "autoarmor", "keepsprint", "cheatprotector",
            "arraylist", "nametags", "esp", "fullbright", "nameprotect",
            "antiblind", "nopush", "noslowdown", "autorespawn"
        });
        defaultSets.put("Crystal (Ghost)", new String[]{
            "crystalaura", "anchoraura", "offhandswap", "autocity", "reach", "hitboxes",
            "velocity", "nofall", "autosprint",
            "autototem", "autoarmor", "fastuse", "keepsprint", "cheatprotector",
            "arraylist", "armorhud", "esp", "tracers", "fullbright", "nameprotect",
            "antiblind", "nopush", "noslowdown"
        });
        defaultSets.put("Sword (Ghost)", new String[]{
            "killaura", "triggerbot", "aimassist", "reach", "hitboxes",
            "velocity", "nofall", "autosprint",
            "autototem", "autoarmor", "autosword", "fastuse", "keepsprint", "criticals", "cheatprotector",
            "arraylist", "armorhud", "esp", "nametags", "fullbright", "nameprotect",
            "autoshielddisable", "antiblind", "noslowdown"
        });
        defaultSets.put("Anti-Cheat Bypass", new String[]{
            "velocity", "killaura", "triggerbot", "aimassist", "reach",
            "nofall", "autosprint", "keepsprint", "criticals",
            "autototem", "autoarmor", "fastuse", "noslowdown",
            "arraylist", "armorhud", "esp", "fullbright", "nameprotect",
            "antiblind", "nopush", "norender", "antihunger"
        });
        defaultSets.put("Vulcan", new String[]{
            "killaura", "triggerbot", "aimassist", "reach", "hitboxes",
            "velocity", "nofall", "autosprint", "keepsprint",
            "autototem", "autoarmor", "autosword", "fastuse", "noslowdown",
            "arraylist", "armorhud", "esp", "nametags", "fullbright",
            "nameprotect", "antiblind", "nopush", "antihunger", "autorespawn"
        });
        defaultSets.put("Closet", new String[]{
            "killaura", "triggerbot", "aimassist", "reach", "hitboxes",
            "velocity", "nofall", "autosprint", "noslowdown",
            "autototem", "autoarmor",
            "arraylist", "armorhud", "nametags", "inventoryhud", "fullbright",
            "safewalk"
        });
        defaultSets.put("Anarchy", new String[]{
            "killaura", "crystalaura", "anchoraura", "automace", "reach", "hitboxes",
            "velocity", "nofall", "autosprint", "noslowdown", "criticals",
            "autototem", "autoarmor", "triggerbot",
            "esp", "tracers", "nametags", "arraylist", "armorhud", "fullbright",
            "elytrafly", "antibot"
        });
        defaultSets.put("Blatant", new String[]{
            "killaura", "crystalaura", "anchoraura", "automace", "bedaura",
            "autocity", "offhandswap", "reach", "hitboxes",
            "velocity", "nofall", "autosprint", "noslowdown", "criticals",
            "autototem", "autoarmor", "autotool",
            "flight", "elytrafly", "scaffold",
            "esp", "tracers", "nametags", "arraylist", "armorhud", "fullbright",
            "blockesp", "nuker", "antibot"
        });
        defaultSets.put("Vulcan (Bypass)", new String[]{
            "killaura", "triggerbot", "aimassist", "reach", "hitboxes",
            "velocity", "nofall", "autosprint", "keepsprint",
            "autototem", "autoarmor", "autosword", "fastuse", "noslowdown",
            "arraylist", "armorhud", "esp", "nametags", "fullbright",
            "nameprotect", "antiblind", "nopush", "antihunger", "autorespawn",
            "cheatprotector"
        });
        defaultSets.put("SMP", new String[]{
            "killaura", "triggerbot", "aimassist", "reach", "hitboxes",
            "autosprint", "nofall", "velocity",
            "autototem", "autoarmor", "autotool", "autosword",
            "autopearl", "autopearlcatch", "cheststealer", "fastuse", "antihunger",
            "arraylist", "armorhud", "esp", "blockesp", "nametags", "fullbright",
            "scaffold", "nopush", "antibot", "autoreconnect"
        });
        defaultSets.put("KitPVP", new String[]{
            "killaura", "triggerbot", "aimassist", "reach", "hitboxes",
            "velocity", "nofall", "autosprint", "keepsprint", "wtap",
            "autototem", "autoarmor", "autosword", "fastuse", "criticals",
            "arraylist", "armorhud", "esp", "nametags", "fullbright",
            "autoshielddisable", "noslowdown"
        });
        defaultSets.put("NoDebuff (Pot)", new String[]{
            "killaura", "triggerbot", "reach", "hitboxes",
            "velocity", "nofall", "autosprint", "keepsprint", "criticals",
            "autototem", "autoarmor", "autosword", "fastuse", "antihunger",
            "arraylist", "armorhud", "esp", "tracers", "nametags", "fullbright",
            "noslowdown", "antiblind"
        });
        defaultSets.put("SpearMace", new String[]{
            "automace", "autoelytraspear", "shortwindcharge", "autowindcharge",
            "autopearlcatch", "breachswap", "maceassist", "reach", "hitboxes",
            "velocity", "nofall", "autosprint", "autoelytrabounce",
            "autototem", "autoarmor", "autosword",
            "arraylist", "armorhud", "nametags", "fullbright",
            "elytrafly", "elytraplus",
            "damageindicator", "noslowdown"
        });
        defaultSets.put("DonutSMP", new String[]{
            "killaura", "crystalaura", "anchoraura", "reach", "hitboxes",
            "velocity", "nofall", "autosprint",
            "autototem", "autoarmor", "autotool", "autosword",
            "arraylist", "armorhud", "blockesp", "esp", "nametags", "fullbright",
            "antibot", "cheatprotector", "norender", "antiblind",
            "blink", "scaffold"
        });

        FalarisClient client = FalarisClient.getInstance();
        for (Map.Entry<String, String[]> entry : defaultSets.entrySet()) {
            Map<String, Map<String, Object>> presetData = new LinkedHashMap<>();
            for (Module module : client.getModuleManager().getModules()) {
                boolean shouldEnable = false;
                for (String id : entry.getValue()) {
                    if (module.getId().equals(id)) {
                        shouldEnable = true;
                        break;
                    }
                }
                Map<String, Object> settings = new LinkedHashMap<>();
                settings.put("enabled", shouldEnable);
                settings.put("key", 0);

                // Set bypass settings on ghost/bypass presets
                if (entry.getKey().contains("Ghost") || entry.getKey().contains("Bypass") || entry.getKey().contains("Vulcan")) {
                    for (Setting<?> setting : module.getSettings()) {
                        String name = setting.getName();
                        if (name.equals("Bypass")) {
                            Map<String, Object> bypassVal = new LinkedHashMap<>();
                            bypassVal.put("type", "mode");
                            bypassVal.put("value", entry.getKey().contains("Vulcan") ? "Legit" : "Grim");
                            settings.put(name, bypassVal);
                        } else if (name.equals("Mode") && module.getId().equals("velocity")) {
                            Map<String, Object> modeVal = new LinkedHashMap<>();
                            modeVal.put("type", "mode");
                            modeVal.put("value", "Reduce");
                            settings.put(name, modeVal);
                        } else if (name.equals("Horizontal") && module.getId().equals("velocity")) {
                            Map<String, Object> hVal = new LinkedHashMap<>();
                            hVal.put("type", "double");
                            hVal.put("value", entry.getKey().contains("Vulcan") ? 0.80 : 0.85);
                            settings.put(name, hVal);
                        } else if (name.equals("Vertical") && module.getId().equals("velocity")) {
                            Map<String, Object> vVal = new LinkedHashMap<>();
                            vVal.put("type", "double");
                            vVal.put("value", entry.getKey().contains("Vulcan") ? 0.80 : 0.85);
                            settings.put(name, vVal);
                        } else if (name.equals("Range") && module.getId().equals("reach")) {
                            Map<String, Object> rVal = new LinkedHashMap<>();
                            rVal.put("type", "double");
                            rVal.put("value", 3.0);
                            settings.put(name, rVal);
                        } else if (name.equals("Silent Rotate")) {
                            Map<String, Object> srVal = new LinkedHashMap<>();
                            srVal.put("type", "boolean");
                            srVal.put("value", true);
                            settings.put(name, srVal);
                        } else if (name.equals("CPS") || name.equals("Max CPS")) {
                            Map<String, Object> cpsVal = new LinkedHashMap<>();
                            cpsVal.put("type", "double");
                            cpsVal.put("value", entry.getKey().contains("Vulcan") ? 10.0 : 12.0);
                            settings.put(name, cpsVal);
                        } else if (name.equals("CPS Min")) {
                            Map<String, Object> cpsMinVal = new LinkedHashMap<>();
                            cpsMinVal.put("type", "double");
                            cpsMinVal.put("value", entry.getKey().contains("Vulcan") ? 6.0 : 8.0);
                            settings.put(name, cpsMinVal);
                        } else if (name.equals("CPS Max")) {
                            Map<String, Object> cpsMaxVal = new LinkedHashMap<>();
                            cpsMaxVal.put("type", "double");
                            cpsMaxVal.put("value", entry.getKey().contains("Vulcan") ? 10.0 : 12.0);
                            settings.put(name, cpsMaxVal);
                        } else if (name.equals("Auto Jump")) {
                            Map<String, Object> ajVal = new LinkedHashMap<>();
                            ajVal.put("type", "boolean");
                            ajVal.put("value", false);
                            settings.put(name, ajVal);
                        } else if (name.equals("Rotation Speed")) {
                            Map<String, Object> rsVal = new LinkedHashMap<>();
                            rsVal.put("type", "double");
                            rsVal.put("value", entry.getKey().contains("Vulcan") ? 15.0 : 20.0);
                            settings.put(name, rsVal);
                        }
                    }
                }
                presetData.put(module.getId(), settings);
            }
            presets.put(entry.getKey(), presetData);
        }
        save();
    }
}
