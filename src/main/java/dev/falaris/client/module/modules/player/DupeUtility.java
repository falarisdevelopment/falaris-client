package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Set;

public final class DupeUtility extends PlayerModule {
    private final BooleanSetting containerSteal = setting(new BooleanSetting("Container Steal", "Auto-steal from opened containers.", true));
    private final BooleanSetting quickMove = setting(new BooleanSetting("Quick Move", "Quick-move items from container to inventory.", true));
    private final BooleanSetting autoDrop = setting(new BooleanSetting("Auto Drop", "Drop configured items from inventory.", false));
    private final BooleanSetting dropTrash = setting(new BooleanSetting("Drop Trash", "Drop trash items when Auto Drop is on.", true));
    private final BooleanSetting dropTools = setting(new BooleanSetting("Drop Tools", "Drop tools when Auto Drop is on.", false));
    private final BooleanSetting dropArmor = setting(new BooleanSetting("Drop Armor", "Drop armor when Auto Drop is on.", false));
    private final IntegerSetting stealDelay = setting(new IntegerSetting("Steal Delay", "Ticks between steal actions.", 2, 0, 10));
    private final IntegerSetting dropDelay = setting(new IntegerSetting("Drop Delay", "Ticks between drops.", 5, 1, 20));

    private int tickCounter;

    private static final Set<Item> TRASH = Set.of(
        Items.DIRT, Items.COBBLESTONE, Items.GRAVEL, Items.SAND, Items.RED_SAND,
        Items.GRANITE, Items.DIORITE, Items.ANDESITE, Items.TUFF, Items.CALCITE,
        Items.DEEPSLATE, Items.STONE, Items.NETHERRACK, Items.BASALT,
        Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.POISONOUS_POTATO,
        Items.ARROW, Items.BONE, Items.STRING, Items.FEATHER,
        Items.LEATHER, Items.KELP, Items.SEAGRASS, Items.STICK, Items.FLINT,
        Items.EGG, Items.INK_SAC, Items.GLOW_INK_SAC,
        Items.WHEAT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.BEETROOT_SEEDS,
        Items.COCOA_BEANS, Items.SUGAR_CANE, Items.CACTUS,
        Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.JUNGLE_LOG,
        Items.ACACIA_LOG, Items.DARK_OAK_LOG, Items.CHERRY_LOG, Items.MANGROVE_LOG,
        Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS, Items.JUNGLE_PLANKS,
        Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS, Items.CHERRY_PLANKS, Items.MANGROVE_PLANKS
    );

    public DupeUtility() {
        super("DupeUtility", "Container-stealer, quick-move, and auto-drop utilities.");
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

        if (containerSteal.enabled() || quickMove.enabled()) {
            handleContainer(client);
        }

        if (autoDrop.enabled()) {
            handleAutoDrop(client);
        }
    }

    private void handleContainer(MinecraftClient client) {
        ScreenHandler handler = client.player.currentScreenHandler;
        if (!(handler instanceof GenericContainerScreenHandler container)) return;

        if (tickCounter < stealDelay.get() + 1) return;
        tickCounter = 0;

        int rows = container.getRows();
        int chestSlots = rows * 9;

        for (int slot = 0; slot < chestSlots; slot++) {
            if (!container.getSlot(slot).hasStack()) continue;
            if (quickMove.enabled()) {
                client.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.QUICK_MOVE, client.player);
                return;
            }
            if (containerSteal.enabled()) {
                client.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, client.player);
                return;
            }
        }
    }

    private void handleAutoDrop(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (tickCounter < dropDelay.get() + 1) return;
        tickCounter = 0;

        for (int slot = 9; slot < 36; slot++) {
            Item item = player.getInventory().getStack(slot).getItem();
            if (shouldDrop(item)) {
                dropSlot(player, slot);
                return;
            }
        }
        for (int slot = 0; slot < 9; slot++) {
            Item item = player.getInventory().getStack(slot).getItem();
            if (shouldDrop(item)) {
                dropSlot(player, slot);
                return;
            }
        }
    }

    private boolean shouldDrop(Item item) {
        if (dropTrash.enabled() && TRASH.contains(item)) return true;
        if (dropTools.enabled() && isTool(item)) return true;
        if (dropArmor.enabled() && isArmor(item)) return true;
        return false;
    }

    private boolean isTool(Item item) {
        return item == Items.WOODEN_PICKAXE || item == Items.STONE_PICKAXE || item == Items.IRON_PICKAXE
            || item == Items.GOLDEN_PICKAXE || item == Items.DIAMOND_PICKAXE || item == Items.NETHERITE_PICKAXE
            || item == Items.WOODEN_AXE || item == Items.STONE_AXE || item == Items.IRON_AXE
            || item == Items.GOLDEN_AXE || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE
            || item == Items.WOODEN_SHOVEL || item == Items.STONE_SHOVEL || item == Items.IRON_SHOVEL
            || item == Items.GOLDEN_SHOVEL || item == Items.DIAMOND_SHOVEL || item == Items.NETHERITE_SHOVEL
            || item == Items.WOODEN_HOE || item == Items.STONE_HOE || item == Items.IRON_HOE
            || item == Items.GOLDEN_HOE || item == Items.DIAMOND_HOE || item == Items.NETHERITE_HOE;
    }

    private boolean isArmor(Item item) {
        return item == Items.LEATHER_HELMET || item == Items.CHAINMAIL_HELMET || item == Items.IRON_HELMET
            || item == Items.GOLDEN_HELMET || item == Items.DIAMOND_HELMET || item == Items.NETHERITE_HELMET
            || item == Items.LEATHER_CHESTPLATE || item == Items.CHAINMAIL_CHESTPLATE || item == Items.IRON_CHESTPLATE
            || item == Items.GOLDEN_CHESTPLATE || item == Items.DIAMOND_CHESTPLATE || item == Items.NETHERITE_CHESTPLATE
            || item == Items.LEATHER_LEGGINGS || item == Items.CHAINMAIL_LEGGINGS || item == Items.IRON_LEGGINGS
            || item == Items.GOLDEN_LEGGINGS || item == Items.DIAMOND_LEGGINGS || item == Items.NETHERITE_LEGGINGS
            || item == Items.LEATHER_BOOTS || item == Items.CHAINMAIL_BOOTS || item == Items.IRON_BOOTS
            || item == Items.GOLDEN_BOOTS || item == Items.DIAMOND_BOOTS || item == Items.NETHERITE_BOOTS;
    }

    private void dropSlot(ClientPlayerEntity player, int slot) {
        if (MinecraftClient.getInstance().interactionManager != null) {
            MinecraftClient.getInstance().interactionManager.clickSlot(player.currentScreenHandler.syncId, 36 + slot, 1, SlotActionType.THROW, player);
        }
    }
}
