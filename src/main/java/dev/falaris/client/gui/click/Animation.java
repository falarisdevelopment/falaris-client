package dev.falaris.client.gui.click;

import net.minecraft.util.math.MathHelper;

public final class Animation {
    private float value;
    private float target;
    private float lastValue;
    private float speed;

    public Animation(float initialValue) {
        this.value = initialValue;
        this.target = initialValue;
        this.lastValue = initialValue;
        this.speed = 0.15f;
    }

    public Animation(float initialValue, float speed) {
        this(initialValue);
        this.speed = speed;
    }

    public void setTarget(float target) {
        this.target = target;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void tick() {
        lastValue = value;
        value = MathHelper.lerp(speed, value, target);
        if (Math.abs(value - target) < 0.0005f) {
            value = target;
        }
    }

    public void tick(float customSpeed) {
        lastValue = value;
        value = MathHelper.lerp(MathHelper.clamp(customSpeed, 0.0f, 1.0f), value, target);
        if (Math.abs(value - target) < 0.0005f) {
            value = target;
        }
    }

    public float get() {
        return value;
    }

    public float getTarget() {
        return target;
    }

    public float getDelta() {
        return value - lastValue;
    }

    public boolean isFinished() {
        return Math.abs(value - target) < 0.0005f;
    }

    public static float easeOut(float x) {
        return 1.0f - (float) Math.pow(1.0f - MathHelper.clamp(x, 0.0f, 1.0f), 3.0);
    }

    public static float easeOutQuart(float x) {
        return 1.0f - (float) Math.pow(1.0f - MathHelper.clamp(x, 0.0f, 1.0f), 4.0);
    }

    public static float easeOutQuint(float x) {
        return 1.0f - (float) Math.pow(1.0f - MathHelper.clamp(x, 0.0f, 1.0f), 5.0);
    }

    public static float easeInOutQuad(float x) {
        return x < 0.5f ? 2.0f * x * x : 1.0f - (float) Math.pow(-2.0f * x + 2.0f, 2.0) / 2.0f;
    }

    public static float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float) Math.pow(x - 1.0f, 3.0) + c1 * (float) Math.pow(x - 1.0f, 2.0);
    }

    public static float easeOutElastic(float x) {
        if (x == 0 || x == 1) return x;
        return (float) (Math.pow(2.0, -10.0 * x) * Math.sin((x * 10.0 - 0.75) * (2.0 * Math.PI) / 3.0) + 1.0);
    }
}
