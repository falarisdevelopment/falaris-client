package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;

public final class Reach extends CombatModule {
    private final DoubleSetting attackReach = setting(new DoubleSetting("Attack Reach", "Extended attack range.", 0.5, 0.0, 3.0));
    private final DoubleSetting blockReach = setting(new DoubleSetting("Block Reach", "Extended block interact range.", 0.5, 0.0, 3.0));
    private final BooleanSetting verticalCheck = setting(new BooleanSetting("Vertical Check", "Also extend vertical reach.", false));
    private final BooleanSetting onlySword = setting(new BooleanSetting("Only Sword", "Only apply when holding a sword.", false));
    private final ModeSetting bypass = setting(new ModeSetting("Bypass", "Anticheat bypass method.", "Vanilla", "Vanilla", "Vulcan", "Verus", "Grim"));

    public Reach() {
        super("Reach", "Extends the player's attack and interact reach.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
    }

    public double getAttackReach() { return 3.0 + attackReach.get(); }
    public double getBlockReach() { return 4.5 + blockReach.get(); }
    public boolean extendVertical() { return verticalCheck.enabled(); }
    public boolean onlySword() { return onlySword.enabled(); }
    public boolean isActive() { return isEnabled(); }
}
