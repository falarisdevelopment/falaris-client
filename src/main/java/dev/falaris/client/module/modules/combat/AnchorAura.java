package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public final class AnchorAura extends CombatModule {
    private final DoubleSetting targetRange = setting(new DoubleSetting("Target Range", "Maximum target distance.", 6.0, 1.0, 10.0));
    private final DoubleSetting actionRange = setting(new DoubleSetting("Action Range", "Maximum anchor interaction distance.", 4.5, 1.0, 6.0));
    private final DoubleSetting minDamage = setting(new DoubleSetting("Min Damage", "Minimum estimated target damage.", 5.0, 1.0, 20.0));
    private final DoubleSetting maxSelfDamage = setting(new DoubleSetting("Max Self Damage", "Maximum estimated self damage.", 8.0, 1.0, 20.0));
    private final DoubleSetting rotationSpeed = setting(new DoubleSetting("Rotation Speed", "Maximum rotation step per tick.", 18.0, 1.0, 60.0));
    private final IntegerSetting actionJitter = setting(new IntegerSetting("Action Jitter", "Random extra ticks between anchor actions.", 3, 0, 10));
    private final ModeSetting priority = setting(new ModeSetting("Priority", "Target sorting mode.", "Distance", "Distance", "Health", "Angle"));
    private final BooleanSetting placeAnchors = setting(new BooleanSetting("Place Anchors", "Place anchors near targets when none are available.", true));
    private final BooleanSetting chargeAnchors = setting(new BooleanSetting("Charge Anchors", "Charge anchors with glowstone.", true));
    private final BooleanSetting detonateAnchors = setting(new BooleanSetting("Detonate Anchors", "Use charged anchors.", true));
    private final BooleanSetting rotate = setting(new BooleanSetting("Rotate", "Face anchor actions.", true));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Allow targets without line of sight.", false));

    public AnchorAura() {
        super("AnchorAura", "Places, charges, and uses respawn anchors near selected targets.");
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

        Optional<BlockPos> anchor = findBestAnchor(client, target);
        if (anchor.isPresent()) {
            useAnchor(client, anchor.get());
        } else if (placeAnchors.enabled()) {
            placeAnchor(client, target);
        }
    }

    private void useAnchor(MinecraftClient client, BlockPos pos) {
        BlockState state = client.world.getBlockState(pos);
        int charges = state.get(RespawnAnchorBlock.CHARGES);
        if (rotate.enabled()) {
            CombatUtil.face(rotations(), client.player, Vec3d.ofCenter(pos), rotationSpeed.get());
        }

        if (charges <= 0 && chargeAnchors.enabled()) {
            int slot = CombatUtil.findItem(client.player, Items.GLOWSTONE);
            if (slot != -1 && actionReady(3, actionJitter.get())) {
                CombatUtil.selectHotbarSlot(client.player, slot);
                CombatUtil.interactBlock(client, pos, Direction.UP);
            }
            return;
        }

        if (charges > 0 && detonateAnchors.enabled() && actionReady(3, actionJitter.get())) {
            int emptyHandSlot = findNonGlowstoneSlot(client);
            if (emptyHandSlot != -1) {
                CombatUtil.selectHotbarSlot(client.player, emptyHandSlot);
            }
            CombatUtil.interactBlock(client, pos, Direction.UP);
        }
    }

    private void placeAnchor(MinecraftClient client, LivingEntity target) {
        int slot = CombatUtil.findItem(client.player, Items.RESPAWN_ANCHOR);
        if (slot == -1) {
            return;
        }

        Optional<BlockPos> placePos = findBestAnchorPlacement(client, target);
        if (placePos.isEmpty()) {
            return;
        }

        BlockPos pos = placePos.get();
        if (rotate.enabled()) {
            CombatUtil.face(rotations(), client.player, Vec3d.ofCenter(pos), rotationSpeed.get());
        }

        if (actionReady(4, actionJitter.get())) {
            CombatUtil.selectHotbarSlot(client.player, slot);
            CombatUtil.interactBlock(client, pos.down(), Direction.UP);
        }
    }

    private Optional<BlockPos> findBestAnchor(MinecraftClient client, LivingEntity target) {
        int radius = (int) Math.ceil(actionRange.get());
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.iterateOutwards(target.getBlockPos(), radius, 3, radius)) {
            Vec3d center = Vec3d.ofCenter(pos);
            if (!client.world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR)) {
                continue;
            }
            if (client.player.squaredDistanceTo(center) > actionRange.get() * actionRange.get()) {
                continue;
            }
            if (estimatedDamage(new net.minecraft.util.math.Vec3d(target.getX(), target.getY(), target.getZ()), center) < minDamage.get()) {
                continue;
            }
            if (estimatedDamage(new net.minecraft.util.math.Vec3d(client.player.getX(), client.player.getY(), client.player.getZ()), center) > maxSelfDamage.get()) {
                continue;
            }

            double distance = target.squaredDistanceTo(center);
            if (distance < bestDistance) {
                best = pos.toImmutable();
                bestDistance = distance;
            }
        }

        return Optional.ofNullable(best);
    }

    private Optional<BlockPos> findBestAnchorPlacement(MinecraftClient client, LivingEntity target) {
        int radius = (int) Math.ceil(actionRange.get());
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.iterateOutwards(target.getBlockPos(), radius, 2, radius)) {
            Vec3d center = Vec3d.ofCenter(pos);
            if (!client.world.getBlockState(pos).isAir()) {
                continue;
            }
            if (client.world.getBlockState(pos.down()).isAir()) {
                continue;
            }
            if (client.player.squaredDistanceTo(center) > actionRange.get() * actionRange.get()) {
                continue;
            }
            if (estimatedDamage(new net.minecraft.util.math.Vec3d(target.getX(), target.getY(), target.getZ()), center) < minDamage.get()) {
                continue;
            }
            if (estimatedDamage(new net.minecraft.util.math.Vec3d(client.player.getX(), client.player.getY(), client.player.getZ()), center) > maxSelfDamage.get()) {
                continue;
            }

            double distance = target.squaredDistanceTo(center);
            if (distance < bestDistance) {
                best = pos.toImmutable();
                bestDistance = distance;
            }
        }

        return Optional.ofNullable(best);
    }

    private int findNonGlowstoneSlot(MinecraftClient client) {
        for (int slot = 0; slot < 9; slot++) {
            if (!client.player.getInventory().getStack(slot).isOf(Items.GLOWSTONE)) {
                return slot;
            }
        }
        return -1;
    }

    private double estimatedDamage(Vec3d target, Vec3d anchor) {
        double distance = target.distanceTo(anchor);
        return Math.max(0.0, 10.0 - distance * 1.8);
    }
}
