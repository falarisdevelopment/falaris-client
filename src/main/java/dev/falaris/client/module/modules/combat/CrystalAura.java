package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public final class CrystalAura extends CombatModule {
    private final DoubleSetting targetRange = setting(new DoubleSetting("Target Range", "Maximum target distance.", 8.0, 1.0, 12.0));
    private final DoubleSetting breakRange = setting(new DoubleSetting("Break Range", "Maximum crystal break distance.", 5.0, 1.0, 6.0));
    private final DoubleSetting placeRange = setting(new DoubleSetting("Place Range", "Maximum crystal place distance.", 4.5, 1.0, 6.0));
    private final DoubleSetting minDamage = setting(new DoubleSetting("Min Damage", "Minimum estimated target damage.", 6.0, 1.0, 20.0));
    private final DoubleSetting maxSelfDamage = setting(new DoubleSetting("Max Self Damage", "Maximum estimated self damage.", 8.0, 1.0, 20.0));
    private final DoubleSetting rotationSpeed = setting(new DoubleSetting("Rotation Speed", "Maximum rotation step per tick.", 20.0, 1.0, 60.0));
    private final IntegerSetting actionJitter = setting(new IntegerSetting("Action Jitter", "Random extra ticks between crystal actions.", 2, 0, 8));
    private final BooleanSetting place = setting(new BooleanSetting("Place", "Place end crystals.", true));
    private final BooleanSetting explode = setting(new BooleanSetting("Break", "Break end crystals.", true));
    private final BooleanSetting rotate = setting(new BooleanSetting("Rotate", "Face crystal actions.", true));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Allow crystals without line of sight.", false));
    private final ModeSetting priority = setting(new ModeSetting("Priority", "Target sorting mode.", "Distance", "Distance", "Health", "Angle"));

    public CrystalAura() {
        super("CrystalAura", "Places and breaks end crystals near selected targets.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        LivingEntity target = CombatUtil.bestLivingTarget(client, targetRange.get(), true, true, false, throughWalls.enabled(), priority.get()).orElse(null);
        if (target == null) {
            return;
        }

        if (explode.enabled() && tryBreak(client)) {
            return;
        }
        if (place.enabled()) {
            tryPlace(client, target);
        }
    }

    private boolean tryBreak(MinecraftClient client) {
        Optional<EndCrystalEntity> crystal = CombatUtil.bestCrystal(client, breakRange.get(), throughWalls.enabled());
        if (crystal.isEmpty()) {
            return false;
        }

        EndCrystalEntity entity = crystal.get();
        if (rotate.enabled()) {
            CombatUtil.face(rotations(), client.player, new net.minecraft.util.math.Vec3d(entity.getX(), entity.getY(), entity.getZ()), rotationSpeed.get());
        }

        if (actionReady(2, actionJitter.get())) {
            CombatUtil.attack(client, entity);
            return true;
        }
        return false;
    }

    private void tryPlace(MinecraftClient client, LivingEntity target) {
        int slot = CombatUtil.findItem(client.player, Items.END_CRYSTAL);
        if (slot == -1) {
            return;
        }

        Optional<BlockPos> best = findBestCrystalBase(client, target);
        if (best.isEmpty()) {
            return;
        }

        BlockPos pos = best.get();
        if (rotate.enabled()) {
            CombatUtil.face(rotations(), client.player, Vec3d.ofCenter(pos.up()), rotationSpeed.get());
        }

        if (actionReady(3, actionJitter.get())) {
            CombatUtil.selectHotbarSlot(client.player, slot);
            CombatUtil.interactBlock(client, pos, Direction.UP);
        }
    }

    private Optional<BlockPos> findBestCrystalBase(MinecraftClient client, LivingEntity target) {
        int radius = (int) Math.ceil(placeRange.get());
        BlockPos origin = target.getBlockPos();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.iterateOutwards(origin, radius, 3, radius)) {
            Vec3d crystalPos = Vec3d.ofCenter(pos.up());
            if (!canPlaceCrystal(client, pos)) {
                continue;
            }
            if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > placeRange.get() * placeRange.get()) {
                continue;
            }
            if (estimatedDamage(new net.minecraft.util.math.Vec3d(target.getX(), target.getY(), target.getZ()), crystalPos) < minDamage.get()) {
                continue;
            }
            if (estimatedDamage(new net.minecraft.util.math.Vec3d(client.player.getX(), client.player.getY(), client.player.getZ()), crystalPos) > maxSelfDamage.get()) {
                continue;
            }

            double distance = target.squaredDistanceTo(crystalPos);
            if (distance < bestDistance) {
                best = pos.toImmutable();
                bestDistance = distance;
            }
        }

        return Optional.ofNullable(best);
    }

    private boolean canPlaceCrystal(MinecraftClient client, BlockPos pos) {
        boolean base = client.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) || client.world.getBlockState(pos).isOf(Blocks.BEDROCK);
        return base && client.world.getBlockState(pos.up()).isAir() && client.world.getBlockState(pos.up(2)).isAir();
    }

    private double estimatedDamage(Vec3d target, Vec3d crystal) {
        double distance = target.distanceTo(crystal);
        return Math.max(0.0, 12.0 - distance * 2.0);
    }
}
