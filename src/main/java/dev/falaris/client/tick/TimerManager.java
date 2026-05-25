package dev.falaris.client.tick;

import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.Map;

public final class TimerManager {
    private static final TimerManager INSTANCE = new TimerManager();
    private final Map<Object, TimerRequest> requests = new HashMap<>();
    private float currentSpeed = 1.0f;

    public static TimerManager getInstance() { return INSTANCE; }

    public void requestSpeed(Object owner, float speed, int priority) {
        requests.put(owner, new TimerRequest(speed, priority));
        recompute();
    }

    public void release(Object owner) {
        requests.remove(owner);
        recompute();
    }

    private void recompute() {
        float speed = 1.0f;
        int highestPrio = Integer.MIN_VALUE;
        for (TimerRequest req : requests.values()) {
            if (req.priority > highestPrio) {
                highestPrio = req.priority;
                speed = req.speed;
            }
        }
        currentSpeed = speed;
        apply();
    }

    private void apply() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            Object timer = timerField.get(client);
            timerSpeedField.setFloat(timer, currentSpeed);
        } catch (Exception ignored) {}
    }

    public float getCurrentSpeed() { return currentSpeed; }
    public boolean hasRequests() { return !requests.isEmpty(); }

    private record TimerRequest(float speed, int priority) {}

    private static final java.lang.reflect.Field timerField;
    private static final java.lang.reflect.Field timerSpeedField;

    static {
        java.lang.reflect.Field t = null;
        java.lang.reflect.Field ts = null;
        try {
            t = MinecraftClient.class.getDeclaredField("renderTickCounter");
            t.setAccessible(true);
            Class<?> timerClass = t.getType();
            ts = timerClass.getDeclaredField("tickSpeedMultiplier");
            ts.setAccessible(true);
        } catch (Exception e) {
            try {
                t = MinecraftClient.class.getDeclaredField("field_1728");
                t.setAccessible(true);
                Class<?> timerClass = t.getType();
                ts = timerClass.getDeclaredField("field_18653");
                ts.setAccessible(true);
            } catch (Exception ignored) {}
        }
        timerField = t;
        timerSpeedField = ts;
    }
}
