package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
public final class AutoAttributeSwap extends CombatModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Swap mode.", "Smart", "Smart", "Air Only", "Ground Only"));
    private final BooleanSetting useBreach = setting(new BooleanSetting("Use Breach", "Swap to Breach mace on ground.", true));
    private final BooleanSetting useDensity = setting(new BooleanSetting("Use Density", "Swap to Density mace in air.", true));
    private final BooleanSetting preferBreach = setting(new BooleanSetting("Prefer Breach", "Prefer Breach when both enchants present.", true));
    private final BooleanSetting switchBack = setting(new BooleanSetting("Switch Back", "Return to sword after attacking.", true));
    private final IntegerSetting switchBackDelay = setting(new IntegerSetting("Switch Back Delay", "Ticks before switching back.", 5, 0, 20));
    private final DoubleSetting manualOverrideTime = setting(new DoubleSetting("Manual Override", "Seconds to respect manual slot change.", 2.0, 0.0, 10.0));
    private final BooleanSetting requireSword = setting(new BooleanSetting("Require Sword", "Only swap if holding a sword.", false));
    private final BooleanSetting hotbarOnly = setting(new BooleanSetting("Hotbar Only", "Only search hotbar for mace.", true));

    private int prevSlot = -1;
    private int tickCounter;
    private int switchBackWait;
    private long lastManualTime;
    private int lastKnownSlot = -1;
    private boolean hasAttacked;

    public AutoAttributeSwap() {
        super("AutoAttributeSwap", "Smart Breach/Density mace swapping based on ground/air state with manual override.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        tickCounter = 0;
        prevSlot = -1;
        switchBackWait = 0;
        lastManualTime = 0;
        lastKnownSlot = -1;
        hasAttacked = false;
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null) return;
        tickCounter++;

        int currentSlot = client.player.getInventory().getSelectedSlot();
        ItemStack mainHand = client.player.getMainHandStack();
        boolean isMace = mainHand.isOf(Items.MACE);
        boolean isSword = mainHand.getItem() == Items.WOODEN_SWORD || mainHand.getItem() == Items.STONE_SWORD
            || mainHand.getItem() == Items.IRON_SWORD || mainHand.getItem() == Items.GOLDEN_SWORD
            || mainHand.getItem() == Items.DIAMOND_SWORD || mainHand.getItem() == Items.NETHERITE_SWORD;

        // Detect manual override: user switched slots without our help
        if (lastKnownSlot != -1 && currentSlot != lastKnownSlot && !isMace && switchBackWait <= 0) {
            lastManualTime = System.currentTimeMillis();
        }
        lastKnownSlot = currentSlot;

        // Check if we're in manual override period
        if (isManualOverride()) return;

        // Handle switch-back after attack
        if (switchBackWait > 0) {
            switchBackWait--;
            if (switchBackWait <= 0 && switchBack.enabled() && isMace && prevSlot >= 0 && prevSlot < 9) {
                client.player.getInventory().setSelectedSlot(prevSlot);
                prevSlot = -1;
            }
            return;
        }

        // Detect attack with mace - start switch-back timer
        if (isMace && client.options.attackKey.isPressed() && client.player.handSwinging) {
            hasAttacked = true;
            if (switchBack.enabled() && prevSlot >= 0 && prevSlot < 9) {
                switchBackWait = switchBackDelay.get();
            }
        }

        boolean isAirborne = !client.player.isOnGround() || client.player.fallDistance > 1.0f;
        boolean airMode = mode.is("Smart") || mode.is("Air Only");
        boolean groundMode = mode.is("Smart") || mode.is("Ground Only");

        // Check if we should equip mace
        boolean shouldEquip = false;
        String enchantPreference = null;

        if (airMode && isAirborne) {
            shouldEquip = useDensity.enabled() && findBestMace(client, "density") != -1;
            enchantPreference = "density";
        } else if (groundMode && !isAirborne) {
            shouldEquip = useBreach.enabled() && (findBestMace(client, "breach") != -1 || findBestMace(client, null) != -1);
            enchantPreference = preferBreach.enabled() ? "breach" : null;
        }

        if (!shouldEquip) {
            // If we have a mace equipped and shouldn't, consider switching back
            if (isMace && prevSlot >= 0 && prevSlot < 9 && switchBack.enabled() && tickCounter > 5) {
                client.player.getInventory().setSelectedSlot(prevSlot);
                prevSlot = -1;
            }
            return;
        }

        // Don't swap if we require sword and aren't holding one
        if (requireSword.enabled() && !isMace && !isSword) return;

        // Find and equip the best mace
        int maceSlot = findBestMace(client, enchantPreference);
        if (maceSlot == -1) return;

        if (!isMace && tickCounter >= 3) {
            if (prevSlot == -1) {
                prevSlot = currentSlot;
            }
            if (maceSlot < 9) {
                client.player.getInventory().setSelectedSlot(maceSlot);
                tickCounter = 0;
            }
        }
    }

    private boolean isManualOverride() {
        if (manualOverrideTime.get() <= 0) return false;
        return System.currentTimeMillis() - lastManualTime < manualOverrideTime.get() * 1000;
    }

    private int findBestMace(MinecraftClient client, String preferredEnchant) {
        int bestSlot = -1;
        int bestScore = -1;
        int limit = hotbarOnly.enabled() ? 9 : 36;

        for (int slot = 0; slot < limit; slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (!stack.isOf(Items.MACE)) continue;

            int score = slot < 9 ? 3 : 0;
            boolean hasBreach = hasEnchantment(stack, "breach");
            boolean hasDensity = hasEnchantment(stack, "density");

            if (preferredEnchant != null) {
                if (preferredEnchant.equals("breach") && hasBreach) score += 5;
                if (preferredEnchant.equals("density") && hasDensity) score += 5;
                // Still usable even without preferred enchant
                if (hasBreach || hasDensity) score += 1;
            } else {
                if (hasBreach) score += 2;
                if (hasDensity) score += 2;
            }

            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private boolean hasEnchantment(ItemStack stack, String name) {
        var ench = EnchantmentHelper.getEnchantments(stack);
        if (ench == null) return false;
        var list = ench.getEnchantments();
        if (list.isEmpty()) return false;
        // Use toString as a simple check matching existing codebase pattern
        return ench.toString().toLowerCase().contains(name);
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && prevSlot >= 0 && prevSlot < 9) {
            client.player.getInventory().setSelectedSlot(prevSlot);
        }
        prevSlot = -1;
        tickCounter = 0;
        switchBackWait = 0;
        hasAttacked = false;
    }
}
