package dev.falaris.client.backtrack;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.stream.Collectors;

public final class BacktrackManager {
    private static final BacktrackManager INSTANCE = new BacktrackManager();
    private static final int HISTORY_MS = 2000;
    private final Map<Integer, List<TimedPosition>> history = new HashMap<>();
    private long lastCapture;

    public static BacktrackManager getInstance() { return INSTANCE; }

    public void capture(ClientWorld world) {
        long now = System.currentTimeMillis();
        lastCapture = now;
        for (Entity entity : world.getEntities()) {
            Vec3d pos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
            history.computeIfAbsent(entity.getId(), k -> new ArrayList<>())
                   .add(new TimedPosition(now, pos, entity.getYaw(), entity.getPitch(), entity.getBoundingBox()));
        }
        long cutoff = now - HISTORY_MS;
        history.values().forEach(list -> list.removeIf(tp -> tp.time < cutoff));
        history.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    public TimedPosition getBacktracked(int entityId, int delayMs) {
        List<TimedPosition> positions = history.get(entityId);
        if (positions == null || positions.isEmpty()) return null;
        long targetTime = System.currentTimeMillis() - delayMs;
        return positions.stream()
            .min(Comparator.comparingLong(tp -> Math.abs(tp.time - targetTime)))
            .orElse(null);
    }

    public Map<Integer, List<TimedPosition>> getAllHistory() { return history; }

    public static final class TimedPosition {
        public final long time;
        public final Vec3d pos;
        public final float yaw;
        public final float pitch;
        public final Box box;

        public TimedPosition(long time, Vec3d pos, float yaw, float pitch, Box box) {
            this.time = time;
            this.pos = pos;
            this.yaw = yaw;
            this.pitch = pitch;
            this.box = box;
        }
    }
}
