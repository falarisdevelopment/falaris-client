package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public final class Reach extends CombatModule {
    private final DoubleSetting attackReach = setting(new DoubleSetting("Attack Reach", "Extended attack range.", 0.5, 0.0, 3.0));
    private final DoubleSetting blockReach = setting(new DoubleSetting("Block Reach", "Extended block interact range.", 0.5, 0.0, 3.0));
    private final BooleanSetting verticalCheck = setting(new BooleanSetting("Vertical Check", "Also extend vertical reach.", false));
    private final BooleanSetting onlySword = setting(new BooleanSetting("Only Sword", "Only apply when holding a sword.", false));
    private final ModeSetting bypass = setting(new ModeSetting("Bypass", "Anticheat bypass method.", "Vanilla", "Vanilla", "Vulcan", "Verus", "Grim"));

    private final Random random = new Random();
    private double gaussReach = 3.0;

    public Reach() {
        super("Reach", "Extends the player's attack and interact reach.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null) return;
        try {
            var attr = client.player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.BLOCK_INTERACTION_RANGE);
            if (attr != null) attr.setBaseValue(getBlockReach());
        } catch (Exception ignored) {}
        if (bypass.is("Grim")) {
            double target = getCappedReach();
            gaussReach += (random.nextGaussian() * 0.01);
            gaussReach = MathHelper.clamp(gaussReach, 3.0, target);
        }
    }

    public double getAttackReach() {
        if (bypass.is("Grim")) return gaussReach;
        double base = 3.0 + attackReach.get();
        if (bypass.is("Vulcan") && base > 3.3) return 3.3;
        if (bypass.is("Verus") && base > 3.5) return 3.5;
        return base;
    }

    private double getCappedReach() {
        double base = 3.0 + attackReach.get();
        return Math.min(base, 3.05);
    }

    public double getBlockReach() { return 4.5 + blockReach.get(); }
    public boolean extendVertical() { return verticalCheck.enabled(); }
    public boolean onlySword() { return onlySword.enabled(); }
    public boolean isActive() { return isEnabled(); }
}
