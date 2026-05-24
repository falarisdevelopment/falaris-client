package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public final class AutoShieldDisable extends CombatModule {
    private final BooleanSetting autoSwitch = setting(new BooleanSetting("Auto Switch", "Swap to axe when target has shield.", true));
    private final BooleanSetting silentRotate = setting(new BooleanSetting("Silent Rotate", "Silently face targets.", true));
    private final BooleanSetting onlyWhenBlocking = setting(new BooleanSetting("Only When Blocking", "Only attack when target is blocking.", true));
    private final BooleanSetting requireLooking = setting(new BooleanSetting("Require Looking", "Only attack when looking at target (anti-detection).", true));
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Attack range.", 3.0, 1.0, 3.0));
    private final DoubleSetting cps = setting(new DoubleSetting("CPS", "Attacks per second.", 8.0, 1.0, 20.0));
    private final IntegerSetting jitter = setting(new IntegerSetting("Delay Jitter", "Random extra ticks.", 2, 0, 8));

    public AutoShieldDisable() {
        super("AutoShieldDisable", "Auto-swaps to axe to disable enemy shields.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        LivingEntity target = CombatUtil.bestLivingTarget(
                client, range.get(), true, true, false, false, "Angle"
        ).orElse(null);

        if (target == null) return;
        if (!(target instanceof PlayerEntity)) return;
        if (onlyWhenBlocking.enabled() && !target.isBlocking()) return;

        if (requireLooking.enabled()) {
            if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.ENTITY) return;
            EntityHitResult entityHit = (EntityHitResult) client.crosshairTarget;
            if (entityHit.getEntity() != target) return;
        }

        if (autoSwitch.enabled()) {
            int axeSlot = findBestAxe(client.player);
            if (axeSlot >= 0 && axeSlot < 9) {
                client.player.getInventory().setSelectedSlot(axeSlot);
            }
        }

        if (silentRotate.enabled()) {
            float[] rots = CombatUtil.rotationsTo(client.player, target.getEyePos());
            var rotMgr = dev.falaris.client.FalarisClient.getInstance().getRotationManager();
            rotMgr.setMaxStep(30.0f);
            rotMgr.rotateToSilent(rots[0], rots[1], 2);
        }

        int minimumTicks = Math.max(1, (int) Math.round(20.0 / cps.get()));
        if (actionReady(minimumTicks, jitter.get())) {
            CombatUtil.attack(client, target);
        }
    }

    private int findBestAxe(ClientPlayerEntity player) {
        int best = -1;
        double bestScore = -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.getItem() instanceof AxeItem) {
                double score = stack.getMaxDamage() - stack.getDamage();
                if (score > bestScore) {
                    bestScore = score;
                    best = slot;
                }
            }
        }
        return best;
    }
}
