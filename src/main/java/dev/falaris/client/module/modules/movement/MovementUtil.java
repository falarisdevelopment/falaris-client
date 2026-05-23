package dev.falaris.client.module.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

final class MovementUtil {
    private MovementUtil() {
    }

    static Vec3d inputVelocity(ClientPlayerEntity player, double speed, boolean includeY, MinecraftClient client) {
        double forward = client.options.forwardKey.isPressed() ? 1.0 : 0.0;
        forward -= client.options.backKey.isPressed() ? 1.0 : 0.0;
        double strafe = client.options.leftKey.isPressed() ? 1.0 : 0.0;
        strafe -= client.options.rightKey.isPressed() ? 1.0 : 0.0;

        double y = 0.0;
        if (includeY) {
            y += client.options.jumpKey.isPressed() ? speed : 0.0;
            y -= client.options.sneakKey.isPressed() ? speed : 0.0;
        }

        if (forward == 0.0 && strafe == 0.0) {
            return new Vec3d(0.0, y, 0.0);
        }

        double length = Math.sqrt(forward * forward + strafe * strafe);
        forward /= length;
        strafe /= length;

        double yaw = Math.toRadians(player.getYaw());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double x = (strafe * cos - forward * sin) * speed;
        double z = (forward * cos + strafe * sin) * speed;
        return new Vec3d(x, y, z);
    }

    static void setHorizontalSpeed(Entity entity, double speed, MinecraftClient client) {
        if (!(entity instanceof ClientPlayerEntity player)) {
            return;
        }
        Vec3d input = inputVelocity(player, speed, false, client);
        entity.setVelocity(input.x, entity.getVelocity().y, input.z);
    }

    static int findItem(ClientPlayerEntity player, Item item) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(item)) {
                return slot;
            }
        }
        return -1;
    }

    static boolean selectSlot(ClientPlayerEntity player, int slot) {
        if (slot < 0 || slot > 8) {
            return false;
        }
        player.getInventory().setSelectedSlot(slot);
        return true;
    }

    static boolean interactBlock(MinecraftClient client, BlockPos pos, Direction side, Hand hand) {
        if (client.player == null || client.interactionManager == null) {
            return false;
        }

        Vec3d hit = Vec3d.ofCenter(pos).add(Vec3d.of(side.getVector()).multiply(0.5));
        BlockHitResult result = new BlockHitResult(hit, side, pos, false);
        client.interactionManager.interactBlock(client.player, hand, result);
        client.player.swingHand(hand);
        return true;
    }

    static float yawTo(Vec3d from, Vec3d to) {
        double diffX = to.x - from.x;
        double diffZ = to.z - from.z;
        return (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
    }
}
