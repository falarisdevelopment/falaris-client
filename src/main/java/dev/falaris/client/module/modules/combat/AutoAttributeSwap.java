package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class AutoAttributeSwap extends CombatModule {
    private final BooleanSetting swordToMace = setting(new BooleanSetting("Sword to Mace", "Swap sword to mace when airborne.", true));
    private final BooleanSetting preferBreach = setting(new BooleanSetting("Prefer Breach", "Prefer mace with Breach enchant.", true));
    private final BooleanSetting preferDensity = setting(new BooleanSetting("Prefer Density", "Prefer mace with Density enchant.", true));
    private final BooleanSetting detectAirborne = setting(new BooleanSetting("Detect Airborne", "Swap when falling or flying.", true));
    private final BooleanSetting detectElytra = setting(new BooleanSetting("Detect Elytra", "Swap when elytra flying.", true));
    private final IntegerSetting swapDelay = setting(new IntegerSetting("Swap Delay", "Ticks between swaps.", 10, 1, 40));
    private final BooleanSetting swapBackOnGround = setting(new BooleanSetting("Swap Back on Ground", "Return to previous slot when on ground.", true));
    private final BooleanSetting preferMaceOnly = setting(new BooleanSetting("Spear Mace", "Swap to mace even when holding non-sword weapons.", false));

    private int lastNonMaceSlot = -1;
    private int tickCounter;
    private boolean hasMaceEquipped;

    public AutoAttributeSwap() {
        super("AutoAttributeSwap", "Auto-swaps between sword and mace based on fall state. Spear mace mode swaps any held item to mace.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        tickCounter = 0;
        lastNonMaceSlot = -1;
        hasMaceEquipped = false;
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null) return;

        tickCounter++;
        ItemStack mainHand = client.player.getMainHandStack();
        boolean isMace = mainHand.getItem() == Items.MACE;
        boolean shouldUseMace = shouldUseMace(client);

        int currentSlot = client.player.getInventory().getSelectedSlot();

        if (isMace) {
            hasMaceEquipped = true;
        }

        if (!shouldUseMace && isMace && hasMaceEquipped && lastNonMaceSlot >= 0 && lastNonMaceSlot < 9 && tickCounter >= swapDelay.get()) {
            if (swapBackOnGround.enabled()) {
                client.player.getInventory().setSelectedSlot(lastNonMaceSlot);
                lastNonMaceSlot = -1;
                hasMaceEquipped = false;
                tickCounter = 0;
            }
            return;
        }

        if (shouldUseMace) {
            if (!isMace && tickCounter >= swapDelay.get()) {
                int maceSlot = findBestMace(client.player);
                if (maceSlot >= 0 && maceSlot < 9) {
                    if (!isMace && preferMaceOnly.enabled() && swordToMace.enabled()) {
                        lastNonMaceSlot = currentSlot;
                    } else if (!isMace && isSwordItem(mainHand.getItem()) && swordToMace.enabled()) {
                        lastNonMaceSlot = currentSlot;
                    }
                    if (lastNonMaceSlot >= 0) {
                        client.player.getInventory().setSelectedSlot(maceSlot);
                        hasMaceEquipped = true;
                        tickCounter = 0;
                    }
                }
            }
        } else {
            if (!isMace && lastNonMaceSlot == -1) {
                lastNonMaceSlot = currentSlot;
            }
        }
    }

    private boolean shouldUseMace(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (detectAirborne.enabled()) {
            if (!player.isOnGround() || player.fallDistance > 2.0f) return true;
        }
        if (detectElytra.enabled()) {
            ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
            if (chest.isOf(Items.ELYTRA) && player.isGliding()) return true;
        }
        return false;
    }

    private int findBestMace(ClientPlayerEntity player) {
        int bestSlot = -1;
        int bestScore = -1;

        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (!stack.isOf(Items.MACE)) continue;

            int score = 0;
            if (slot < 9) score += 3;
            var ench = stack.get(net.minecraft.component.DataComponentTypes.ENCHANTMENTS);
            if (ench != null && preferBreach.enabled()) {
                String s = ench.toString().toLowerCase();
                if (s.contains("breach")) score += 2;
            }
            if (ench != null && preferDensity.enabled()) {
                String s = ench.toString().toLowerCase();
                if (s.contains("density")) score += 2;
            }

            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private boolean isSwordItem(net.minecraft.item.Item item) {
        return item == Items.WOODEN_SWORD || item == Items.STONE_SWORD
            || item == Items.IRON_SWORD || item == Items.GOLDEN_SWORD
            || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD;
    }
}
