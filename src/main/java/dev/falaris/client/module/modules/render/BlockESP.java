package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import dev.falaris.client.setting.StringSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class BlockESP extends RenderModule {
    private final ModeSetting blockPreset = setting(new ModeSetting("Block Preset", "Predefined block groups or custom.", "Custom",
            "Ores", "Rare Ores", "All Ores", "Diamonds", "Netherite", "Emeralds", "Iron", "Gold", "Lapis", "Redstone", "Coal",
            "Ancient Debris", "Chests", "Spawners", "Bookshelves", "Crafting", "Custom"));
    private final StringSetting blocks = setting(new StringSetting("Blocks",
            "Comma-separated block IDs. Type any block from the game (e.g. minecraft:ancient_debris,minecraft:diamond_block).",
            "minecraft:diamond_ore,minecraft:deepslate_diamond_ore,minecraft:ancient_debris"));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Show blocks through walls.", true));
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Block scan range.", 32.0, 4.0, 128.0));

    public BlockESP() {
        super("BlockESP", "Highlights any blocks you choose. Type any block ID in the Blocks field.");
    }

    @Override
    protected void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Set<Block> targets = parseTargets();
        if (targets.isEmpty()) return;
        int radius = (int) Math.ceil(range.get());
        BlockPos origin = client.player.getBlockPos();
        boolean tw = throughWalls.enabled();
        for (BlockPos pos : BlockPos.iterateOutwards(origin, radius, radius, radius)) {
            if (client.player.squaredDistanceTo(pos.toCenterPos()) > range.get() * range.get()) continue;
            if (targets.contains(client.world.getBlockState(pos).getBlock())) {
                RenderUtil.drawBlockBox(context, pos, RenderUtil.Color.rgba(118, 156, 255, 220), tw);
            }
        }
    }

    private Set<Block> parseTargets() {
        if (!blockPreset.is("Custom")) {
            return getPresetBlocks();
        }
        Set<Block> result = new HashSet<>();
        for (String raw : blocks.get().split(",")) {
            String id = raw.trim().toLowerCase(Locale.ROOT);
            if (!id.isEmpty()) {
                Registries.BLOCK.getOptionalValue(Identifier.of(id)).ifPresent(result::add);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Set<Block> getPresetBlocks() {
        return switch (blockPreset.get()) {
            case "Ores" -> Set.of(
                Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
                Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
                Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
                Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
                Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
                Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
                Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
                Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
                Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE,
                Blocks.ANCIENT_DEBRIS
            );
            case "Rare Ores" -> Set.of(
                Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
                Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
                Blocks.ANCIENT_DEBRIS
            );
            case "All Ores" -> Set.of(
                Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
                Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
                Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
                Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
                Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE,
                Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
                Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
                Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
                Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE,
                Blocks.ANCIENT_DEBRIS,
                Blocks.NETHERITE_BLOCK, Blocks.DIAMOND_BLOCK,
                Blocks.EMERALD_BLOCK, Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK
            );
            case "Diamonds" -> Set.of(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DIAMOND_BLOCK);
            case "Netherite" -> Set.of(Blocks.ANCIENT_DEBRIS, Blocks.NETHERITE_BLOCK);
            case "Emeralds" -> Set.of(Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE, Blocks.EMERALD_BLOCK);
            case "Iron" -> Set.of(Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.IRON_BLOCK, Blocks.RAW_IRON_BLOCK);
            case "Gold" -> Set.of(Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.GOLD_BLOCK, Blocks.RAW_GOLD_BLOCK, Blocks.NETHER_GOLD_ORE);
            case "Lapis" -> Set.of(Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE, Blocks.LAPIS_BLOCK);
            case "Redstone" -> Set.of(Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.REDSTONE_BLOCK);
            case "Coal" -> Set.of(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.COAL_BLOCK);
            case "Ancient Debris" -> Set.of(Blocks.ANCIENT_DEBRIS);
            case "Chests" -> Set.of(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL);
            case "Spawners" -> Set.of(Blocks.SPAWNER, Blocks.TRIAL_SPAWNER);
            case "Bookshelves" -> Set.of(Blocks.BOOKSHELF, Blocks.CHISELED_BOOKSHELF, Blocks.LECTERN, Blocks.ENCHANTING_TABLE);
            case "Crafting" -> Set.of(Blocks.CRAFTING_TABLE, Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL,
                Blocks.GRINDSTONE, Blocks.SMITHING_TABLE, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER);
            default -> Set.of();
        };
    }
}
