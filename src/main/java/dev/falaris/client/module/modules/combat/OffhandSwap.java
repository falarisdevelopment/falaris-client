package dev.falaris.client.module.modules.combat;

import dev.falaris.client.module.modules.player.InventoryUtil;
import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;

import java.util.Random;

public final class OffhandSwap extends CombatModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Swap strategy.", "Crystal", "Crystal", "Totem", "Auto"));
    private final BooleanSetting autoBack = setting(new BooleanSetting("Auto Back", "Swap back to previous item after burst.", true));
    private final IntegerSetting minDelay = setting(new IntegerSetting("Min Delay", "Min ticks between swaps.", 2, 1, 10));
    private final IntegerSetting maxDelay = setting(new IntegerSetting("Max Delay", "Max ticks between swaps.", 5, 1, 20));

    private final Random random = new Random();
    private int tickCounter;
    private int prevSlot;
    private boolean swapped;

    public OffhandSwap() {
        super("OffhandSwap", "Auto-swaps between totem and crystal in offhand.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null) return;
        tickCounter++;

        boolean hasCrystal = false;
        boolean hasTotem = false;
        for (int i = 0; i < 36; i++) {
            var stack = client.player.getInventory().getStack(i);
            if (stack.isOf(Items.END_CRYSTAL)) hasCrystal = true;
            if (stack.isOf(Items.TOTEM_OF_UNDYING)) hasTotem = true;
        }

        boolean offhandIsCrystal = client.player.getOffHandStack().isOf(Items.END_CRYSTAL);
        boolean offhandIsTotem = client.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING);
        boolean attacking = client.options.attackKey.isPressed();

        int delay = minDelay.get() + random.nextInt(maxDelay.get() - minDelay.get() + 1);
        if (tickCounter < delay) return;
        tickCounter = 0;

        if (mode.is("Crystal")) {
            if (attacking && hasCrystal && !offhandIsCrystal) {
                swapOffhand(client, Items.END_CRYSTAL);
            } else if (!attacking && hasTotem && !offhandIsTotem && autoBack.enabled()) {
                swapOffhand(client, Items.TOTEM_OF_UNDYING);
            }
        } else if (mode.is("Totem")) {
            if (attacking && hasTotem && !offhandIsTotem) {
                swapOffhand(client, Items.TOTEM_OF_UNDYING);
            }
        } else if (mode.is("Auto")) {
            float health = client.player.getHealth() + client.player.getAbsorptionAmount();
            if (health < 8.0f && hasTotem && !offhandIsTotem) {
                swapOffhand(client, Items.TOTEM_OF_UNDYING);
            } else if (health >= 8.0f && hasCrystal && !offhandIsCrystal && attacking) {
                swapOffhand(client, Items.END_CRYSTAL);
            }
        }
    }

    private void swapOffhand(MinecraftClient client, net.minecraft.item.Item targetItem) {
        for (int i = 0; i < 36; i++) {
            var stack = client.player.getInventory().getStack(i);
            if (stack.isOf(targetItem)) {
                int screenSlot = InventoryUtil.screenSlotForInventoryIndex(i);
                InventoryUtil.clickMove(client, screenSlot, InventoryUtil.OFFHAND_SLOT);
                return;
            }
        }
    }
}
