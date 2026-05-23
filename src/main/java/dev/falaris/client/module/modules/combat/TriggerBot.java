package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public final class TriggerBot extends CombatModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum trigger distance.", 4.5, 1.0, 6.0));
    private final BooleanSetting useCooldown = setting(new BooleanSetting("Use Cooldown", "Wait for the weapon to fully charge before attacking.", true));
    private final DoubleSetting cps = setting(new DoubleSetting("CPS", "Average attacks per second.", 10.0, 1.0, 20.0));
    private final IntegerSetting jitter = setting(new IntegerSetting("Delay Jitter", "Random extra ticks between attacks.", 2, 0, 8));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Target players.", true));
    private final BooleanSetting hostiles = setting(new BooleanSetting("Hostiles", "Target hostile mobs.", true));
    private final BooleanSetting passives = setting(new BooleanSetting("Passives", "Target passive mobs.", false));

    public TriggerBot() {
        super("TriggerBot", "Attacks valid entities under the crosshair.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.ENTITY) {
            return;
        }

        EntityHitResult hit = (EntityHitResult) client.crosshairTarget;
        if (!(hit.getEntity() instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }
        if (client.player.squaredDistanceTo(target) > range.get() * range.get()) {
            return;
        }
        if (!isAllowed(target)) {
            return;
        }

        if (useCooldown.enabled()) {
            if (client.player.getAttackCooldownProgress(0.5f) >= 1.0f) {
                CombatUtil.attack(client, target);
            }
        } else {
            int minimumTicks = Math.max(1, (int) Math.round(20.0 / cps.get()));
            if (actionReady(minimumTicks, jitter.get())) {
                CombatUtil.attack(client, target);
            }
        }
    }

    private boolean isAllowed(LivingEntity target) {
        if (target instanceof PlayerEntity) {
            return players.enabled();
        }
        if (target instanceof HostileEntity) {
            return hostiles.enabled();
        }
        return passives.enabled();
    }
}
