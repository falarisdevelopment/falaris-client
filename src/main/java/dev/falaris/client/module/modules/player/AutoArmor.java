package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

public final class AutoArmor extends PlayerModule {
    private final ModeSetting priority = setting(new ModeSetting("Priority", "How armor is ranked.", "Protection", "Protection", "Durability"));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks between armor swaps.", 4, 1, 30));
    private final IntegerSetting jitter = setting(new IntegerSetting("Jitter", "Random extra ticks between armor swaps.", 2, 0, 12));
    private final BooleanSetting allowWhileMoving = setting(new BooleanSetting("Allow Moving", "Swap armor while moving.", true));
    private final BooleanSetting inventoryOnly = setting(new BooleanSetting("Inventory Only", "Only swap while an inventory screen is open.", false));

    public AutoArmor() {
        super("AutoArmor", "Equips the best available armor pieces.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null || !ready(delay.get(), jitter.get())) {
            return;
        }
        if (inventoryOnly.enabled() && client.currentScreen == null) {
            return;
        }
        if (!allowWhileMoving.enabled() && client.player.getVelocity().horizontalLengthSquared() > 0.001) {
            return;
        }

        int bestInventoryIndex = -1;
        int bestTargetSlot = -1;
        double bestImprovement = 0.0;
        for (int index = 0; index < 36; index++) {
            ItemStack candidate = client.player.getInventory().getStack(index);
            int targetSlot = InventoryUtil.armorTargetSlot(candidate);
            if (targetSlot == -1) {
                continue;
            }

            ItemStack equipped = client.player.currentScreenHandler.getSlot(targetSlot).getStack();
            double candidateScore = score(candidate);
            double equippedScore = score(equipped);
            double improvement = candidateScore - equippedScore;
            if (improvement > bestImprovement) {
                bestInventoryIndex = index;
                bestTargetSlot = targetSlot;
                bestImprovement = improvement;
            }
        }

        if (bestInventoryIndex != -1) {
            InventoryUtil.clickMove(client, InventoryUtil.screenSlotForInventoryIndex(bestInventoryIndex), bestTargetSlot);
        }
    }

    private double score(ItemStack stack) {
        if (priority.is("Durability")) {
            return stack.isEmpty() ? -1.0 : stack.getMaxDamage() - stack.getDamage();
        }
        return InventoryUtil.armorScore(stack);
    }
}
