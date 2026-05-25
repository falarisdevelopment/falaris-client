package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;

import java.util.Random;

public final class AutoSlam extends CombatModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Attack mode.", "Slam", "Slam", "Axe Stun", "Axe Slam"));
    private final DoubleSetting minFallDist = setting(new DoubleSetting("Min Fall Dist", "Minimum fall distance for mace bonus.", 3.0, 1.0, 10.0));
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Attack range.", 3.0, 1.0, 3.5));
    private final BooleanSetting autoSwitch = setting(new BooleanSetting("Auto Switch", "Swap to mace/axe.", true));
    private final BooleanSetting onlyWhenBlocking = setting(new BooleanSetting("Only When Blocking", "Only attack blocking targets.", false));
    private final BooleanSetting requireBelow = setting(new BooleanSetting("Require Below", "Only targets below you.", true));
    private final IntegerSetting verticalRange = setting(new IntegerSetting("Vertical Range", "Max Y difference.", 3, 1, 6));
    private final IntegerSetting stunSwitchDelay = setting(new IntegerSetting("Stun Switch Delay", "Ticks between axe stun and mace.", 5, 1, 20));
    private final BooleanSetting silentRotate = setting(new BooleanSetting("Silent Rotate", "Face target.", true));
    private final DoubleSetting rotationSpeed = setting(new DoubleSetting("Rotation Speed", "Rotation speed.", 20.0, 1.0, 60.0));
    private final ModeSetting bypass = setting(new ModeSetting("Bypass", "Anti-cheat.", "Vanilla", "Vanilla", "Vanilla", "Ghost"));

    private boolean stunned;
    private int stunTicker;
    private final Random random = new Random();

    public AutoSlam() {
        super("AutoSlam", "Slams targets with mace; Axe Stun mode uses axe shield-stun then mace.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        stunned = false;
        stunTicker = 0;
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        boolean isFalling = !client.player.isOnGround() && client.player.getVelocity().y < -0.1;
        if (mode.is("Slam") || mode.is("Axe Slam")) {
            if (!isFalling && client.player.fallDistance < minFallDist.get()) return;
        }

        LivingEntity target = CombatUtil.bestLivingTarget(client, range.get(), true, true, false, false, "Distance").orElse(null);
        if (target == null) return;
        if (onlyWhenBlocking.enabled() && !target.isBlocking()) return;

        if (requireBelow.enabled()) {
            double dy = target.getY() - client.player.getY();
            if (dy > 0 || Math.abs(dy) > verticalRange.get()) return;
        }

        boolean isGhost = bypass.is("Ghost");

        // Rotate toward target
        if (silentRotate.enabled()) {
            float[] rots = CombatUtil.rotationsTo(client.player, target.getEyePos());
            rotations().setMaxStep(rotationSpeed.get().floatValue());
            int ticks = isGhost ? 2 + random.nextInt(2) : 1;
            rotations().rotateToSilent(rots[0], rots[1], ticks);
        }

        // Axe stun logic
        if (mode.is("Axe Stun") || mode.is("Axe Slam")) {
            if (!stunned && target.isBlocking()) {
                if (autoSwitch.enabled()) {
                    int axeSlot = findAxe(client);
                    if (axeSlot >= 0 && axeSlot < 9) {
                        client.player.getInventory().setSelectedSlot(axeSlot);
                    }
                }
                CombatUtil.attack(client, target);
                stunned = true;
                stunTicker = 0;
                return;
            }

            if (stunned) {
                stunTicker++;
                if (stunTicker >= stunSwitchDelay.get()) {
                    stunned = false;
                    if (mode.is("Axe Stun")) return;
                    if (autoSwitch.enabled()) {
                        int maceSlot = findMace(client);
                        if (maceSlot >= 0 && maceSlot < 9) {
                            client.player.getInventory().setSelectedSlot(maceSlot);
                        }
                    }
                } else {
                    return;
                }
            }
        }

        if (!mode.is("Axe Stun") && autoSwitch.enabled()) {
            boolean alreadyMace = client.player.getMainHandStack().isOf(Items.MACE);
            if (!alreadyMace) {
                int maceSlot = findMace(client);
                if (maceSlot >= 0 && maceSlot < 9) {
                    client.player.getInventory().setSelectedSlot(maceSlot);
                }
            }
        }

        int delay = isGhost ? 1 + random.nextInt(2) : 0;
        if (actionReady(1 + delay, 0)) {
            CombatUtil.attack(client, target);
        }
    }

    private int findMace(MinecraftClient client) {
        for (int slot = 0; slot < 9; slot++) {
            if (client.player.getInventory().getStack(slot).isOf(Items.MACE)) return slot;
        }
        return -1;
    }

    private int findAxe(MinecraftClient client) {
        int best = -1;
        double bestScore = -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (stack.getItem() instanceof AxeItem) {
                double score = stack.getMaxDamage() - stack.getDamage();
                if (score > bestScore) { bestScore = score; best = slot; }
            }
        }
        return best;
    }
}
