package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
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

public final class XRay extends RenderModule {
    private final StringSetting ores = setting(new StringSetting("Ores", "Comma-separated ore ids.", "minecraft:diamond_ore,minecraft:deepslate_diamond_ore,minecraft:emerald_ore,minecraft:ancient_debris,minecraft:gold_ore,minecraft:deepslate_gold_ore,minecraft:iron_ore,minecraft:deepslate_iron_ore,minecraft:coal_ore,minecraft:deepslate_coal_ore,minecraft:copper_ore,minecraft:deepslate_copper_ore,minecraft:redstone_ore,minecraft:deepslate_redstone_ore,minecraft:lapis_ore,minecraft:deepslate_lapis_ore,minecraft:nether_quartz_ore,minecraft:nether_gold_ore"));
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Ore scan range.", 64.0, 8.0, 128.0));
    private final BooleanSetting fullbright = setting(new BooleanSetting("Fullbright", "Apply high gamma while XRay is enabled.", true));
    private Double previousGamma;

    public XRay() {
        super("XRay", "Highlights configured ore blocks through the world.");
    }

    @Override
    protected void onRenderTick(MinecraftClient client) {
        if (fullbright.enabled()) {
            if (previousGamma == null) {
                previousGamma = client.options.getGamma().getValue();
            }
            client.options.getGamma().setValue(15.0);
        }
    }

    @Override
    protected void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Set<Block> targets = parseTargets();
        if (targets.isEmpty()) return;

        int radius = (int) Math.min(range.get(), 64.0);
        BlockPos origin = client.player.getBlockPos();

        for (BlockPos pos : BlockPos.iterateOutwards(origin, radius, radius, radius)) {
            if (client.world.getBlockState(pos).isAir()) continue;
            if (client.player.squaredDistanceTo(pos.toCenterPos()) > range.get() * range.get()) continue;
            if (targets.contains(client.world.getBlockState(pos).getBlock())) {
                RenderUtil.drawBlockBox(context, pos, RenderUtil.Color.rgba(178, 112, 255, 180));
            }
        }
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (previousGamma != null) {
            client.options.getGamma().setValue(previousGamma);
            previousGamma = null;
        }
    }

    private Set<Block> parseTargets() {
        Set<Block> result = new HashSet<>();
        for (String raw : ores.get().split(",")) {
            String id = raw.trim().toLowerCase(Locale.ROOT);
            if (!id.isEmpty()) {
                Registries.BLOCK.getOptionalValue(Identifier.of(id)).ifPresent(result::add);
            }
        }
        return result;
    }
}
