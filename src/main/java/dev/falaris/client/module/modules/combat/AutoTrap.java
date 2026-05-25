package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class AutoTrap extends CombatModule {
    private final ModeSetting blockType = setting(new ModeSetting("Block Type", "Block to place.", "Obsidian", "Obsidian", "Crying Obsidian", "Netherite Block", "End Stone"));
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Target range.", 5.0, 2.0, 8.0));
    private final BooleanSetting above = setting(new BooleanSetting("Above", "Place block above target.", true));
    private final BooleanSetting surround = setting(new BooleanSetting("Surround", "Place blocks around target.", true));
    private final BooleanSetting ceiling = setting(new BooleanSetting("Ceiling", "Place block at target head level.", false));
    private final BooleanSetting toggleAfterTrap = setting(new BooleanSetting("Toggle After Trap", "Disable after full trap.", false));
    private final IntegerSetting blocksPerTick = setting(new IntegerSetting("Blocks Per Tick", "Max blocks per tick.", 2, 1, 6));

    private int tickCounter;
    private LivingEntity target;

    public AutoTrap() {
        super("AutoTrap", "Traps enemies by placing blocks around them.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        tickCounter++;
        if (tickCounter < 2) return;
        tickCounter = 0;

        target = CombatUtil.bestLivingTarget(client, range.get(), true, false, false, false, "Distance").orElse(null);
        if (target == null) return;

        if (!hasBlock(client)) return;
        BlockPos tPos = target.getBlockPos();
        int placed = 0;

        if (above.enabled() && placed < blocksPerTick.get()) {
            BlockPos up = tPos.up(2);
            if (canPlace(client, up) && tryPlace(client, up)) placed++;
        }
        if (ceiling.enabled() && placed < blocksPerTick.get()) {
            BlockPos ceil = tPos.up(1);
            if (canPlace(client, ceil) && tryPlace(client, ceil)) placed++;
        }
        if (surround.enabled()) {
            BlockPos[] around = {
                tPos.north(), tPos.south(), tPos.east(), tPos.west(),
                tPos.north().east(), tPos.north().west(), tPos.south().east(), tPos.south().west()
            };
            for (BlockPos p : around) {
                if (placed >= blocksPerTick.get()) break;
                if (canPlace(client, p) && tryPlace(client, p)) placed++;
            }
        }

        if (toggleAfterTrap.enabled() && placed == 0) setEnabled(false);
    }

    private boolean canPlace(MinecraftClient client, BlockPos pos) {
        return client.world.getBlockState(pos).isAir() &&
               client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) <= 36 &&
               client.world.getBlockState(pos.down()).isSolid();
    }

    private boolean tryPlace(MinecraftClient client, BlockPos pos) {
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
        for (int slot = 0; slot < 9; slot++) {
            var stack = client.player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;
            boolean match = switch (blockType.get()) {
                case "Obsidian" -> stack.isOf(net.minecraft.item.Items.OBSIDIAN);
                case "Crying Obsidian" -> stack.isOf(net.minecraft.item.Items.CRYING_OBSIDIAN);
                case "Netherite Block" -> stack.isOf(net.minecraft.item.Items.NETHERITE_BLOCK);
                case "End Stone" -> stack.isOf(net.minecraft.item.Items.END_STONE);
                default -> false;
            };
            if (match) return slot;
        }
        return -1;
    }

    private boolean hasBlock(MinecraftClient client) {
        return findBlock(client) >= 0;
    }

    @Override protected void onEnable() { super.onEnable(); target = null; tickCounter = 0; }
}
