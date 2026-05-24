package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

public final class AutoReplenish extends PlayerModule {
    private final IntegerSetting threshold = setting(new IntegerSetting("Threshold", "Refill when stack is below this count.", 16, 1, 64));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks between refills.", 5, 1, 20));
    private final BooleanSetting hotbarOnly = setting(new BooleanSetting("Hotbar Only", "Only refill hotbar from inventory.", true));
    private final BooleanSetting pauseOnOpen = setting(new BooleanSetting("Pause On Open", "Don't refill while inventory is open.", true));

    private int tickCounter;

    public AutoReplenish() {
        super("AutoReplenish", "Auto-refills hotbar slots from inventory.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        tickCounter = 0;
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null) return;
        tickCounter++;
        if (tickCounter < delay.get()) return;
        tickCounter = 0;

        if (pauseOnOpen.enabled() && client.currentScreen != null) return;

        var inventory = client.player.getInventory();

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            ItemStack stack = inventory.getStack(hotbarSlot);
            if (stack.isEmpty()) continue;
            if (stack.getCount() >= threshold.get()) continue;

            String itemId = net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString();

            int bestSlot = -1;
            int bestCount = 0;
            int start = hotbarOnly.enabled() ? 9 : 0;
            for (int slot = start; slot < 36; slot++) {
                ItemStack other = inventory.getStack(slot);
                if (other.isEmpty() || slot == hotbarSlot) continue;
                String otherId = net.minecraft.registry.Registries.ITEM.getId(other.getItem()).toString();
                if (!otherId.equals(itemId)) continue;
                if (other.getCount() > bestCount) {
                    bestCount = other.getCount();
                    bestSlot = slot;
                }
            }

            if (bestSlot >= 0) {
                int targetCount = Math.min(stack.getMaxCount(), stack.getCount() + bestCount);
                if (bestSlot >= 9) {
                    client.interactionManager.clickSlot(
                        client.player.currentScreenHandler.syncId,
                        bestSlot < 36 ? bestSlot + 36 : bestSlot,
                        hotbarSlot,
                        net.minecraft.screen.slot.SlotActionType.SWAP,
                        client.player
                    );
                } else {
                    client.interactionManager.clickSlot(
                        client.player.currentScreenHandler.syncId,
                        bestSlot,
                        hotbarSlot,
                        net.minecraft.screen.slot.SlotActionType.SWAP,
                        client.player
                    );
                }
            }
        }
    }
}
