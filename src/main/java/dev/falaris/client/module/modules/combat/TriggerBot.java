package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.Random;

public final class TriggerBot extends CombatModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum trigger distance.", 3.0, 1.0, 6.0));
    private final BooleanSetting useCooldown = setting(new BooleanSetting("Use Cooldown", "Wait for the weapon to fully charge before attacking.", true));
    private final DoubleSetting cpsMin = setting(new DoubleSetting("CPS Min", "Minimum attacks per second.", 4.0, 1.0, 15.0));
    private final DoubleSetting cpsMax = setting(new DoubleSetting("CPS Max", "Maximum attacks per second.", 8.0, 1.0, 20.0));
    private final IntegerSetting jitter = setting(new IntegerSetting("Delay Jitter", "Random extra ticks between attacks.", 4, 0, 12));
    private final ModeSetting bypass = setting(new ModeSetting("Bypass", "Anticheat bypass mode.", "Normal", "Normal", "Swing Only", "Randomized", "Legit", "Grim"));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Target players.", true));
    private final BooleanSetting hostiles = setting(new BooleanSetting("Hostiles", "Target hostile mobs.", true));
    private final BooleanSetting passives = setting(new BooleanSetting("Passives", "Target passive mobs.", false));

    private final Random random = new Random();

    public TriggerBot() {
        super("TriggerBot", "Attacks entities under the crosshair using silent rotation.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.ENTITY) return;

        EntityHitResult hit = (EntityHitResult) client.crosshairTarget;
        if (!(hit.getEntity() instanceof LivingEntity target) || !target.isAlive()) return;
        if (client.player.squaredDistanceTo(target) > range.get() * range.get()) return;
        if (!isAllowed(target)) return;

        String bypassMode = bypass.get();

        if (bypassMode.equals("Swing Only")) {
            client.player.swingHand(Hand.MAIN_HAND);
            return;
        }

        if (bypassMode.equals("Legit") || bypassMode.equals("Grim")) {
            double cps = cpsMin.get() + random.nextFloat() * (cpsMax.get() - cpsMin.get());
            int minTicks = Math.max(1, (int) Math.round(20.0 / cps));
            int extraJitter = bypassMode.equals("Grim") ? jitter.get() + 2 : jitter.get();
            if (actionReady(minTicks, extraJitter)) {
                if (random.nextFloat() < 0.10f) return;
                if (useCooldown.enabled() && client.player.getAttackCooldownProgress(0.5f) < 1.0f) return;
                CombatUtil.attack(client, target);
            }
            return;
        }

        if (bypassMode.equals("Randomized")) {
            int minTicks = Math.max(1, (int) Math.round(20.0 / cpsMax.get()));
            int maxTicks = Math.max(minTicks + 1, (int) Math.round(20.0 / cpsMin.get()) + jitter.get());
            int randomized = minTicks + random.nextInt(maxTicks - minTicks + 1);
            if (actionReady(randomized, 0)) {
                CombatUtil.attack(client, target);
            }
            return;
        }

        boolean canBypassCooldown = client.player.getAbilities().creativeMode;
        if (useCooldown.enabled() && !canBypassCooldown) {
            if (client.player.getAttackCooldownProgress(0.5f) >= 1.0f) {
                CombatUtil.attack(client, target);
            }
        } else {
            double cps = cpsMin.get() + (cpsMax.get() - cpsMin.get()) * random.nextFloat();
            int minimumTicks = Math.max(1, (int) Math.round(20.0 / cps));
            if (actionReady(minimumTicks, jitter.get())) {
                CombatUtil.attack(client, target);
            }
        }
    }

    private boolean isAllowed(LivingEntity target) {
        if (target instanceof PlayerEntity) return players.enabled();
        if (target instanceof HostileEntity) return hostiles.enabled();
        return passives.enabled();
    }
}
