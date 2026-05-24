package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Hand;

public final class AutoShield extends CombatModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Blocking behavior.", "Always", "Always", "On Target", "When Hurt"));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks between offhand swaps.", 2, 1, 10));
    private final IntegerSetting jitter = setting(new IntegerSetting("Jitter", "Random extra ticks.", 1, 0, 5));
    private final BooleanSetting autoEquip = setting(new BooleanSetting("Auto Equip", "Move shield to offhand automatically.", true));
    private final BooleanSetting keepShield = setting(new BooleanSetting("Keep Shield", "Don't unblock when using items.", false));

    private boolean wasBlocking;
    private int offhandTries;

    public AutoShield() {
        super("AutoShield", "Automatically blocks with a shield and keeps it equipped.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null) return;

        if (autoEquip.enabled() && !isShieldInOffhand(client)) {
            if (offhandTries > 0) {
                offhandTries--;
                return;
            }
            int shieldSlot = findShield(client);
            if (shieldSlot != -1) {
                if (actionReady(delay.get(), jitter.get())) {
                    int screenSlot = shieldSlot >= 36 ? shieldSlot : dev.falaris.client.module.modules.player.InventoryUtil.screenSlotForInventoryIndex(shieldSlot);
                    dev.falaris.client.module.modules.player.InventoryUtil.clickMove(client, screenSlot, 45);
                    offhandTries = 5;
                }
            }
            return;
        }

        boolean shouldBlock = shouldBlock(client);
        if (shouldBlock && !wasBlocking) {
            if (isShieldInOffhand(client) || isShieldInMainHand(client)) {
                client.options.useKey.setPressed(true);
                wasBlocking = true;
            }
        } else if (!shouldBlock && wasBlocking) {
            client.options.useKey.setPressed(false);
            wasBlocking = false;
        }
    }

    private boolean shouldBlock(MinecraftClient client) {
        if (client.player.isUsingItem() && !keepShield.enabled()) return false;
        if (mode.is("Always")) return true;
        if (mode.is("On Target")) {
            return client.crosshairTarget != null && client.crosshairTarget.getType() == net.minecraft.util.hit.HitResult.Type.ENTITY;
        }
        return client.player.hurtTime > 0;
    }

    private boolean isShieldInOffhand(MinecraftClient client) {
        return client.player.getOffHandStack().getItem() instanceof ShieldItem;
    }

    private boolean isShieldInMainHand(MinecraftClient client) {
        return client.player.getMainHandStack().getItem() instanceof ShieldItem;
    }

    private int findShield(MinecraftClient client) {
        for (int i = 0; i < 36; i++) {
            if (client.player.getInventory().getStack(i).isOf(Items.SHIELD)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.options.useKey.setPressed(false);
        }
        wasBlocking = false;
        offhandTries = 0;
    }
}
