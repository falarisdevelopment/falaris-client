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

import java.util.ArrayList;
import java.util.List;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AnchorAura extends CombatModule {
    private final DoubleSetting targetRange = setting(new DoubleSetting("Target Range", "Maximum target distance.", 6.0, 1.0, 10.0));
    private final DoubleSetting actionRange = setting(new DoubleSetting("Action Range", "Maximum anchor interaction distance.", 3.0, 1.0, 4.0));
    private final DoubleSetting minDamage = setting(new DoubleSetting("Min Damage", "Minimum estimated target damage.", 5.0, 1.0, 20.0));
    private final DoubleSetting maxSelfDamage = setting(new DoubleSetting("Max Self Damage", "Maximum estimated self damage.", 8.0, 1.0, 20.0));
    private final DoubleSetting rotationSpeed = setting(new DoubleSetting("Rotation Speed", "Maximum rotation step per tick.", 18.0, 1.0, 60.0));
    private final IntegerSetting actionJitter = setting(new IntegerSetting("Action Jitter", "Random extra ticks between anchor actions.", 2, 0, 10));
    private final ModeSetting priority = setting(new ModeSetting("Priority", "Target sorting mode.", "Distance", "Distance", "Health", "Angle"));
    private final BooleanSetting placeAnchors = setting(new BooleanSetting("Place Anchors", "Place anchors near targets when none are available.", true));
    private final BooleanSetting chargeAnchors = setting(new BooleanSetting("Charge Anchors", "Charge anchors with glowstone.", true));
    private final BooleanSetting detonateAnchors = setting(new BooleanSetting("Detonate Anchors", "Use charged anchors.", true));
    private final BooleanSetting rotate = setting(new BooleanSetting("Rotate", "Face anchor actions.", true));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Allow targets without line of sight.", false));
    private final BooleanSetting autoSafeAnchor = setting(new BooleanSetting("Auto Safe Anchor", "Only detonate when self damage is below threshold.", true));
    private final DoubleSetting safeAnchorMaxSelf = setting(new DoubleSetting("Safe Max Self", "Max safe self damage for auto-safe mode.", 6.0, 1.0, 16.0));
    private final ModeSetting anchorMode = setting(new ModeSetting("Anchor Mode", "Single or double anchor.", "Single", "Single", "Double"));

    private int phaseTick = 0;

    public AnchorAura() {
        super("AnchorAura", "Places, charges, and uses respawn anchors near selected targets. Double-anchor mode.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        phaseTick++;

        LivingEntity target = CombatUtil.bestLivingTarget(client, targetRange.get(), true, true, false, throughWalls.enabled(), priority.get()).orElse(null);
        if (target == null) return;

        if (anchorMode.is("Double")) {
            tickDoubleAnchor(client, target);
            return;
        }

        tickSingleAnchor(client, target);
    }

    private void tickSingleAnchor(MinecraftClient client, LivingEntity target) {
        if (detonateAnchors.enabled()) {
            Optional<BlockPos> charged = findChargedAnchorNearTarget(client, target);
            if (charged.isPresent()) {
                BlockPos pos = charged.get();
                Vec3d center = Vec3d.ofCenter(pos);
                double selfDmg = estimatedDamage(Vec3d.ofCenter(client.player.getBlockPos()), center);
                if (!autoSafeAnchor.enabled() || selfDmg <= safeAnchorMaxSelf.get()) {
                    if (rotate.enabled()) {
                        CombatUtil.face(rotations(), client.player, center, rotationSpeed.get());
                    }
                    if (actionReady(1, actionJitter.get())) {
                        int heldSlot = client.player.getInventory().getSelectedSlot();
                        int nonGlowstone = findNonGlowstoneSlot(client);
                        if (nonGlowstone >= 0) {
                            CombatUtil.selectHotbarSlot(client.player, nonGlowstone);
                        }
                        CombatUtil.interactBlock(client, pos, Direction.UP);
                        CombatUtil.selectHotbarSlot(client.player, heldSlot);
                        return;
                    }
                }
            }
        }

        Optional<BlockPos> existing = findExistingUnchargedAnchor(client, target);
        if (existing.isPresent() && chargeAnchors.enabled()) {
            chargeAnchor(client, existing.get());
            return;
        }

        if (placeAnchors.enabled()) {
            placeAnchor(client, target);
        }
    }

    private void tickDoubleAnchor(MinecraftClient client, LivingEntity target) {
        List<BlockPos> charged = findChargedAnchorsNearTargetList(client, target);
        if (charged.size() >= 2) {
            detonateAnchorsList(client, charged);
            return;
        }

        List<BlockPos> uncharged = findUnchargedAnchorsNearTargetList(client, target);
        int needed = 2 - charged.size();
        int charged_count = chargeAnchorsList(client, uncharged, needed);

        if (charged.size() + charged_count >= 2) {
            List<BlockPos> allCharged = findChargedAnchorsNearTargetList(client, target);
            detonateAnchorsList(client, allCharged);
            return;
        }

        int toPlace = 2 - (charged.size() + charged_count);
        if (placeAnchors.enabled()) {
            placeAnchorsCount(client, target, toPlace);
        }
    }

    private Optional<BlockPos> findChargedAnchorNearTarget(MinecraftClient client, LivingEntity target) {
        int radius = (int) Math.ceil(actionRange.get());
        BlockPos best = null;
        double bestDamage = -1;

        for (BlockPos pos : BlockPos.iterateOutwards(target.getBlockPos(), radius, 3, radius)) {
            BlockState state = client.world.getBlockState(pos);
            if (!state.isOf(Blocks.RESPAWN_ANCHOR)) continue;
            int charges = state.get(RespawnAnchorBlock.CHARGES);
            if (charges <= 0) continue;
            Vec3d center = Vec3d.ofCenter(pos);
            if (client.player.squaredDistanceTo(center) > actionRange.get() * actionRange.get()) continue;
            Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
            double dmg = estimatedDamage(targetPos, center);
            if (dmg >= minDamage.get() && dmg > bestDamage) {
                best = pos.toImmutable();
                bestDamage = dmg;
            }
        }
        return Optional.ofNullable(best);
    }

    private List<BlockPos> findChargedAnchorsNearTargetList(MinecraftClient client, LivingEntity target) {
        List<BlockPos> list = new ArrayList<>();
        int radius = (int) Math.ceil(actionRange.get());
        for (BlockPos pos : BlockPos.iterateOutwards(target.getBlockPos(), radius, 3, radius)) {
            BlockState state = client.world.getBlockState(pos);
            if (!state.isOf(Blocks.RESPAWN_ANCHOR)) continue;
            int charges = state.get(RespawnAnchorBlock.CHARGES);
            if (charges <= 0) continue;
            Vec3d center = Vec3d.ofCenter(pos);
            if (client.player.squaredDistanceTo(center) > actionRange.get() * actionRange.get()) continue;
            Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
            double dmg = estimatedDamage(targetPos, center);
            if (dmg < minDamage.get()) continue;
            if (autoSafeAnchor.enabled()) {
                double selfDmg = estimatedDamage(Vec3d.ofCenter(client.player.getBlockPos()), center);
                if (selfDmg > safeAnchorMaxSelf.get()) continue;
            }
            list.add(pos.toImmutable());
        }
        return list;
    }

    private List<BlockPos> findUnchargedAnchorsNearTargetList(MinecraftClient client, LivingEntity target) {
        List<BlockPos> list = new ArrayList<>();
        int radius = (int) Math.ceil(actionRange.get());
        for (BlockPos pos : BlockPos.iterateOutwards(target.getBlockPos(), radius, 3, radius)) {
            BlockState state = client.world.getBlockState(pos);
            if (!state.isOf(Blocks.RESPAWN_ANCHOR)) continue;
            int charges = state.get(RespawnAnchorBlock.CHARGES);
            if (charges >= 4) continue;
            Vec3d center = Vec3d.ofCenter(pos);
            if (client.player.squaredDistanceTo(center) > actionRange.get() * actionRange.get()) continue;
            Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
            double dmg = estimatedDamage(targetPos, center);
            if (dmg < minDamage.get()) continue;
            list.add(pos.toImmutable());
        }
        return list;
    }

    private void detonateAnchorsList(MinecraftClient client, List<BlockPos> anchors) {
        for (BlockPos pos : anchors) {
            Vec3d center = Vec3d.ofCenter(pos);
            if (rotate.enabled()) {
                CombatUtil.face(rotations(), client.player, center, rotationSpeed.get());
            }
            if (actionReady(1, actionJitter.get())) {
                int heldSlot = client.player.getInventory().getSelectedSlot();
                int nonGlowstone = findNonGlowstoneSlot(client);
                if (nonGlowstone >= 0) {
                    CombatUtil.selectHotbarSlot(client.player, nonGlowstone);
                }
                CombatUtil.interactBlock(client, pos, Direction.UP);
                CombatUtil.selectHotbarSlot(client.player, heldSlot);
            }
        }
    }

    private int chargeAnchorsList(MinecraftClient client, List<BlockPos> uncharged, int needed) {
        int charged = 0;
        int glowstoneSlot = CombatUtil.findItem(client.player, Items.GLOWSTONE);
        if (glowstoneSlot == -1) return 0;

        for (BlockPos pos : uncharged) {
            if (charged >= needed) break;
            Vec3d center = Vec3d.ofCenter(pos);
            if (rotate.enabled()) {
                CombatUtil.face(rotations(), client.player, center, rotationSpeed.get());
            }
            if (actionReady(2, actionJitter.get())) {
                CombatUtil.selectHotbarSlot(client.player, glowstoneSlot);
                CombatUtil.interactBlock(client, pos, Direction.UP);
                charged++;
            }
        }
        return charged;
    }

    private void placeAnchorsCount(MinecraftClient client, LivingEntity target, int count) {
        int anchorSlot = CombatUtil.findItem(client.player, Items.RESPAWN_ANCHOR);
        if (anchorSlot == -1) return;

        int radius = (int) Math.ceil(actionRange.get());
        for (BlockPos pos : BlockPos.iterateOutwards(target.getBlockPos(), radius, 2, radius)) {
            if (count <= 0) break;
            if (!client.world.getBlockState(pos).isAir()) continue;
            if (client.world.getBlockState(pos.down()).isAir()) continue;
            Vec3d center = Vec3d.ofCenter(pos);
            if (client.player.squaredDistanceTo(center) > actionRange.get() * actionRange.get()) continue;
            Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
            if (estimatedDamage(targetPos, center) < minDamage.get()) continue;
            if (autoSafeAnchor.enabled()) {
                double selfDmg = estimatedDamage(Vec3d.ofCenter(client.player.getBlockPos()), center);
                if (selfDmg > safeAnchorMaxSelf.get()) continue;
            }
            if (rotate.enabled()) {
                CombatUtil.face(rotations(), client.player, center, rotationSpeed.get());
            }
            if (actionReady(2, actionJitter.get())) {
                CombatUtil.selectHotbarSlot(client.player, anchorSlot);
                CombatUtil.interactBlock(client, pos, Direction.UP);
                count--;
            }
        }
    }

    // Existing methods kept for backward compatibility
    private void chargeAnchor(MinecraftClient client, BlockPos pos) {
        if (rotate.enabled()) {
            CombatUtil.face(rotations(), client.player, Vec3d.ofCenter(pos), rotationSpeed.get());
        }
        int slot = CombatUtil.findItem(client.player, Items.GLOWSTONE);
        if (slot == -1) return;
        if (actionReady(2, actionJitter.get())) {
            CombatUtil.selectHotbarSlot(client.player, slot);
            CombatUtil.interactBlock(client, pos, Direction.UP);
        }
    }

    private void placeAnchor(MinecraftClient client, LivingEntity target) {
        int slot = CombatUtil.findItem(client.player, Items.RESPAWN_ANCHOR);
        if (slot == -1) return;

        Optional<BlockPos> placePos = findBestAnchorPlacement(client, target);
        if (placePos.isEmpty()) return;

        BlockPos pos = placePos.get();
        if (rotate.enabled()) {
            CombatUtil.face(rotations(), client.player, Vec3d.ofCenter(pos), rotationSpeed.get());
        }

        if (actionReady(3, actionJitter.get())) {
            CombatUtil.selectHotbarSlot(client.player, slot);
            CombatUtil.interactBlock(client, pos.down(), Direction.UP);
        }
    }

    private Optional<BlockPos> findExistingUnchargedAnchor(MinecraftClient client, LivingEntity target) {
        int radius = (int) Math.ceil(actionRange.get());
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.iterateOutwards(target.getBlockPos(), radius, 3, radius)) {
            Vec3d center = Vec3d.ofCenter(pos);
            BlockState state = client.world.getBlockState(pos);
            if (!state.isOf(Blocks.RESPAWN_ANCHOR)) continue;
            int charges = state.get(RespawnAnchorBlock.CHARGES);
            if (charges >= 4) continue;
            if (client.player.squaredDistanceTo(center) > actionRange.get() * actionRange.get()) continue;
            double dist = client.player.squaredDistanceTo(center);
            if (dist < bestDist) {
                best = pos.toImmutable();
                bestDist = dist;
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
            if (!client.world.getBlockState(pos).isAir()) continue;
            if (client.world.getBlockState(pos.down()).isAir()) continue;
            if (client.player.squaredDistanceTo(center) > actionRange.get() * actionRange.get()) continue;
            if (estimatedDamage(new Vec3d(target.getX(), target.getY(), target.getZ()), center) < minDamage.get()) continue;
            double selfDmg = estimatedDamage(new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ()), center);
            if (autoSafeAnchor.enabled() && selfDmg > safeAnchorMaxSelf.get()) continue;
            if (selfDmg > maxSelfDamage.get()) continue;

            double distance = target.squaredDistanceTo(center);
            if (distance < bestDistance) {
                best = pos.toImmutable();
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    private double estimatedDamage(Vec3d target, Vec3d anchor) {
        double distance = target.distanceTo(anchor);
        return Math.max(0.0, 10.0 - distance * 1.8);
    }

    private int findNonGlowstoneSlot(MinecraftClient client) {
        for (int slot = 0; slot < 9; slot++) {
            if (!client.player.getInventory().getStack(slot).isOf(Items.GLOWSTONE)) return slot;
        }
        return -1;
    }
}
