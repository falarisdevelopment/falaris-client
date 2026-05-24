package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
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
import java.util.Optional;

public final class DoubleAnchor extends CombatModule {
    private final DoubleSetting targetRange = setting(new DoubleSetting("Target Range", "Max target distance.", 6.0, 1.0, 10.0));
    private final DoubleSetting actionRange = setting(new DoubleSetting("Action Range", "Max anchor interaction distance.", 3.0, 1.0, 4.0));
    private final DoubleSetting minDamage = setting(new DoubleSetting("Min Damage", "Minimum per anchor damage.", 5.0, 1.0, 20.0));
    private final DoubleSetting maxSelfDamage = setting(new DoubleSetting("Max Self Damage", "Max total self damage.", 10.0, 1.0, 20.0));
    private final DoubleSetting rotationSpeed = setting(new DoubleSetting("Rotation Speed", "Max rotation per tick.", 18.0, 1.0, 60.0));
    private final IntegerSetting actionJitter = setting(new IntegerSetting("Action Jitter", "Extra ticks between phases.", 2, 0, 10));
    private final BooleanSetting safeAnchor = setting(new BooleanSetting("Safe Anchor", "Skip if self damage > max.", true));
    private final BooleanSetting rotate = setting(new BooleanSetting("Rotate", "Face anchor.", true));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Target through blocks.", false));

    public DoubleAnchor() {
        super("DoubleAnchor", "Places two anchors and detonates both instantly for massive burst damage.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        LivingEntity target = CombatUtil.bestLivingTarget(client, targetRange.get(), true, true, false, throughWalls.enabled(), "Distance").orElse(null);
        if (target == null) return;

        List<BlockPos> charged = findChargedAnchorsNearTarget(client, target);
        if (charged.size() >= 2) {
            detonateAnchors(client, charged);
            return;
        }

        List<BlockPos> uncharged = findUnchargedAnchorsNearTarget(client, target);
        int needed = 2 - charged.size();
        int charged_count = chargeAnchors(client, uncharged, needed);

        if (charged.size() + charged_count >= 2) {
            List<BlockPos> allCharged = findChargedAnchorsNearTarget(client, target);
            detonateAnchors(client, allCharged);
            return;
        }

        int toPlace = 2 - (charged.size() + charged_count);
        placeAnchors(client, target, toPlace);
    }

    private List<BlockPos> findChargedAnchorsNearTarget(MinecraftClient client, LivingEntity target) {
        List<BlockPos> list = new ArrayList<>();
        int radius = (int) Math.ceil(actionRange.get());
        for (BlockPos pos : BlockPos.iterateOutwards(target.getBlockPos(), radius, 3, radius)) {
            BlockState state = client.world.getBlockState(pos);
            if (!state.isOf(Blocks.RESPAWN_ANCHOR)) continue;
            int charges = state.get(RespawnAnchorBlock.CHARGES);
            if (charges <= 0) continue;
            Vec3d center = Vec3d.ofCenter(pos);
            if (client.player.squaredDistanceTo(center) > actionRange.get() * actionRange.get()) continue;
            if (estimatedDamage(Vec3d.ofCenter(target.getBlockPos()), center) < minDamage.get()) continue;
            if (safeAnchor.enabled()) {
                double self = estimatedDamage(Vec3d.ofCenter(client.player.getBlockPos()), center);
                if (self > maxSelfDamage.get()) continue;
            }
            list.add(pos.toImmutable());
        }
        return list;
    }

    private List<BlockPos> findUnchargedAnchorsNearTarget(MinecraftClient client, LivingEntity target) {
        List<BlockPos> list = new ArrayList<>();
        int radius = (int) Math.ceil(actionRange.get());
        for (BlockPos pos : BlockPos.iterateOutwards(target.getBlockPos(), radius, 3, radius)) {
            BlockState state = client.world.getBlockState(pos);
            if (!state.isOf(Blocks.RESPAWN_ANCHOR)) continue;
            int charges = state.get(RespawnAnchorBlock.CHARGES);
            if (charges >= 4) continue;
            Vec3d center = Vec3d.ofCenter(pos);
            if (client.player.squaredDistanceTo(center) > actionRange.get() * actionRange.get()) continue;
            if (estimatedDamage(Vec3d.ofCenter(target.getBlockPos()), center) < minDamage.get()) continue;
            if (safeAnchor.enabled()) {
                double self = estimatedDamage(Vec3d.ofCenter(client.player.getBlockPos()), center);
                if (self > maxSelfDamage.get()) continue;
            }
            list.add(pos.toImmutable());
        }
        return list;
    }

    private void detonateAnchors(MinecraftClient client, List<BlockPos> anchors) {
        for (BlockPos pos : anchors) {
            Vec3d center = Vec3d.ofCenter(pos);
            if (rotate.enabled()) {
                float[] rots = CombatUtil.rotationsTo(client.player, center);
                client.player.setYaw(rots[0]);
                client.player.setPitch(rots[1]);
            }
            int heldSlot = client.player.getInventory().getSelectedSlot();
            int nonGlowstone = findNonGlowstoneSlot(client);
            if (nonGlowstone >= 0) {
                client.player.getInventory().setSelectedSlot(nonGlowstone);
            }
            if (actionReady(1, actionJitter.get())) {
                CombatUtil.interactBlock(client, pos, Direction.UP);
            }
            client.player.getInventory().setSelectedSlot(heldSlot);
        }
    }

    private int chargeAnchors(MinecraftClient client, List<BlockPos> uncharged, int needed) {
        int charged = 0;
        int glowstoneSlot = CombatUtil.findItem(client.player, Items.GLOWSTONE);
        if (glowstoneSlot == -1) return 0;

        for (BlockPos pos : uncharged) {
            if (charged >= needed) break;
            Vec3d center = Vec3d.ofCenter(pos);
            if (rotate.enabled()) {
                float[] rots = CombatUtil.rotationsTo(client.player, center);
                client.player.setYaw(rots[0]);
                client.player.setPitch(rots[1]);
            }
            if (actionReady(2, actionJitter.get())) {
                CombatUtil.selectHotbarSlot(client.player, glowstoneSlot);
                CombatUtil.interactBlock(client, pos, Direction.UP);
                charged++;
            }
        }
        return charged;
    }

    private void placeAnchors(MinecraftClient client, LivingEntity target, int count) {
        int anchorSlot = CombatUtil.findItem(client.player, Items.RESPAWN_ANCHOR);
        if (anchorSlot == -1) return;

        int radius = (int) Math.ceil(actionRange.get());
        for (BlockPos pos : BlockPos.iterateOutwards(target.getBlockPos(), radius, 2, radius)) {
            if (count <= 0) break;
            if (!client.world.getBlockState(pos).isAir()) continue;
            if (client.world.getBlockState(pos.down()).isAir()) continue;
            Vec3d center = Vec3d.ofCenter(pos);
            if (client.player.squaredDistanceTo(center) > actionRange.get() * actionRange.get()) continue;
            if (estimatedDamage(Vec3d.ofCenter(target.getBlockPos()), center) < minDamage.get()) continue;
            if (safeAnchor.enabled()) {
                double self = estimatedDamage(Vec3d.ofCenter(client.player.getBlockPos()), center);
                if (self > maxSelfDamage.get()) continue;
            }
            if (rotate.enabled()) {
                float[] rots = CombatUtil.rotationsTo(client.player, center);
                client.player.setYaw(rots[0]);
                client.player.setPitch(rots[1]);
            }
            if (actionReady(2, actionJitter.get())) {
                CombatUtil.selectHotbarSlot(client.player, anchorSlot);
                CombatUtil.interactBlock(client, pos, Direction.UP);
                count--;
            }
        }
    }

    private int findNonGlowstoneSlot(MinecraftClient client) {
        for (int slot = 0; slot < 9; slot++) {
            if (!client.player.getInventory().getStack(slot).isOf(Items.GLOWSTONE)) return slot;
        }
        return -1;
    }

    private double estimatedDamage(Vec3d target, Vec3d anchor) {
        double distance = target.distanceTo(anchor);
        return Math.max(0.0, 10.0 - distance * 1.8);
    }
}
