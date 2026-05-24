package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public final class AutoDrain extends CombatModule {
    private final DoubleSetting breakRange = setting(new DoubleSetting("Break Range", "Max crystal break distance.", 3.0, 1.0, 5.0));
    private final DoubleSetting minHealth = setting(new DoubleSetting("Min Health", "Break when health below this % of max.", 0.5, 0.1, 1.0));
    private final DoubleSetting maxHealth = setting(new DoubleSetting("Max Health", "Stop breaking when health above this %.", 0.8, 0.2, 1.0));
    private final IntegerSetting cooldown = setting(new IntegerSetting("Cooldown", "Ticks between breaks.", 10, 2, 40));
    private final BooleanSetting autoSword = setting(new BooleanSetting("Auto Sword", "Switch to sword when breaking.", true));
    private final BooleanSetting onlyOwnCrystals = setting(new BooleanSetting("Only Own", "Only break crystals you placed.", false));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Break through walls.", false));
    private final BooleanSetting healMode = setting(new BooleanSetting("Heal Mode", "Break crystal for health regen.", true));

    private int tickCounter;

    public AutoDrain() {
        super("AutoDrain", "Breaks end crystals to heal or deny enemy damage.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        tickCounter = 0;
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        tickCounter++;
        if (tickCounter < cooldown.get()) return;

        float ratio = client.player.getHealth() / client.player.getMaxHealth();
        boolean shouldBreak = false;

        if (healMode.enabled() && ratio < minHealth.get()) {
            shouldBreak = true;
        }

        if (!shouldBreak && ratio > maxHealth.get()) return;

        Optional<EndCrystalEntity> crystal = CombatUtil.bestCrystal(client, breakRange.get(), throughWalls.enabled());
        if (crystal.isEmpty()) return;

        if (autoSword.enabled()) {
            int swordSlot = findBestSword(client);
            if (swordSlot != -1 && swordSlot < 9) {
                client.player.getInventory().setSelectedSlot(swordSlot);
            }
        }

        if (actionReady(1, 0)) {
            CombatUtil.attack(client, crystal.get());
            tickCounter = 0;
        }
    }

    private int findBestSword(MinecraftClient client) {
        net.minecraft.item.Item[] swords = {Items.NETHERITE_SWORD, Items.DIAMOND_SWORD, Items.IRON_SWORD, Items.STONE_SWORD};
        for (net.minecraft.item.Item sword : swords) {
            int slot = dev.falaris.client.module.modules.combat.CombatUtil.findItem(client.player, sword);
            if (slot != -1) return slot;
        }
        return -1;
    }
}
