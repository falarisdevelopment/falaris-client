package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class Nuker extends MiscModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Block selection mode.", "Radius", "Radius", "Flatten", "Forward"));
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum block break range.", 4.5, 1.0, 6.0));
    private final IntegerSetting blocksPerTick = setting(new IntegerSetting("Blocks/Tick", "Maximum break attempts per tick.", 2, 1, 12));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks between nuker passes.", 1, 1, 20));
    private final IntegerSetting jitter = setting(new IntegerSetting("Jitter", "Random extra ticks between passes.", 1, 0, 10));
    private final BooleanSetting ignoreAir = setting(new BooleanSetting("Ignore Air", "Skip air and fluids.", true));
    private final BooleanSetting creativeOnly = setting(new BooleanSetting("Creative Only", "Only run while creative.", false));
    private final BooleanSetting instant = setting(new BooleanSetting("Instant", "Break blocks instantly (creative mode).", true));

    public Nuker() {
        super("Nuker", "Breaks nearby blocks according to a configured selection mode.");
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) {
            return;
        }
        if (creativeOnly.enabled() && !client.player.getAbilities().creativeMode) {
            return;
        }
        if (!ready(delay.get(), jitter.get())) {
            return;
        }

        int broken = 0;
        int radius = (int) Math.ceil(range.get());
        BlockPos origin = client.player.getBlockPos();
        for (BlockPos pos : BlockPos.iterateOutwards(origin, radius, radius, radius)) {
            if (broken >= blocksPerTick.get()) break;
            if (!allowed(client, pos)) continue;
            Direction side = sideFor(client.player.getEyePos(), Vec3d.ofCenter(pos));

            if (instant.enabled() && client.player.getAbilities().creativeMode) {
                client.interactionManager.attackBlock(pos, side);
            } else {
                client.interactionManager.attackBlock(pos, side);
                client.interactionManager.updateBlockBreakingProgress(pos, side);
            }
            client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            broken++;
        }
    }

    private boolean allowed(MinecraftClient client, BlockPos pos) {
        if (client.player.squaredDistanceTo(pos.toCenterPos()) > range.get() * range.get()) return false;
        if (mode.is("Flatten") && pos.getY() > client.player.getBlockY()) return false;
        if (mode.is("Forward")) {
            Vec3d toBlock = pos.toCenterPos().subtract(client.player.getEyePos()).normalize();
            if (client.player.getRotationVector().dotProduct(toBlock) < 0.82) return false;
        }
        BlockState state = client.world.getBlockState(pos);
        return !(ignoreAir.enabled() && (state.isAir() || !state.getFluidState().isEmpty()));
    }

    private Direction sideFor(Vec3d from, Vec3d to) {
        Vec3d diff = to.subtract(from);
        return Direction.getFacing(diff.x, diff.y, diff.z);
    }
}
