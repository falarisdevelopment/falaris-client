package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public final class BedAura extends CombatModule {
    private final DoubleSetting targetRange = setting(new DoubleSetting("Target Range", "Maximum target distance.", 6.0, 1.0, 10.0));
    private final DoubleSetting actionRange = setting(new DoubleSetting("Action Range", "Maximum bed interaction distance.", 4.5, 1.0, 6.0));
    private final DoubleSetting minDamage = setting(new DoubleSetting("Min Damage", "Minimum estimated target damage.", 5.0, 1.0, 20.0));
    private final DoubleSetting maxSelfDamage = setting(new DoubleSetting("Max Self Damage", "Maximum estimated self damage.", 8.0, 1.0, 20.0));
    private final DoubleSetting rotationSpeed = setting(new DoubleSetting("Rotation Speed", "Maximum rotation step per tick.", 18.0, 1.0, 60.0));
    private final IntegerSetting actionJitter = setting(new IntegerSetting("Action Jitter", "Random extra ticks between actions.", 3, 0, 10));
    private final ModeSetting priority = setting(new ModeSetting("Priority", "Target sorting mode.", "Distance", "Distance", "Health", "Angle"));
    private final BooleanSetting rotate = setting(new BooleanSetting("Rotate", "Face bed actions.", true));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Allow targets without line of sight.", false));
    private final BooleanSetting placeBeds = setting(new BooleanSetting("Place Beds", "Place beds near targets.", true));
    private final BooleanSetting autoSwitch = setting(new BooleanSetting("Auto Switch", "Auto switch to bed item.", true));

    public BedAura() {
        super("BedAura", "Places and detonates beds near selected targets.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        LivingEntity target = CombatUtil.bestLivingTarget(client, targetRange.get(), true, true, false, throughWalls.enabled(), priority.get()).orElse(null);
        if (target == null) return;

        Optional<BlockPos> bed = findBestBed(client, target);
        if (bed.isPresent()) {
            detonateBed(client, bed.get());
        } else if (placeBeds.enabled()) {
            placeBed(client, target);
        }
    }

    private void detonateBed(MinecraftClient client, BlockPos pos) {
        if (rotate.enabled()) {
            CombatUtil.face(rotations(), client.player, Vec3d.ofCenter(pos), rotationSpeed.get());
        }
        if (actionReady(3, actionJitter.get())) {
            CombatUtil.interactBlock(client, pos, Direction.UP);
        }
    }

    private void placeBed(MinecraftClient client, LivingEntity target) {
        int slot = CombatUtil.findItem(client.player, Items.WHITE_BED);
        if (slot == -1) slot = CombatUtil.findItem(client.player, Items.RED_BED);
        if (slot == -1) return;

        if (autoSwitch.enabled() && slot < 9) {
            CombatUtil.selectHotbarSlot(client.player, slot);
        }

        Optional<BlockPos> placePos = findBestBedPlacement(client, target);
        if (placePos.isEmpty()) return;

        BlockPos pos = placePos.get();
        if (rotate.enabled()) {
            CombatUtil.face(rotations(), client.player, Vec3d.ofCenter(pos), rotationSpeed.get());
        }
        if (actionReady(4, actionJitter.get())) {
            CombatUtil.interactBlock(client, pos.down(), Direction.UP);
        }
    }

    private Optional<BlockPos> findBestBed(MinecraftClient client, LivingEntity target) {
        int radius = (int) Math.ceil(actionRange.get());
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.iterateOutwards(target.getBlockPos(), radius, 3, radius)) {
            Vec3d center = Vec3d.ofCenter(pos);
            BlockState state = client.world.getBlockState(pos);
            if (!(state.getBlock() instanceof BedBlock)) continue;
            if (client.player.squaredDistanceTo(center) > actionRange.get() * actionRange.get()) continue;
            if (estimatedDamage(new Vec3d(target.getX(), target.getY(), target.getZ()), center) < minDamage.get()) continue;
            if (estimatedDamage(new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ()), center) > maxSelfDamage.get()) continue;

            double distance = target.squaredDistanceTo(center);
            if (distance < bestDistance) {
                best = pos.toImmutable();
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    private Optional<BlockPos> findBestBedPlacement(MinecraftClient client, LivingEntity target) {
        int radius = (int) Math.ceil(actionRange.get());
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.iterateOutwards(target.getBlockPos(), radius, 2, radius)) {
            Vec3d center = Vec3d.ofCenter(pos);
            if (!client.world.getBlockState(pos).isAir()) continue;
            if (client.world.getBlockState(pos.down()).isAir()) continue;
            if (client.player.squaredDistanceTo(center) > actionRange.get() * actionRange.get()) continue;
            if (estimatedDamage(new Vec3d(target.getX(), target.getY(), target.getZ()), center) < minDamage.get()) continue;
            if (estimatedDamage(new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ()), center) > maxSelfDamage.get()) continue;

            double distance = target.squaredDistanceTo(center);
            if (distance < bestDistance) {
                best = pos.toImmutable();
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    private double estimatedDamage(Vec3d target, Vec3d bed) {
        double distance = target.distanceTo(bed);
        return Math.max(0.0, 10.0 - distance * 1.8);
    }
}
