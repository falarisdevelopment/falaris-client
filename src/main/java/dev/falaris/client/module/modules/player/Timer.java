package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;

public final class Timer extends PlayerModule {
    private final DoubleSetting speed = setting(new DoubleSetting("Speed", "Game speed multiplier.", 1.5, 0.1, 10.0));
    private final IntegerSetting fadeTicks = setting(new IntegerSetting("Fade Ticks", "Ticks to fade when toggling off.", 5, 1, 20));
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Timer mode.", "Normal", "Normal", "Balance"));

    private float originalSpeed = 1.0f;
    private int fadeCounter;
    private double speedExcess;

    public Timer() {
        super("Timer", "Modifies the game tick speed.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null) return;
        float target = speed.get().floatValue();

        if (isEnabled()) {
            if (fadeCounter > 0) {
                fadeCounter--;
                float current = getTimerSpeed();
                if (current < target) {
                    setTimerSpeed(current + (target - current) / Math.max(1, fadeCounter));
                }
            } else if (mode.is("Balance")) {
                float balance = computeBalancedSpeed(target);
                setTimerSpeed(balance);
            } else {
                setTimerSpeed(target);
            }
        }
    }

    private float computeBalancedSpeed(float target) {
        if (target <= 1.0f) return target;
        speedExcess += (target - 1.0);
        if (speedExcess > 0.4) {
            speedExcess -= 0.5;
            return 0.5f;
        }
        return target;
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        originalSpeed = getTimerSpeed();
        if (originalSpeed < 0.01f) originalSpeed = 1.0f;
        fadeCounter = 0;
        speedExcess = 0;
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        fadeCounter = fadeTicks.get();
        if (fadeCounter <= 0) {
            setTimerSpeed(1.0f);
        }
    }

    private float getTimerSpeed() {
        try {
            Object timer = timerField.get(MinecraftClient.getInstance());
            return timerSpeedField.getFloat(timer);
        } catch (Exception e) {
            return 1.0f;
        }
    }

    private void setTimerSpeed(float speed) {
        try {
            Object timer = timerField.get(MinecraftClient.getInstance());
            timerSpeedField.setFloat(timer, speed);
        } catch (Exception ignored) {}
    }

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
