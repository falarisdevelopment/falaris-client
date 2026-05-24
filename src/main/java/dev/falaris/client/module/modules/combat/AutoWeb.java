package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class AutoWeb extends CombatModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Web placement mode.", "Single", "Single", "Surround", "Trap"));
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Max distance to target.", 4.5, 1.0, 6.0));
    private final DoubleSetting targetRange = setting(new DoubleSetting("Target Range", "Range to find targets.", 6.0, 1.0, 12.0));
    private final IntegerSetting placeDelay = setting(new IntegerSetting("Place Delay", "Ticks between placements.", 2, 0, 10));
    private final BooleanSetting autoSwitch = setting(new BooleanSetting("Auto Switch", "Auto-switch to webs in hotbar.", true));
    private final BooleanSetting rotate = setting(new BooleanSetting("Rotate", "Face block being placed.", true));
    private final BooleanSetting toggleAfterPlace = setting(new BooleanSetting("Toggle After Place", "Disable after placing.", false));
    private final ModeSetting targetMode = setting(new ModeSetting("Target Mode", "Who to web.", "Nearest", "Nearest", "Crosshair", "Combat Target"));

    private int tickCounter;
    private final Random random = new Random();

    public AutoWeb() {
        super("AutoWeb", "Automatically places webs on your opponent to trap them.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        tickCounter++;
        if (tickCounter < placeDelay.get() + random.nextInt(2)) return;
        tickCounter = 0;

        LivingEntity target = findTarget(client);
        if (target == null) return;

        Set<BlockPos> positions = getWebPositions(client, target);
        if (positions.isEmpty()) return;

        int webSlot = findWebs(client);
        if (webSlot < 0) {
            if (!autoSwitch.enabled()) return;
            // Try to find any slot with webs
            webSlot = findWebs(client);
            if (webSlot < 0) return;
        }

        if (autoSwitch.enabled()) {
            client.player.getInventory().setSelectedSlot(webSlot);
        }

        for (BlockPos pos : positions) {
            if (!client.world.getBlockState(pos).isAir()) continue;
            if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > range.get() * range.get()) continue;

            if (rotate.enabled()) {
                float[] rots = CombatUtil.rotationsTo(client.player, Vec3d.ofCenter(pos));
                rotations().setMaxStep(25.0f);
                rotations().rotateTo(rots[0], rots[1], 2);
            }

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = pos.offset(dir);
                if (!client.world.getBlockState(neighbor).isAir()) {
                    Vec3d hit = Vec3d.ofCenter(neighbor).add(Vec3d.of(dir.getOpposite().getVector()).multiply(0.5));
                    BlockHitResult result = new BlockHitResult(hit, dir.getOpposite(), neighbor, false);
                    client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, result);
                    client.player.swingHand(Hand.MAIN_HAND);
                    break;
                }
            }

            if (toggleAfterPlace.enabled()) {
                setEnabled(false);
                return;
            }
            break;
        }
    }

    private LivingEntity findTarget(MinecraftClient client) {
        if (targetMode.is("Crosshair")) {
            if (client.crosshairTarget instanceof EntityHitResult hit && hit.getType() == HitResult.Type.ENTITY) {
                if (hit.getEntity() instanceof LivingEntity le && le != client.player) return le;
            }
            return null;
        }
        if (targetMode.is("Combat Target")) {
            // Check if KillAura or similar has a target
            var opt = CombatUtil.bestLivingTarget(client, targetRange.get(), true, true, false, false, "Distance");
            return opt.orElse(null);
        }
        return CombatUtil.bestLivingTarget(client, targetRange.get(), true, true, false, false, "Distance").orElse(null);
    }

    private Set<BlockPos> getWebPositions(MinecraftClient client, LivingEntity target) {
        BlockPos feet = target.getBlockPos();
        Set<BlockPos> result = new HashSet<>();

        switch (mode.get()) {
            case "Single" -> {
                result.add(feet);
            }
            case "Surround" -> {
                result.add(feet.north());
                result.add(feet.south());
                result.add(feet.east());
                result.add(feet.west());
            }
            case "Trap" -> {
                result.add(feet);
                result.add(feet.up());
                result.add(feet.north());
                result.add(feet.south());
                result.add(feet.east());
                result.add(feet.west());
            }
        }
        return result;
    }

    private int findWebs(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isOf(Items.COBWEB)) return i;
        }
        return -1;
    }
}
