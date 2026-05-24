package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

import java.util.Random;

public final class AutoWindCharge extends CombatModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "When to use wind charges.", "On Jump", "On Jump", "Always", "Manual"));
    private final BooleanSetting requireMace = setting(new BooleanSetting("Require Mace", "Only use wind charges while holding a mace.", true));
    private final BooleanSetting autoSwitch = setting(new BooleanSetting("Auto Switch", "Auto-switch to wind charge in hotbar.", true));
    private final IntegerSetting useDelay = setting(new IntegerSetting("Use Delay", "Ticks between wind charge uses.", 10, 1, 40));
    private final IntegerSetting jitter = setting(new IntegerSetting("Jitter", "Random extra ticks between uses.", 3, 0, 10));

    private final BooleanSetting humanFailure = setting(new BooleanSetting("Human Failure", "Simulate human-like imperfections.", true));
    private final DoubleSetting failureChance = setting(new DoubleSetting("Failure Chance", "Chance per activation to skip (0-1).", 0.05, 0.0, 0.5));
    private final IntegerSetting aimVariance = setting(new IntegerSetting("Aim Variance", "Random pitch offset when using (degrees).", 5, 0, 30));
    private final IntegerSetting holdVariance = setting(new IntegerSetting("Hold Variance", "Extra ticks wind charge is 'held' before use.", 2, 0, 10));
    private final BooleanSetting doubleClickChance = setting(new BooleanSetting("Double Click Chance", "Small chance to use twice rapidly.", false));
    private final DoubleSetting doubleClickOdds = setting(new DoubleSetting("Double Click Odds", "Chance of double-click (0-1).", 0.03, 0.0, 0.2));

    private boolean wasOnGround = true;
    private int lastUseTick;
    private boolean doubleClickPending;
    private int heldTicks;
    private final Random random = new Random();

    public AutoWindCharge() {
        super("AutoWindCharge", "Automatically uses wind charges with human-like imperfections.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        wasOnGround = true;
        lastUseTick = 0;
        doubleClickPending = false;
        heldTicks = 0;
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;

        lastUseTick++;

        if (requireMace.enabled() && !client.player.getMainHandStack().isOf(Items.MACE)) {
            doubleClickPending = false;
            return;
        }

        if (doubleClickPending) {
            int slot = findWindCharge(client);
            if (slot >= 0) {
                if (autoSwitch.enabled() && slot < 9) {
                    client.player.getInventory().setSelectedSlot(slot);
                }
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            }
            doubleClickPending = false;
            lastUseTick = 0;
            return;
        }

        boolean shouldUse = false;

        if (mode.is("On Jump")) {
            boolean onGround = client.player.isOnGround();
            if (wasOnGround && !onGround && client.player.getVelocity().y > 0) {
                shouldUse = true;
            }
            wasOnGround = onGround;
        } else if (mode.is("Always")) {
            if (client.player.isOnGround()) {
                shouldUse = true;
            }
        }

        if (!shouldUse) return;

        int baseDelay = useDelay.get();
        int extraJitter = jitter.get() > 0 ? random.nextInt(jitter.get() + 1) : 0;
        int humanHold = (humanFailure.enabled() && holdVariance.get() > 0) ? random.nextInt(holdVariance.get() + 1) : 0;
        int totalDelay = baseDelay + extraJitter + humanHold;

        if (lastUseTick < totalDelay) return;

        if (humanFailure.enabled() && failureChance.get() > 0 && random.nextFloat() < failureChance.get()) {
            lastUseTick = (int)(lastUseTick * 0.5);
            return;
        }

        int slot = findWindCharge(client);
        if (slot < 0) return;

        if (autoSwitch.enabled()) {
            if (slot < 9) {
                client.player.getInventory().setSelectedSlot(slot);
            }
        }

        if (humanFailure.enabled() && aimVariance.get() > 0) {
            float pitchVar = (random.nextFloat() - 0.5f) * 2 * aimVariance.get();
            client.player.setPitch(client.player.getPitch() + pitchVar);
        }

        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        lastUseTick = 0;

        if (humanFailure.enabled() && doubleClickChance.enabled() && doubleClickOdds.get() > 0 && random.nextFloat() < doubleClickOdds.get()) {
            doubleClickPending = true;
        }
    }

    private int findWindCharge(MinecraftClient client) {
        for (int slot = 0; slot < 9; slot++) {
            if (client.player.getInventory().getStack(slot).isOf(Items.WIND_CHARGE)) {
                return slot;
            }
        }
        return -1;
    }
}
