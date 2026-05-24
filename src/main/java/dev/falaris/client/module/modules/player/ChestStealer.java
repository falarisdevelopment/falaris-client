package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public final class ChestStealer extends PlayerModule {
    private final BooleanSetting silent = setting(new BooleanSetting("Silent", "Close screen after stealing.", true));
    private final BooleanSetting ignorePlayerInv = setting(new BooleanSetting("Ignore Player Inv", "Only steal from chest slots.", true));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks between item moves.", 2, 0, 10));

    private int tickCounter;

    public ChestStealer() {
        super("ChestStealer", "Automatically takes all items from opened chests.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        tickCounter = 0;
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null) return;

        ScreenHandler handler = client.player.currentScreenHandler;
        if (!(handler instanceof GenericContainerScreenHandler container)) return;

        tickCounter++;
        if (tickCounter < delay.get() + 1) return;
        tickCounter = 0;

        int rows = container.getRows();
        int chestSlots = rows * 9;
        int playerInvStart = chestSlots;

        for (int slot = 0; slot < chestSlots; slot++) {
            if (!container.getSlot(slot).hasStack()) continue;
            if (ignorePlayerInv.enabled() && slot < playerInvStart) {
                client.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.QUICK_MOVE, client.player);
                return;
            }
        }

        if (silent.enabled()) {
            boolean empty = true;
            for (int slot = 0; slot < chestSlots; slot++) {
                if (container.getSlot(slot).hasStack()) {
                    empty = false;
                    break;
                }
            }
            if (empty) {
                client.player.closeHandledScreen();
            }
        }
    }
}
