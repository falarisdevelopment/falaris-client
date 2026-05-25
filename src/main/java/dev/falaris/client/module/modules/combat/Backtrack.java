package dev.falaris.client.module.modules.combat;

import dev.falaris.client.backtrack.BacktrackManager;
import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;

public final class Backtrack extends CombatModule {
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Milliseconds to backtrack.", 200, 50, 1000));
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Backtrack mode.", "Simple", "Simple", "Advanced"));
    private final BooleanSetting render = setting(new BooleanSetting("Render", "Show backtracked entity positions.", true));

    public Backtrack() {
        super("Backtrack", "Attacks entities at their past position (delayed).");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
    }

    public int getDelayMs() { return delay.get(); }
    public boolean shouldRender() { return render.enabled(); }
}
