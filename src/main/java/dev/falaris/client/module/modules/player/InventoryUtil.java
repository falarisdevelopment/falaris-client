package dev.falaris.client.module.modules.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public final class InventoryUtil {
    public static final int OFFHAND_SLOT = 45;
    public static final int HELMET_SLOT = 5;
    public static final int CHESTPLATE_SLOT = 6;
    public static final int LEGGINGS_SLOT = 7;
    public static final int BOOTS_SLOT = 8;

    InventoryUtil() {
    }

    public static int screenSlotForInventoryIndex(int inventoryIndex) {
        if (inventoryIndex >= 0 && inventoryIndex <= 8) {
            return 36 + inventoryIndex;
        }
        if (inventoryIndex >= 9 && inventoryIndex <= 35) {
            return inventoryIndex;
        }
        return -1;
    }

    public static int findItem(ClientPlayerEntity player, Item item, boolean hotbarOnly) {
        int end = hotbarOnly ? 9 : 36;
        for (int index = 0; index < end; index++) {
            if (player.getInventory().getStack(index).isOf(item)) {
                return index;
            }
        }
        return -1;
    }

    static int findBestSword(ClientPlayerEntity player, boolean allowAxes, boolean hotbarOnly) {
        int end = hotbarOnly ? 9 : 36;
        int best = -1;
        double bestScore = -1.0;
        for (int index = 0; index < end; index++) {
            ItemStack stack = player.getInventory().getStack(index);
            double score = weaponScore(stack, allowAxes);
            if (score > bestScore) {
                best = index;
                bestScore = score;
            }
        }
        return best;
    }

    static double weaponScore(ItemStack stack, boolean allowAxes) {
        if (stack.isEmpty()) {
            return -1.0;
        }
        if (isSword(stack.getItem())) {
            return 100.0 + stack.getMaxDamage() - stack.getDamage();
        }
        if (allowAxes && stack.getItem() instanceof AxeItem) {
            return 80.0 + stack.getMaxDamage() - stack.getDamage();
        }
        return -1.0;
    }

    static int findBestTool(ClientPlayerEntity player, net.minecraft.block.BlockState state, boolean hotbarOnly) {
        int end = hotbarOnly ? 9 : 36;
        int best = -1;
        float bestSpeed = 1.0f;
        for (int index = 0; index < end; index++) {
            ItemStack stack = player.getInventory().getStack(index);
            if (stack.getMiningSpeedMultiplier(state) <= 1.0f) {
                continue;
            }
            float speed = stack.getMiningSpeedMultiplier(state);
            if (speed > bestSpeed) {
                best = index;
                bestSpeed = speed;
            }
        }
        return best;
    }

    static int armorTargetSlot(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.LEATHER_HELMET || item == Items.CHAINMAIL_HELMET || item == Items.COPPER_HELMET || item == Items.IRON_HELMET || item == Items.GOLDEN_HELMET || item == Items.DIAMOND_HELMET || item == Items.NETHERITE_HELMET || item == Items.TURTLE_HELMET) {
            return HELMET_SLOT;
        }
        if (item == Items.LEATHER_CHESTPLATE || item == Items.CHAINMAIL_CHESTPLATE || item == Items.COPPER_CHESTPLATE || item == Items.IRON_CHESTPLATE || item == Items.GOLDEN_CHESTPLATE || item == Items.DIAMOND_CHESTPLATE || item == Items.NETHERITE_CHESTPLATE) {
            return CHESTPLATE_SLOT;
        }
        if (item == Items.LEATHER_LEGGINGS || item == Items.CHAINMAIL_LEGGINGS || item == Items.COPPER_LEGGINGS || item == Items.IRON_LEGGINGS || item == Items.GOLDEN_LEGGINGS || item == Items.DIAMOND_LEGGINGS || item == Items.NETHERITE_LEGGINGS) {
            return LEGGINGS_SLOT;
        }
        if (item == Items.LEATHER_BOOTS || item == Items.CHAINMAIL_BOOTS || item == Items.COPPER_BOOTS || item == Items.IRON_BOOTS || item == Items.GOLDEN_BOOTS || item == Items.DIAMOND_BOOTS || item == Items.NETHERITE_BOOTS) {
            return BOOTS_SLOT;
        }
        return -1;
    }

    static double armorScore(ItemStack stack) {
        return armorProtection(stack.getItem()) * 1000.0 + stack.getMaxDamage() - stack.getDamage();
    }

    public static boolean clickMove(MinecraftClient client, int fromScreenSlot, int toScreenSlot) {
        if (client.player == null || client.interactionManager == null || fromScreenSlot < 0 || toScreenSlot < 0) {
            return false;
        }
        int syncId = client.player.currentScreenHandler.syncId;
        client.interactionManager.clickSlot(syncId, fromScreenSlot, 0, SlotActionType.PICKUP, client.player);
        client.interactionManager.clickSlot(syncId, toScreenSlot, 0, SlotActionType.PICKUP, client.player);
        client.interactionManager.clickSlot(syncId, fromScreenSlot, 0, SlotActionType.PICKUP, client.player);
        return true;
    }

    static boolean quickMove(MinecraftClient client, int screenSlot) {
        if (client.player == null || client.interactionManager == null || screenSlot < 0) {
            return false;
        }
        client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, screenSlot, 0, SlotActionType.QUICK_MOVE, client.player);
        return true;
    }

    static boolean selectHotbar(ClientPlayerEntity player, int inventoryIndex) {
        if (inventoryIndex < 0 || inventoryIndex > 8) {
            return false;
        }
        player.getInventory().setSelectedSlot(inventoryIndex);
        return true;
    }

    public static boolean isTotem(ItemStack stack) {
        return stack.isOf(Items.TOTEM_OF_UNDYING);
    }

    private static boolean isSword(Item item) {
        return item == Items.WOODEN_SWORD || item == Items.STONE_SWORD || item == Items.COPPER_SWORD || item == Items.IRON_SWORD || item == Items.GOLDEN_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD;
    }

    private static int armorProtection(Item item) {
        if (item == Items.LEATHER_HELMET || item == Items.LEATHER_BOOTS || item == Items.GOLDEN_HELMET || item == Items.GOLDEN_BOOTS || item == Items.CHAINMAIL_HELMET || item == Items.CHAINMAIL_BOOTS || item == Items.COPPER_HELMET || item == Items.COPPER_BOOTS || item == Items.TURTLE_HELMET) return 1;
        if (item == Items.LEATHER_LEGGINGS || item == Items.GOLDEN_LEGGINGS) return 2;
        if (item == Items.LEATHER_CHESTPLATE || item == Items.CHAINMAIL_LEGGINGS || item == Items.COPPER_LEGGINGS) return 3;
        if (item == Items.IRON_HELMET || item == Items.IRON_BOOTS || item == Items.CHAINMAIL_CHESTPLATE || item == Items.COPPER_CHESTPLATE) return 4;
        if (item == Items.IRON_LEGGINGS || item == Items.DIAMOND_HELMET || item == Items.DIAMOND_BOOTS || item == Items.NETHERITE_HELMET || item == Items.NETHERITE_BOOTS) return 5;
        if (item == Items.IRON_CHESTPLATE || item == Items.DIAMOND_LEGGINGS || item == Items.NETHERITE_LEGGINGS) return 6;
        if (item == Items.DIAMOND_CHESTPLATE || item == Items.NETHERITE_CHESTPLATE) return 8;
        return -1;
    }
}
