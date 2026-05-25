package dev.falaris.client.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public final class PositionExtrapolation {
    private static final Map<Integer, List<Snapshot>> history = new HashMap<>();
    private static final int TRAJECTORY_TICKS = 5;

    public static void record(Entity entity) {
        long now = System.currentTimeMillis();
        history.computeIfAbsent(entity.getId(), k -> new ArrayList<>())
               .add(new Snapshot(now, new Vec3d(entity.getX(), entity.getY(), entity.getZ()), entity.getVelocity()));
        long cutoff = now - 2000;
        history.values().forEach(l -> l.removeIf(s -> s.time < cutoff));
    }

    public static Vec3d predict(Entity entity, int ticksAhead) {
        Vec3d pos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        Vec3d vel = entity.getVelocity();
        for (int i = 0; i < ticksAhead; i++) {
            pos = pos.add(vel.x, vel.y, vel.z);
            vel = vel.add(0, -0.08, 0).multiply(0.98);
        }
        return pos;
    }

    public static boolean willLeaveRange(Entity self, Entity target, double range, int ticksAhead) {
        Vec3d selfFuture = predict(self, ticksAhead);
        Vec3d targetFuture = predict(target, ticksAhead);
        return selfFuture.squaredDistanceTo(targetFuture) > range * range;
    }

    public static void clear() {
        history.clear();
    }

    private record Snapshot(long time, Vec3d pos, Vec3d vel) {}
}
