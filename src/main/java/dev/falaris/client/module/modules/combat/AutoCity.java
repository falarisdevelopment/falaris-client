package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.Random;

public final class AutoCity extends CombatModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Mining range.", 4.5, 1.0, 6.0));
    private final DoubleSetting targetRange = setting(new DoubleSetting("Target Range", "Target detection range.", 6.0, 1.0, 12.0));
    private final BooleanSetting autoSwitch = setting(new BooleanSetting("Auto Switch", "Auto-switch to pickaxe.", true));
    private final BooleanSetting rotate = setting(new BooleanSetting("Rotate", "Face blocks being mined.", true));
    private final BooleanSetting toggleAfterCity = setting(new BooleanSetting("Toggle After City", "Disable after breaking a surround block.", false));
    private final ModeSetting targetMode = setting(new ModeSetting("Target Mode", "Which side to mine.", "Nearest", "Nearest", "Target", "Both"));
    private final IntegerSetting breakDelay = setting(new IntegerSetting("Break Delay", "Ticks between block attempts.", 3, 1, 10));

    private int tickCounter;
    private final Random random = new Random();

    public AutoCity() {
        super("AutoCity", "Mines target's surround blocks to enable crystal hits.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        tickCounter++;
        if (tickCounter < breakDelay.get() + random.nextInt(2)) return;
        tickCounter = 0;

        LivingEntity target = CombatUtil.bestLivingTarget(client, targetRange.get(), true, true, false, false, "Distance").orElse(null);
        if (target == null) return;

        BlockPos targetFeet = target.getBlockPos();
        BlockPos minePos = null;

        if (targetMode.is("Nearest") || targetMode.is("Both")) {
            minePos = findNearestSurround(client, targetFeet);
        }
        if (minePos == null && (targetMode.is("Target") || targetMode.is("Both"))) {
            minePos = findSurroundBlockTowards(client, targetFeet);
        }
        if (minePos == null) return;

        if (autoSwitch.enabled()) {
            int pickSlot = findPickaxe(client);
            if (pickSlot >= 0) {
                client.player.getInventory().setSelectedSlot(pickSlot);
            }
        }

        if (rotate.enabled()) {
            Vec3d targetPos = Vec3d.ofCenter(minePos);
            float[] rots = CombatUtil.rotationsTo(client.player, targetPos);
            rotations().setMaxStep(20.0f);
            rotations().rotateTo(rots[0], rots[1], 2);
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = minePos.offset(dir);
            if (!client.world.getBlockState(neighbor).isAir()) {
                Vec3d hit = Vec3d.ofCenter(neighbor).add(Vec3d.of(dir.getOpposite().getVector()).multiply(0.5));
                client.interactionManager.attackBlock(minePos, dir);
                client.player.swingHand(Hand.MAIN_HAND);
                break;
            }
        }

        if (toggleAfterCity.enabled() && isSurroundBlock(client, minePos)) {
            setEnabled(false);
        }
    }

    private BlockPos findNearestSurround(MinecraftClient client, BlockPos feet) {
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (BlockPos pos : new BlockPos[]{feet.north(), feet.south(), feet.east(), feet.west(),
                feet.north().east(), feet.north().west(), feet.south().east(), feet.south().west()}) {
            if (!isSurroundBlock(client, pos)) continue;
            double dist = client.player.squaredDistanceTo(Vec3d.ofCenter(pos));
            if (dist > range.get() * range.get()) continue;
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = pos;
            }
        }
        return nearest;
    }

    private BlockPos findSurroundBlockTowards(MinecraftClient client, BlockPos targetFeet) {
        BlockPos playerFeet = client.player.getBlockPos();
        Direction towards = Direction.getFacing(targetFeet.getX() - playerFeet.getX(), 0, targetFeet.getZ() - playerFeet.getZ());
        BlockPos check = targetFeet.offset(towards);
        if (isSurroundBlock(client, check) && client.player.squaredDistanceTo(Vec3d.ofCenter(check)) <= range.get() * range.get()) {
            return check;
        }
        return null;
    }

    private boolean isSurroundBlock(MinecraftClient client, BlockPos pos) {
        if (client.world == null) return false;
        BlockState state = client.world.getBlockState(pos);
        return state.isOf(Blocks.OBSIDIAN) || state.isOf(Blocks.END_STONE) || state.isOf(Blocks.BEDROCK)
                || state.isOf(Blocks.CRYING_OBSIDIAN) || state.isOf(Blocks.NETHERITE_BLOCK);
    }

    private int findPickaxe(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            var stack = client.player.getInventory().getStack(i);
            if (stack.isOf(Items.NETHERITE_PICKAXE) || stack.isOf(Items.DIAMOND_PICKAXE)
                    || stack.isOf(Items.IRON_PICKAXE)) {
                return i;
            }
        }
        return -1;
    }
}
