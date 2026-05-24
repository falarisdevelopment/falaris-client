package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.Random;

public final class AutoClicker extends CombatModule {
    private final IntegerSetting minCps = setting(new IntegerSetting("Min CPS", "Minimum clicks per second.", 8, 1, 20));
    private final IntegerSetting maxCps = setting(new IntegerSetting("Max CPS", "Maximum clicks per second.", 12, 1, 20));
    private final BooleanSetting onlyTarget = setting(new BooleanSetting("Only Target", "Only click when hovering an entity.", true));
    private final BooleanSetting breakBlocks = setting(new BooleanSetting("Break Blocks", "Also click on blocks for mining.", false));
    private final ModeSetting clickMode = setting(new ModeSetting("Click Mode", "Attack pattern.", "Left", "Left", "Right", "Both"));
    private final BooleanSetting weaponOnly = setting(new BooleanSetting("Weapon Only", "Only click when holding sword/axe.", true));
    private final ModeSetting pattern = setting(new ModeSetting("Pattern", "Click pattern behavior.", "Normal", "Normal", "Blatant", "Randomized", "Legit"));
    private final DoubleSetting dropChance = setting(new DoubleSetting("Drop Chance", "Chance to skip a click (Legit).", 0.15, 0.0, 0.5));
    private final IntegerSetting burstLength = setting(new IntegerSetting("Burst Length", "Max clicks before pause (Randomized).", 6, 2, 15));
    private final IntegerSetting burstPause = setting(new IntegerSetting("Burst Pause", "Ticks between bursts (Randomized).", 3, 1, 10));

    private final Random random = new Random();
    private long nextClickTime;
    private int clicksThisBurst;
    private int pauseTicks;

    public AutoClicker() {
        super("AutoClicker", "Automatically clicks for consistent attack speed.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        nextClickTime = System.currentTimeMillis();
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null) return;

        long now = System.currentTimeMillis();
        if (now < nextClickTime) return;

        if (!client.mouse.isCursorLocked()) return;

        String p = pattern.get();
        if (p.equals("Randomized") && pauseTicks > 0) {
            pauseTicks--;
            nextClickTime = now + 50;
            return;
        }

        if (weaponOnly.enabled()) {
            var item = client.player.getMainHandStack().getItem();
            if (!isWeapon(item)) return;
        }

        boolean shouldClick = false;
        if (onlyTarget.enabled()) {
            if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
                var entity = ((EntityHitResult) client.crosshairTarget).getEntity();
                if (entity != null && entity != client.player && entity.isAlive()) {
                    shouldClick = true;
                }
            }
            if (!shouldClick && breakBlocks.enabled() && client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                shouldClick = true;
            }
        } else {
            shouldClick = true;
        }

        if (!shouldClick) return;

        // Legit: random click drops
        if (p.equals("Legit") && random.nextDouble() < dropChance.get()) {
            int delay = 1000 / (minCps.get() + random.nextInt(maxCps.get() - minCps.get() + 1));
            nextClickTime = now + Math.max(50, delay);
            return;
        }

        // Randomized: burst-based clicking
        if (p.equals("Randomized")) {
            if (clicksThisBurst >= burstLength.get()) {
                pauseTicks = burstPause.get();
                clicksThisBurst = 0;
                int delay = 1000 / (minCps.get() + random.nextInt(maxCps.get() - minCps.get() + 1));
                nextClickTime = now + Math.max(50, delay);
                return;
            }
            clicksThisBurst++;
        }

        if (clickMode.is("Left") || clickMode.is("Both")) {
            client.options.attackKey.setPressed(true);
            if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
                var entity = ((EntityHitResult) client.crosshairTarget).getEntity();
                client.interactionManager.attackEntity(client.player, entity);
            }
            client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        }
        if (clickMode.is("Right") || clickMode.is("Both")) {
            client.options.useKey.setPressed(true);
            client.interactionManager.interactItem(client.player, net.minecraft.util.Hand.MAIN_HAND);
        }

        int delay = 1000 / (minCps.get() + random.nextInt(maxCps.get() - minCps.get() + 1));
        nextClickTime = System.currentTimeMillis() + Math.max(50, delay);
    }

    private boolean isWeapon(net.minecraft.item.Item item) {
        return item == net.minecraft.item.Items.NETHERITE_SWORD || item == net.minecraft.item.Items.DIAMOND_SWORD
            || item == net.minecraft.item.Items.IRON_SWORD || item == net.minecraft.item.Items.STONE_SWORD
            || item == net.minecraft.item.Items.WOODEN_SWORD || item == net.minecraft.item.Items.GOLDEN_SWORD
            || item == net.minecraft.item.Items.NETHERITE_AXE || item == net.minecraft.item.Items.DIAMOND_AXE
            || item == net.minecraft.item.Items.IRON_AXE || item == net.minecraft.item.Items.MACE;
    }
}
