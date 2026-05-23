package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.StringSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class BlockESP extends RenderModule {
    private final StringSetting blocks = setting(new StringSetting("Blocks", "Comma-separated block ids to highlight.", "minecraft:diamond_ore,minecraft:deepslate_diamond_ore,minecraft:ancient_debris"));
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Block scan range.", 32.0, 4.0, 96.0));

    public BlockESP() {
        super("BlockESP", "Highlights configured blocks nearby.");
    }

    @Override
    protected void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        Set<Block> targets = parseTargets();
        int radius = (int) Math.ceil(range.get());
        BlockPos origin = client.player.getBlockPos();
        for (BlockPos pos : BlockPos.iterateOutwards(origin, radius, radius, radius)) {
            if (client.player.squaredDistanceTo(pos.toCenterPos()) > range.get() * range.get()) {
                continue;
            }
            if (targets.contains(client.world.getBlockState(pos).getBlock())) {
                RenderUtil.drawBlockBox(context, pos, RenderUtil.Color.rgba(118, 156, 255, 220));
            }
        }
    }

    private Set<Block> parseTargets() {
        Set<Block> result = new HashSet<>();
        for (String raw : blocks.get().split(",")) {
            String id = raw.trim().toLowerCase(Locale.ROOT);
            if (!id.isEmpty()) {
                Registries.BLOCK.getOptionalValue(Identifier.of(id)).ifPresent(result::add);
            }
        }
        return result;
    }
}
