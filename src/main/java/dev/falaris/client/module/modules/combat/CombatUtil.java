package dev.falaris.client.module.modules.combat;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.rotation.RotationManager;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class CombatUtil {
    private CombatUtil() {
    }

    static List<LivingEntity> livingTargets(MinecraftClient client, double range, boolean players, boolean hostiles, boolean passives, boolean walls) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return List.of();
        }

        Box box = player.getBoundingBox().expand(range);
        return client.world.getEntitiesByClass(LivingEntity.class, box, entity -> isValidTarget(player, entity, range, players, hostiles, passives, walls));
    }

    static Optional<LivingEntity> bestLivingTarget(MinecraftClient client, double range, boolean players, boolean hostiles, boolean passives, boolean walls, String priority) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return Optional.empty();
        }

        Comparator<LivingEntity> comparator = switch (priority) {
            case "Health" -> Comparator.comparingDouble(LivingEntity::getHealth);
            case "Angle" -> Comparator.comparingDouble(entity -> angleTo(player, entity.getEyePos()));
            default -> Comparator.comparingDouble(player::distanceTo);
        };

        return livingTargets(client, range, players, hostiles, passives, walls).stream().min(comparator);
    }

    static List<EndCrystalEntity> crystals(MinecraftClient client, double range, boolean walls) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return List.of();
        }

        return client.world.getEntitiesByClass(EndCrystalEntity.class, player.getBoundingBox().expand(range), crystal -> {
            if (!crystal.isAlive() || player.squaredDistanceTo(crystal) > range * range) {
                return false;
            }
            return walls || player.canSee(crystal);
        });
    }

    static Optional<EndCrystalEntity> bestCrystal(MinecraftClient client, double range, boolean walls) {
        ClientPlayerEntity player = client.player;
        if (player == null) {
            return Optional.empty();
        }

        return crystals(client, range, walls).stream().min(Comparator.comparingDouble(player::distanceTo));
    }

    static boolean isValidTarget(ClientPlayerEntity player, LivingEntity entity, double range, boolean players, boolean hostiles, boolean passives, boolean walls) {
        if (entity == player || !entity.isAlive() || entity.isRemoved() || player.squaredDistanceTo(entity) > range * range) {
            return false;
        }
        if (entity instanceof PlayerEntity && FalarisClient.getInstance().getFriendsManager().isFriend(entity.getName().getString())) {
            return false;
        }
        if (!walls && !player.canSee(entity)) {
            return false;
        }
        if (entity instanceof PlayerEntity) {
            return players;
        }
        if (entity instanceof HostileEntity) {
            return hostiles;
        }
        return passives;
    }

    public static float[] rotationsTo(ClientPlayerEntity player, Vec3d target) {
        Vec3d eyes = player.getEyePos();
        double diffX = target.x - eyes.x;
        double diffY = target.y - eyes.y;
        double diffZ = target.z - eyes.z;
        double horizontal = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, horizontal));
        return new float[] { yaw, MathHelper.clamp(pitch, -90.0f, 90.0f) };
    }

    static double angleTo(ClientPlayerEntity player, Vec3d target) {
        float[] rotations = rotationsTo(player, target);
        float yawDelta = Math.abs(MathHelper.wrapDegrees(rotations[0] - player.getYaw()));
        float pitchDelta = Math.abs(rotations[1] - player.getPitch());
        return yawDelta + pitchDelta;
    }

    static void face(RotationManager rotationManager, ClientPlayerEntity player, Vec3d target, double speed) {
        float[] rotations = rotationsTo(player, target);
        rotationManager.setMaxStep((float) speed);
        rotationManager.rotateTo(rotations[0], rotations[1], Math.max(1, (int) Math.ceil(20.0 / Math.max(1.0, speed))));
    }

    static void attack(MinecraftClient client, Entity entity) {
        if (client.player == null || client.interactionManager == null) {
            return;
        }

        client.interactionManager.attackEntity(client.player, entity);
        client.player.swingHand(Hand.MAIN_HAND);
    }

    public static int findItem(ClientPlayerEntity player, Item item) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(item)) {
                return slot;
            }
        }
        return -1;
    }

    public static boolean selectHotbarSlot(ClientPlayerEntity player, int slot) {
        if (slot < 0 || slot > 8) {
            return false;
        }
        player.getInventory().setSelectedSlot(slot);
        return true;
    }

    static Optional<BlockPos> nearestBlock(MinecraftClient client, Block block, double range) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            return Optional.empty();
        }

        BlockPos origin = player.getBlockPos();
        int radius = MathHelper.ceil(range);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterateOutwards(origin, radius, radius, radius)) {
            if (!client.world.getBlockState(pos).isOf(block)) {
                continue;
            }
            double distance = player.squaredDistanceTo(Vec3d.ofCenter(pos));
            if (distance < bestDistance && distance <= range * range) {
                best = pos.toImmutable();
                bestDistance = distance;
            }
        }
        return Optional.ofNullable(best);
    }

    static boolean interactBlock(MinecraftClient client, BlockPos pos, Direction side) {
        if (client.player == null || client.interactionManager == null) {
            return false;
        }

        Vec3d hit = Vec3d.ofCenter(pos).add(Vec3d.of(side.getVector()).multiply(0.5));
        BlockHitResult result = new BlockHitResult(hit, side, pos, false);
        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, result);
        client.player.swingHand(Hand.MAIN_HAND);
        return true;
    }

}
