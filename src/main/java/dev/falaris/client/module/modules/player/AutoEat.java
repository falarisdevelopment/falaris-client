package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public final class AutoEat extends PlayerModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "When to eat.", "Hunger", "Hunger", "Always"));
    private final DoubleSetting hungerThreshold = setting(new DoubleSetting("Hunger", "Hunger bar level to eat at.", 6.0, 1.0, 19.0));
    private final DoubleSetting saturationThreshold = setting(new DoubleSetting("Saturation", "Saturation threshold.", 4.0, 0.0, 20.0));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks between actions.", 10, 1, 40));
    private final IntegerSetting jitter = setting(new IntegerSetting("Jitter", "Random extra ticks.", 5, 0, 20));
    private final BooleanSetting preferCooked = setting(new BooleanSetting("Prefer Cooked", "Prefer cooked food over raw.", true));
    private final ModeSetting foodPriority = setting(new ModeSetting("Food Priority", "Which food to prefer.", "Best Saturation", "Best Saturation", "Best Hunger"));

    public AutoEat() {
        super("AutoEat", "Auto-eats food when hunger or saturation is low.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null) return;

        if (client.player.isUsingItem()) return;
        if (mode.is("Hunger")) {
            if (client.player.getHungerManager().getFoodLevel() > hungerThreshold.get()) return;
            if (client.player.getHungerManager().getSaturationLevel() > saturationThreshold.get()) return;
        }
        if (!ready(delay.get(), jitter.get())) return;

        int bestSlot = -1;
        double bestScore = -1;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            FoodComponent food = stack.get(DataComponentTypes.FOOD);
            if (food == null) continue;

            double score = scoreFood(food);
            if (preferCooked.enabled() && stack.get(DataComponentTypes.FOOD) != null) {
                String itemName = stack.getItem().getTranslationKey().toLowerCase();
                if (itemName.contains("raw") || itemName.contains("rotten") || itemName.contains("poison")) {
                    score -= 5.0;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        if (bestSlot == -1) return;

        int hotbarSlot = bestSlot;
        if (bestSlot >= 9) {
            int emptyHotbar = -1;
            for (int h = 0; h < 9; h++) {
                if (client.player.getInventory().getStack(h).isEmpty()) {
                    emptyHotbar = h;
                    break;
                }
            }
            if (emptyHotbar != -1) {
                int screenSlot = InventoryUtil.screenSlotForInventoryIndex(bestSlot);
                int targetSlot = 36 + emptyHotbar;
                InventoryUtil.clickMove(client, screenSlot, targetSlot);
                hotbarSlot = emptyHotbar;
            } else {
                hotbarSlot = bestSlot >= 36 ? bestSlot - 36 : client.player.getInventory().getSelectedSlot();
            }
        }

        client.player.getInventory().setSelectedSlot(hotbarSlot);
        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
    }

    private double scoreFood(FoodComponent food) {
        if (foodPriority.is("Best Hunger")) {
            return food.nutrition();
        }
        return food.saturation();
    }
}
