package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public final class ShortWindCharge extends MiscModule {
    private final BooleanSetting autoJump = setting(new BooleanSetting("Auto Jump", "Jump before wind charge for max height.", true));
    private final DoubleSetting minRange = setting(new DoubleSetting("Min Range", "Min enemy range to activate.", 3.0, 1.0, 10.0));
    private final IntegerSetting cooldown = setting(new IntegerSetting("Cooldown", "Ticks between uses.", 15, 5, 60));
    private final BooleanSetting onlyWhenTarget = setting(new BooleanSetting("Only When Target", "Only use when enemy nearby.", false));
    private final DoubleSetting targetRange = setting(new DoubleSetting("Target Range", "Target detection range.", 8.0, 2.0, 20.0));

    private int tickCounter;

    public ShortWindCharge() {
        super("ShortWindCharge", "Fires a wind charge at your feet to launch upward instantly.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        tickCounter = 0;
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.player == null) return;
        tickCounter++;
        if (tickCounter < cooldown.get()) return;

        if (onlyWhenTarget.enabled()) {
            boolean found = client.world != null && client.world.getPlayers().stream()
                .anyMatch(p -> p != client.player && !p.isDead() && client.player.distanceTo(p) < targetRange.get());
            if (!found) return;
        }

        if (client.player.isOnGround() && client.options.jumpKey.isPressed()) {
            int slot = findWindCharge(client);
            if (slot >= 0 && slot < 9) {
                client.player.getInventory().setSelectedSlot(slot);
                if (autoJump.enabled()) {
                    client.player.jump();
                }
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                tickCounter = 0;
            }
        }
    }

    private int findWindCharge(MinecraftClient client) {
        for (int slot = 0; slot < 9; slot++) {
            if (client.player.getInventory().getStack(slot).isOf(Items.WIND_CHARGE)) return slot;
        }
        return -1;
    }
}
