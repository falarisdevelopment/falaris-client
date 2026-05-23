package dev.falaris.client.module.modules.movement;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class PacketMine extends MovementModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Mining packet flow.", "Normal", "Normal", "Instant", "Abort"));
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum mining distance.", 5.0, 1.0, 6.0));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks between mining packets.", 3, 1, 30));
    private final IntegerSetting jitter = setting(new IntegerSetting("Jitter", "Random extra ticks between mining packets.", 2, 0, 12));
    private final BooleanSetting requireAttack = setting(new BooleanSetting("Require Attack", "Only mine while attack key is held.", true));
    private final BooleanSetting rotate = setting(new BooleanSetting("Rotate", "Face the mined block.", true));

    public PacketMine() {
        super("PacketMine", "Sends controlled block-digging packets for testing block interaction behavior.");
    }

    @Override
    protected void onMovementTick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.crosshairTarget == null || client.player.networkHandler == null) {
            return;
        }
        if (requireAttack.enabled() && !client.options.attackKey.isPressed()) {
            return;
        }
        if (client.crosshairTarget.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult hit = (BlockHitResult) client.crosshairTarget;
        BlockPos pos = hit.getBlockPos();
        Direction side = hit.getSide();
        if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) > range.get() * range.get() || client.world.getBlockState(pos).isAir()) {
            return;
        }

        if (rotate.enabled()) {
            float yaw = MovementUtil.yawTo(client.player.getEyePos(), Vec3d.ofCenter(pos));
            rotations().rotateTo(yaw, client.player.getPitch(), 2);
        }

        if (!ready(delay.get(), jitter.get())) {
            return;
        }

        send(client, PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, side);
        if (mode.is("Instant")) {
            send(client, PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, side);
        } else if (mode.is("Abort")) {
            send(client, PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, side);
        }
        client.player.swingHand(Hand.MAIN_HAND);
    }

    private void send(MinecraftClient client, PlayerActionC2SPacket.Action action, BlockPos pos, Direction side) {
        client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(action, pos, side));
    }
}
