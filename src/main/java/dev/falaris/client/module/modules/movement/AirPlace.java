package dev.falaris.client.module.modules.movement;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class AirPlace extends MovementModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Placement distance from eyes.", 4.5, 1.0, 6.0));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks between placements.", 4, 1, 20));
    private final IntegerSetting jitter = setting(new IntegerSetting("Jitter", "Random extra ticks between placements.", 2, 0, 10));
    private final BooleanSetting requireUse = setting(new BooleanSetting("Require Use", "Only place while use key is held.", true));
    private final BooleanSetting rotate = setting(new BooleanSetting("Rotate", "Face the placement position.", true));

    public AirPlace() {
        super("AirPlace", "Places blocks at a projected position in front of the player.");
    }

    @Override
    protected void onMovementTick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) {
            return;
        }
        if (requireUse.enabled() && !client.options.useKey.isPressed()) {
            return;
        }
        if (!(client.player.getMainHandStack().getItem() instanceof BlockItem)) {
            return;
        }

        Vec3d eye = client.player.getEyePos();
        Vec3d target = eye.add(client.player.getRotationVector().multiply(range.get()));
        BlockPos placePos = BlockPos.ofFloored(target);
        if (!client.world.getBlockState(placePos).isAir() || !ready(delay.get(), jitter.get())) {
            return;
        }

        if (rotate.enabled()) {
            float yaw = MovementUtil.yawTo(client.player.getEyePos(), Vec3d.ofCenter(placePos));
            rotations().rotateTo(yaw, client.player.getPitch(), 2);
        }

        BlockHitResult result = new BlockHitResult(Vec3d.ofCenter(placePos), Direction.UP, placePos.down(), false);
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, result);
        client.player.swingHand(Hand.MAIN_HAND);
    }
}
