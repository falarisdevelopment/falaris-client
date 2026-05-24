package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class AutoFarm extends PlayerModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Reach to scan for crops.", 4.5, 1.0, 6.0));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks between actions.", 2, 0, 10));
    private final BooleanSetting autoReplant = setting(new BooleanSetting("Auto Replant", "Plant seeds after harvesting.", true));
    private final BooleanSetting onlyHoe = setting(new BooleanSetting("Only Hoe", "Only harvest when holding a hoe.", true));

    private int tickCounter;

    public AutoFarm() {
        super("AutoFarm", "Automatically harvests and replants mature crops.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        tickCounter++;
        if (tickCounter < delay.get()) return;
        tickCounter = 0;

        if (onlyHoe.enabled() && !isHoldingHoe(client)) return;

        BlockPos origin = client.player.getBlockPos();
        int r = (int) Math.ceil(range.get());

        for (BlockPos pos : BlockPos.iterateOutwards(origin, r, 2, r)) {
            if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > range.get() * range.get()) continue;
            BlockState state = client.world.getBlockState(pos);

            // Harvest mature crops
            if (state.getBlock() instanceof CropBlock crop) {
                if (crop.isMature(state)) {
                    breakBlock(client, pos);
                    return;
                }
            }

            // Handle nether wart
            if (state.isOf(Blocks.NETHER_WART) && state.get(net.minecraft.block.NetherWartBlock.AGE) >= 3) {
                breakBlock(client, pos);
                return;
            }
        }
    }

    private void breakBlock(MinecraftClient client, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (!client.world.getBlockState(neighbor).isAir()) {
                client.interactionManager.attackBlock(pos, dir);
                client.player.swingHand(Hand.MAIN_HAND);

                // Auto replant
                if (autoReplant.enabled()) {
                    int seedSlot = findSeeds(client);
                    if (seedSlot >= 0) {
                        client.player.getInventory().setSelectedSlot(seedSlot);
                        Vec3d hit = Vec3d.ofCenter(neighbor).add(Vec3d.of(dir.getOpposite().getVector()).multiply(0.5));
                        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND,
                            new BlockHitResult(hit, dir.getOpposite(), neighbor, false));
                    }
                }
                return;
            }
        }
    }

    private int findSeeds(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isOf(Items.WHEAT_SEEDS) || stack.isOf(Items.BEETROOT_SEEDS)
                || stack.isOf(Items.POTATO) || stack.isOf(Items.CARROT)
                || stack.isOf(Items.NETHER_WART)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isHoldingHoe(MinecraftClient client) {
        ItemStack held = client.player.getMainHandStack();
        return held.isOf(Items.WOODEN_HOE) || held.isOf(Items.STONE_HOE)
            || held.isOf(Items.IRON_HOE) || held.isOf(Items.GOLDEN_HOE)
            || held.isOf(Items.DIAMOND_HOE) || held.isOf(Items.NETHERITE_HOE);
    }
}
