package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

public final class HoleESP extends RenderModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Scan range.", 8.0, 4.0, 32.0));
    private final ModeSetting holeType = setting(new ModeSetting("Hole Type", "Type of holes to show.", "Both", "Both", "Double", "Single", "Both"));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Show through blocks.", true));

    private final List<Hole> holes = new ArrayList<>();
    private int scanTimer;

    public HoleESP() {
        super("HoleESP", "Highlights safe obsidian holes for crystal PVP.");
    }

    @Override
    protected void onRenderTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        scanTimer++;
        if (scanTimer < 20) return; // re-scan every 20 ticks
        scanTimer = 0;
        scan(client);
    }

    private void scan(MinecraftClient client) {
        holes.clear();
        BlockPos center = client.player.getBlockPos();
        int r = range.get().intValue();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -3; y <= 1; y++) {
                    BlockPos pos = center.add(x, y, z);
                    if (client.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > range.get() * range.get())
                        continue;
                    if (!client.world.getBlockState(pos).isAir()) continue;
                    if (!isSafeBlock(client, pos.down())) continue;

                    boolean n = isSafeBlock(client, pos.north());
                    boolean s = isSafeBlock(client, pos.south());
                    boolean e = isSafeBlock(client, pos.east());
                    boolean w = isSafeBlock(client, pos.west());

                    if (n && s && e && w) {
                        // Single hole
                        holes.add(new Hole(pos, false));
                    }
                }
            }
        }
    }

    private boolean isSafeBlock(MinecraftClient client, BlockPos pos) {
        var state = client.world.getBlockState(pos);
        return state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.CRYING_OBSIDIAN) ||
               state.isOf(Blocks.NETHERITE_BLOCK) || state.isOf(Blocks.ANVIL) ||
               state.isOf(Blocks.BEDROCK) || state.isOf(Blocks.ENDER_CHEST);
    }

    @Override
    protected void onWorldRender(WorldRenderContext context) {
        for (Hole h : holes) {
            RenderUtil.Color c = h.doubleHole
                ? RenderUtil.Color.rgba(120, 200, 255, 180)
                : RenderUtil.Color.rgba(120, 255, 140, 180);
            RenderUtil.drawBox(context, new Box(h.pos), c, throughWalls.enabled());
        }
    }

    private record Hole(BlockPos pos, boolean doubleHole) {}

    @Override protected void onEnable() { super.onEnable(); holes.clear(); scanTimer = 100; }
}
