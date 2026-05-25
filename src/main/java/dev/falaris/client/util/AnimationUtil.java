package dev.falaris.client.util;

public final class AnimationUtil {
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.min(1.0f, Math.max(0.0f, t));
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * Math.min(1.0, Math.max(0.0, t));
    }

    public static float smoothStep(float a, float b, float t) {
        t = Math.min(1.0f, Math.max(0.0f, t));
        t = t * t * (3.0f - 2.0f * t);
        return a + (b - a) * t;
    }

    public static float easeOut(float a, float b, float t) {
        t = Math.min(1.0f, Math.max(0.0f, t));
        t = 1.0f - (1.0f - t) * (1.0f - t);
        return a + (b - a) * t;
    }

    public static float easeInOut(float a, float b, float t) {
        t = Math.min(1.0f, Math.max(0.0f, t));
        t = t < 0.5f ? 2.0f * t * t : 1.0f - (float)Math.pow(-2.0f * t + 2.0f, 2.0f) / 2.0f;
        return a + (b - a) * t;
    }

    public static class Animation {
        private float value;
        private float target;
        private float speed;

        public Animation(float initial, float speed) {
            this.value = initial;
            this.target = initial;
            this.speed = speed;
        }

        public void update(float delta) { value = lerp(value, target, delta * speed); }
        public void setTarget(float t) { this.target = t; }
        public float getValue() { return value; }
        public float getTarget() { return target; }
        public boolean isDone() { return Math.abs(value - target) < 0.001f; }
        public void snap() { value = target; }
    }
}
