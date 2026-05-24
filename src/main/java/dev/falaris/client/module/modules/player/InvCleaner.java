package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Set;

public final class InvCleaner extends PlayerModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "What to clean.", "Trash", "Trash", "All", "Custom"));
    private final BooleanSetting autoDrop = setting(new BooleanSetting("Auto Drop", "Drop items automatically each tick.", false));
    private final BooleanSetting tools = setting(new BooleanSetting("Tools", "Drop tools (if Trash mode).", false));
    private final BooleanSetting armor = setting(new BooleanSetting("Armor", "Drop armor (if Trash mode).", false));
    private final IntegerSetting invCheckInterval = setting(new IntegerSetting("Check Interval", "Ticks between inventory scans.", 10, 2, 60));

    private static final Set<Item> TRASH = Set.of(
        Items.DIRT, Items.COBBLESTONE, Items.GRAVEL, Items.SAND, Items.RED_SAND,
        Items.GRANITE, Items.DIORITE, Items.ANDESITE, Items.TUFF, Items.CALCITE,
        Items.DEEPSLATE, Items.STONE, Items.NETHERRACK, Items.BASALT,
        Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.POISONOUS_POTATO,
        Items.ARROW, Items.BONE, Items.STRING, Items.FEATHER,
        Items.LEATHER, Items.PORKCHOP, Items.BEEF, Items.CHICKEN,
        Items.KELP, Items.SEAGRASS, Items.STICK, Items.FLINT,
        Items.EGG, Items.INK_SAC, Items.GLOW_INK_SAC,
        Items.WHEAT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.BEETROOT_SEEDS,
        Items.COCOA_BEANS, Items.SUGAR_CANE, Items.CACTUS,
        Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.JUNGLE_LOG,
        Items.ACACIA_LOG, Items.DARK_OAK_LOG, Items.CHERRY_LOG, Items.MANGROVE_LOG,
        Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS, Items.JUNGLE_PLANKS,
        Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS, Items.CHERRY_PLANKS, Items.MANGROVE_PLANKS
    );

    public InvCleaner() {
        super("InvCleaner", "Automatically drops junk items from inventory.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null) return;

        if (!autoDrop.enabled() || !ready(invCheckInterval.get(), 3)) return;

        for (int slot = 9; slot < 36; slot++) {
            Item item = client.player.getInventory().getStack(slot).getItem();
            if (shouldDrop(item)) {
                dropSlot(client.player, slot);
                break;
            }
        }
        for (int slot = 0; slot < 9; slot++) {
            Item item = client.player.getInventory().getStack(slot).getItem();
            if (shouldDrop(item)) {
                dropSlot(client.player, slot);
                break;
            }
        }
    }

    private boolean shouldDrop(Item item) {
        return switch (mode.get()) {
            case "All" -> !tools.enabled() && isTool(item) || !armor.enabled() && isArmor(item) ? false : true;
            case "Trash" -> TRASH.contains(item);
            default -> false;
        };
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
