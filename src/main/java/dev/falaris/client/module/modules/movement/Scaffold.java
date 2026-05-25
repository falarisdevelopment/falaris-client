package dev.falaris.client.module.modules.movement;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public final class Scaffold extends MovementModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Placement reach.", 4.5, 1.0, 6.0));
    private final BooleanSetting swing = setting(new BooleanSetting("Swing", "Swing hand when placing.", true));
    private final BooleanSetting silentRotate = setting(new BooleanSetting("Silent Rotate", "Rotate without moving head.", true));
    private final BooleanSetting tower = setting(new BooleanSetting("Tower", "Hold sneak to tower up.", true));
    private final ModeSetting towerMode = setting(new ModeSetting("Tower Mode", "Tower method.", "Grim Jump", "Jump", "Rubberband", "Grim Jump", "Fast", "None"));
    private final DoubleSetting towerSpeed = setting(new DoubleSetting("Tower Speed", "Blocks per second when towering.", 1.2, 0.5, 3.0));
    private final ModeSetting blockSelect = setting(new ModeSetting("Block Select", "Block selection mode.", "Any", "Any", "First", "Selected", "Obsidian", "Cobblestone", "End Stone", "Wood"));
    private final IntegerSetting placeDelay = setting(new IntegerSetting("Place Delay", "Ticks between placements.", 1, 0, 5));
    private final BooleanSetting placeOnEdge = setting(new BooleanSetting("Place on Edge", "Place blocks even when standing on edge.", true));
    private final ModeSetting placeMode = setting(new ModeSetting("Place Mode", "Where to place blocks.", "Below", "Below", "Side", "Both", "AirPlace"));
    private final DoubleSetting rotationSpeed = setting(new DoubleSetting("Rotation Speed", "Max rotation per tick.", 24.0, 5.0, 60.0));

    private final Random random = new Random();
    private int tickCounter;
    private boolean switchedThisTick;
    private int rotationWait;

    public Scaffold() {
        super("Scaffold", "Places blocks beneath you automatically.");
    }

    @Override
    protected void onMovementTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        tickCounter++;

        handleTower(client);

        if (tickCounter % (placeDelay.get() + 1) != 0) return;

        BlockPos target = findTarget(client);
        if (target == null) return;

        int slot = findBlockSlot(client);
        if (slot == -1) return;

        int prev = client.player.getInventory().getSelectedSlot();
        client.player.getInventory().setSelectedSlot(slot);

        float[] rots = rotationsToBlock(client, target);
        if (silentRotate.enabled()) {
            float ny = (random.nextFloat() - 0.5f) * 2.0f;
            float np = (random.nextFloat() - 0.5f) * 1.0f;
            rotations().setMaxStep(rotationSpeed.get().floatValue());
            rotations().rotateToSilent(rots[0] + ny, rots[1] + np, Math.max(2, (int) Math.ceil(180.0f / rotationSpeed.get().floatValue())));
            rotations().setServerRotation(rots[0], rots[1], Math.max(1, (int) Math.ceil(180.0f / rotationSpeed.get().floatValue())));
        }

        placeBlock(client, target);
        if (swing.enabled()) client.player.swingHand(Hand.MAIN_HAND);

        client.player.getInventory().setSelectedSlot(prev);
    }

    private void handleTower(MinecraftClient client) {
        if (!tower.enabled() || !client.options.sneakKey.isPressed()) return;
        if (!client.player.isOnGround()) return;
        if (client.player.hurtTime > 0) return;

        String tm = towerMode.get();
        double spd = towerSpeed.get();

        switch (tm) {
            case "Grim Jump" -> {
                if (client.player.getVelocity().y != 0) return;
                client.player.jump();
                double vy = 0.42 * Math.min(spd, 2.0);
                client.player.setVelocity(client.player.getVelocity().x, vy, client.player.getVelocity().z);
            }
            case "Jump" -> client.player.jump();
            case "Rubberband" -> {
                client.player.jump();
                client.player.setVelocity(client.player.getVelocity().x, 0.42 * spd, client.player.getVelocity().z);
            }
            case "Fast" -> {
                client.player.jump();
                double vy = 0.42 * Math.min(spd, 2.0);
                client.player.setVelocity(client.player.getVelocity().x, vy, client.player.getVelocity().z);
            }
            default -> {}
        }
    }

    private BlockPos findTarget(MinecraftClient client) {
        Vec3d pos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
        String pm = placeMode.get();

        if (pm.equals("Side") || pm.equals("Both") || pm.equals("AirPlace")) {
            BlockPos sideTarget = findSideTarget(client);
            if (sideTarget != null) return sideTarget;
            if (pm.equals("Side")) return null;
        }

        BlockPos below = BlockPos.ofFloored(pos.x, pos.y - 0.5, pos.z);
        if (!client.world.getBlockState(below).isAir() && !client.world.getBlockState(below).isReplaceable()) return null;
        if (pm.equals("AirPlace")) return below;

        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP) continue;
            if (isValidNeighbor(client, below.offset(dir))) return below;
        }

        if (!placeOnEdge.enabled()) return null;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos check = below.add(dx, 0, dz);
                if (!client.world.getBlockState(check).isAir() && !client.world.getBlockState(check).isReplaceable()) continue;
                for (Direction dir : Direction.values()) {
                    if (dir == Direction.UP) continue;
                    if (isValidNeighbor(client, check.offset(dir))) return check;
                }
            }
        }
        return null;
    }

    private BlockPos findSideTarget(MinecraftClient client) {
        Vec3d pos = client.player.getEyePos();
        Vec3d look = client.player.getRotationVec(1.0f);
        for (double d = 1.0; d <= range.get(); d += 1.0) {
            Vec3d check = pos.add(look.x * d, 0, look.z * d);
            BlockPos bp = BlockPos.ofFloored(check);
            if (client.world.getBlockState(bp).isAir() || client.world.getBlockState(bp).isReplaceable()) {
                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = bp.offset(dir);
                    if (isValidNeighbor(client, neighbor)) return bp;
                }
            }
        }
        return null;
    }

    private boolean isValidNeighbor(MinecraftClient client, BlockPos pos) {
        return client.world.getBlockState(pos).isSolidBlock(client.world, pos);
    }

    private int findBlockSlot(MinecraftClient client) {
        for (int slot = 0; slot < 9; slot++) {
            var stack = client.player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof BlockItem bi)) continue;
            Block b = bi.getBlock();
            BlockState state = b.getDefaultState();
            if (!state.isSolid()) continue;
            if (blockSelect.is("Selected") && !isPreferredBlock(b)) continue;
            if (b == Blocks.AIR) continue;
            return slot;
        }
        return -1;
    }

    private boolean isPreferredBlock(Block b) {
        if (blockSelect.is("Obsidian")) return b == Blocks.OBSIDIAN;
        if (blockSelect.is("Cobblestone")) return b == Blocks.COBBLESTONE;
        if (blockSelect.is("End Stone")) return b == Blocks.END_STONE;
        if (blockSelect.is("Wood")) return b == Blocks.OAK_PLANKS || b == Blocks.SPRUCE_PLANKS || b == Blocks.BIRCH_PLANKS;
        return b == Blocks.OBSIDIAN || b == Blocks.COBBLESTONE || b == Blocks.STONE
                || b == Blocks.END_STONE || b == Blocks.NETHERRACK || b == Blocks.DIRT;
    }

    private boolean placeBlock(MinecraftClient client, BlockPos pos) {
        if (client.interactionManager == null) return false;
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP) continue;
            BlockPos neighbor = pos.offset(dir);
            if (!client.world.getBlockState(neighbor).isSolidBlock(client.world, neighbor)) continue;
            Vec3d hit = Vec3d.ofCenter(neighbor);
            BlockHitResult hitResult = new BlockHitResult(hit, dir.getOpposite(), neighbor, false);
            ActionResult result = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
            if (result.isAccepted()) return true;
        }
        return false;
    }

    private float[] rotationsToBlock(MinecraftClient client, BlockPos pos) {
        Vec3d target = Vec3d.ofCenter(pos);
        Vec3d eyes = client.player.getEyePos();
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        return new float[]{
            (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f,
            (float) -Math.toDegrees(Math.atan2(dy, dist))
        };
    }
}
