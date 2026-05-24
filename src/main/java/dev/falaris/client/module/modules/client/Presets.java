package dev.falaris.client.module.modules.client;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.config.PresetsManager;
import dev.falaris.client.module.Category;
import dev.falaris.client.module.Module;
import dev.falaris.client.setting.ModeSetting;
import dev.falaris.client.setting.StringSetting;
import dev.falaris.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.Set;

public final class Presets extends Module {
    private final ModeSetting presetSelect = setting(new ModeSetting("Load Preset", "Select a preset to load.", "None", "None"));
    private final StringSetting saveName = setting(new StringSetting("Save Name", "Name to save current config as.", ""));
    private final BooleanSetting save = setting(new BooleanSetting("Save", "Save current config as the named preset.", false));
    private final BooleanSetting delete = setting(new BooleanSetting("Delete", "Delete the selected preset.", false));

    public Presets() {
        super("Presets", "Save and load full module configurations for different scenarios.", Category.CLIENT);
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        FalarisClient client = FalarisClient.getInstance();
        PresetsManager pm = client.getPresetsManager();
        pm.load();
        refreshModes(pm);
        if (delete.enabled()) {
            String selected = presetSelect.get();
            if (!selected.equals("None") && pm.hasPreset(selected)) {
                pm.deletePreset(selected);
                pm.save();
                msg("Deleted preset: " + selected);
                presetSelect.set("None");
                refreshModes(pm);
                delete.set(false);
            }
        }
        if (save.enabled()) {
            String name = saveName.get().trim();
            if (!name.isEmpty()) {
                pm.savePreset(name, client.getModuleManager());
                msg("Saved preset: " + name);
                refreshModes(pm);
                if (!presetSelect.modes().contains(name)) {
                    presetSelect.set(name);
                }
                saveName.set("");
                save.set(false);
            }
        }
        String selected = presetSelect.get();
        if (!selected.equals("None") && pm.hasPreset(selected)) {
            pm.loadPreset(selected, client.getModuleManager());
            msg("Loaded preset: " + selected);
        }
        setEnabled(false);
    }

    private void refreshModes(PresetsManager pm) {
        Set<String> names = pm.getPresetNames();
        String[] modes = new String[names.size() + 1];
        modes[0] = "None";
        int i = 1;
        for (String name : names) {
            modes[i++] = name;
        }
        presetSelect.setModes(modes);
        if (!presetSelect.modes().contains(presetSelect.get())) {
            presetSelect.set("None");
        }
    }

    private String chooseDefault() {
        return "None";
    }

    private void msg(String text) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player != null) {
            c.player.sendMessage(Text.literal("§7[§bfalaris§7] §f" + text), false);
        }
    }
}
