package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.entity.LivingEntity;

public final class AutoSwapMace extends CombatModule {
    private final DoubleSetting fallThreshold = setting(new DoubleSetting("Fall Threshold", "Minimum fall distance to swap to mace.", 3.0, 1.0, 10.0));
    private final ModeSetting swapBack = setting(new ModeSetting("Swap Back", "When to swap back to previous slot.", "Never", "Never", "After Attack", "On Landing"));
    private final IntegerSetting swapBackDelay = setting(new IntegerSetting("Swap Back Delay", "Ticks after swap before switching back.", 5, 0, 20));
    private final BooleanSetting requireTarget = setting(new BooleanSetting("Require Target", "Only swap when a target is nearby.", true));
    private final DoubleSetting targetRange = setting(new DoubleSetting("Target Range", "Range to detect nearby targets.", 6.0, 2.0, 12.0));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Target players.", true));
    private final BooleanSetting hostiles = setting(new BooleanSetting("Hostiles", "Target hostile mobs.", false));
    private final BooleanSetting passives = setting(new BooleanSetting("Passives", "Target passive mobs.", false));
    private final IntegerSetting cooldown = setting(new IntegerSetting("Cooldown", "Ticks before next swap attempt.", 10, 0, 40));

    private int previousSlot = -1;
    private int swapTimer;
    private int cooldownTimer;
    private boolean swappedThisFall;

    public AutoSwapMace() {
        super("AutoSwapMace", "Automatically swaps to a mace when falling for slam attacks.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        previousSlot = -1;
        swapTimer = 0;
        cooldownTimer = 0;
        swappedThisFall = false;
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (cooldownTimer < cooldown.get()) { cooldownTimer++; return; }

        boolean isFalling = !client.player.isOnGround() && client.player.getVelocity().y < -0.1;
        boolean onGround = client.player.isOnGround();

        if (onGround) {
            if (swapBack.is("On Landing") && swappedThisFall && swapTimer <= 0) {
                restoreSlot(client);
            }
            swappedThisFall = false;
            swapTimer = 0;
        }

        if (swapTimer > 0) swapTimer--;

        if (!isFalling || client.player.fallDistance < fallThreshold.get()) {
            if (swapBack.is("After Attack") && swappedThisFall && swapTimer <= 0) {
                if (onGround) restoreSlot(client);
            }
            return;
        }

        if (requireTarget.enabled()) {
            boolean hasTarget = client.world.getEntitiesByClass(
                LivingEntity.class,
                client.player.getBoundingBox().expand(targetRange.get()),
                e -> e != client.player && e.isAlive() && CombatUtil.isValidTarget(
                    client.player, e, targetRange.get(),
                    players.enabled(), hostiles.enabled(), passives.enabled(), false
                )
            ).stream().findAny().isPresent();
            if (!hasTarget) return;
        }

        if (client.player.getMainHandStack().isOf(Items.MACE)) {
            swappedThisFall = true;
            return;
        }

        int maceSlot = findMace(client);
        if (maceSlot < 0) return;

        if (!swappedThisFall) {
            previousSlot = client.player.getInventory().getSelectedSlot();
        }
        client.player.getInventory().setSelectedSlot(maceSlot);
        swappedThisFall = true;
        swapTimer = swapBackDelay.get();
        cooldownTimer = 0;
    }

    private void restoreSlot(MinecraftClient client) {
        if (previousSlot >= 0 && previousSlot < 9 && swappedThisFall) {
            client.player.getInventory().setSelectedSlot(previousSlot);
            previousSlot = -1;
            swappedThisFall = false;
        }
    }

    private int findMace(MinecraftClient client) {
        for (int slot = 0; slot < 9; slot++) {
            if (client.player.getInventory().getStack(slot).isOf(Items.MACE)) {
                return slot;
            }
        }
        return -1;
    }
}
