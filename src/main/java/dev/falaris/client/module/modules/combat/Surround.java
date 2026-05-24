package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;

public final class Surround extends CombatModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Surround placement mode.", "Full", "Full", "Single", "AntiFacePlace"));
    private final ModeSetting blockType = setting(new ModeSetting("Block Type", "Block to place.", "Obsidian", "Obsidian", "Crying Obsidian", "Netherite Block", "End Stone", "Any BlockItem"));
    private final BooleanSetting floor = setting(new BooleanSetting("Floor", "Also place a block below you.", true));
    private final BooleanSetting center = setting(new BooleanSetting("Center", "Center player on block before placing.", true));
    private final BooleanSetting toggleAfterPlace = setting(new BooleanSetting("Toggle After Place", "Disable after blocks placed.", false));
    private final BooleanSetting onlyMissing = setting(new BooleanSetting("Only Missing", "Only place where blocks are missing.", true));
    private final IntegerSetting placeDelay = setting(new IntegerSetting("Place Delay", "Ticks between block placements.", 1, 0, 10));
    private final IntegerSetting blocksPerTick = setting(new IntegerSetting("Blocks Per Tick", "Max blocks to place per tick.", 2, 1, 8));

    private int tickCounter;
    private boolean centered;

    public Surround() {
        super("Surround", "Places blocks around your feet to protect from crystal explosions etc.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        centered = false;
        tickCounter = 0;
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        if (center.enabled() && !centered) {
            centerPlayer(client);
            centered = true;
        }

        tickCounter++;
        if (tickCounter < placeDelay.get() + 1) return;
        tickCounter = 0;

        int placed = 0;
        for (BlockPos pos : getTargetPositions(client)) {
            if (placed >= blocksPerTick.get()) break;
            if (tryPlace(client, pos)) {
                placed++;
            }
        }

        if (toggleAfterPlace.enabled() && placed > 0 && isComplete(client)) {
            setEnabled(false);
        }
    }

    private Set<BlockPos> getTargetPositions(MinecraftClient client) {
        BlockPos feet = client.player.getBlockPos();
        Set<BlockPos> targets = new HashSet<>();

        switch (mode.get()) {
            case "Full" -> {
                targets.add(feet.north());
                targets.add(feet.south());
                targets.add(feet.east());
                targets.add(feet.west());
                targets.add(feet.north().east());
                targets.add(feet.north().west());
                targets.add(feet.south().east());
                targets.add(feet.south().west());
            }
            case "Single" -> {
                for (Direction dir : Direction.Type.HORIZONTAL) {
                    BlockPos offset = feet.offset(dir);
                    if (client.world.getBlockState(offset).isAir()) {
                        targets.add(offset);
                        break;
                    }
                }
            }
            case "AntiFacePlace" -> {
                targets.add(feet.north());
                targets.add(feet.south());
                targets.add(feet.east());
                targets.add(feet.west());
            }
        }

        if (floor.enabled()) {
            targets.add(feet.down());
        }
        return targets;
    }

    private boolean tryPlace(MinecraftClient client, BlockPos pos) {
        if (!client.world.getBlockState(pos).isAir() && !client.world.getBlockState(pos).isReplaceable()) return false;
        if (onlyMissing.enabled()) {
            if (!client.world.getBlockState(pos).isAir()) return false;
        }

        int slot = findBlock(client);
        if (slot < 0) return false;
        client.player.getInventory().setSelectedSlot(slot);

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (!client.world.getBlockState(neighbor).isAir()) {
                Vec3d hit = Vec3d.ofCenter(neighbor).add(Vec3d.of(dir.getOpposite().getVector()).multiply(0.5));
                BlockHitResult result = new BlockHitResult(hit, dir.getOpposite(), neighbor, false);
                client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, result);
                client.player.swingHand(Hand.MAIN_HAND);
                return true;
            }
        }
        return false;
    }

    private int findBlock(MinecraftClient client) {
        String bt = blockType.get();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (bt.equals("Obsidian") && stack.isOf(Items.OBSIDIAN)) return slot;
            if (bt.equals("Crying Obsidian") && stack.isOf(Items.CRYING_OBSIDIAN)) return slot;
            if (bt.equals("Netherite Block") && stack.isOf(Items.NETHERITE_BLOCK)) return slot;
            if (bt.equals("End Stone") && stack.isOf(Items.END_STONE)) return slot;
        }
        if (bt.equals("Any BlockItem")) {
            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = client.player.getInventory().getStack(slot);
                if (stack.getItem() instanceof net.minecraft.item.BlockItem) return slot;
            }
        }
        // Fallback: find obsidian or any block
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (stack.isOf(Items.OBSIDIAN)) return slot;
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (stack.getItem() instanceof net.minecraft.item.BlockItem) return slot;
        }
        return -1;
    }

    private boolean isComplete(MinecraftClient client) {
        for (BlockPos pos : getTargetPositions(client)) {
            if (client.world.getBlockState(pos).isAir()) return false;
        }
        return true;
    }

    private void centerPlayer(MinecraftClient client) {
        BlockPos feet = client.player.getBlockPos();
        double x = feet.getX() + 0.5;
        double z = feet.getZ() + 0.5;
        client.player.setPosition(x, client.player.getY(), z);
    }
}
