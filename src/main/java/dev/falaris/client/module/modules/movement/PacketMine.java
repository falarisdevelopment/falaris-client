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
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Mining packet flow.", "Normal", "Normal", "Instant"));
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum mining distance.", 5.0, 1.0, 6.0));
    private final BooleanSetting rotate = setting(new BooleanSetting("Rotate", "Face the mined block.", true));

    private BlockPos miningPos;
    private Direction miningSide;
    private float progress;

    public PacketMine() {
        super("PacketMine", "Mines blocks by sending dig packets without animation.");
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        miningPos = null;
        miningSide = null;
        progress = 0;
    }

    @Override
    protected void onMovementTick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.player.networkHandler == null) {
            return;
        }

        if (miningPos != null) {
            if (client.player.squaredDistanceTo(Vec3d.ofCenter(miningPos)) > range.get() * range.get() || client.world.getBlockState(miningPos).isAir()) {
                miningPos = null;
                miningSide = null;
                progress = 0;
            } else {
                if (rotate.enabled()) {
                    float yaw = MovementUtil.yawTo(client.player.getEyePos(), Vec3d.ofCenter(miningPos));
                    rotations().rotateTo(yaw, client.player.getPitch(), 2);
                }

                progress += client.world.getBlockState(miningPos).calcBlockBreakingDelta(client.player, client.world, miningPos);

                if (progress >= 1.0f) {
                    send(client, PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, miningPos, miningSide);
                    client.player.swingHand(Hand.MAIN_HAND);
                    miningPos = null;
                    miningSide = null;
                    progress = 0;
                }
                return;
            }
        }

        if (client.options.attackKey.isPressed() && client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockHitResult hit = (BlockHitResult) client.crosshairTarget;
            BlockPos pos = hit.getBlockPos();
            Direction side = hit.getSide();

            if (client.player.squaredDistanceTo(Vec3d.ofCenter(pos)) <= range.get() * range.get() && !client.world.getBlockState(pos).isAir()) {
                miningPos = pos;
                miningSide = side;
                progress = 0;

                send(client, PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, miningPos, miningSide);
                client.player.swingHand(Hand.MAIN_HAND);

                if (mode.is("Instant")) {
                    send(client, PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, miningPos, miningSide);
                    miningPos = null;
                }
            }
        }
    }

    private void send(MinecraftClient client, PlayerActionC2SPacket.Action action, BlockPos pos, Direction side) {
        client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(action, pos, side));
    }
}
