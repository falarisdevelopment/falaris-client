package dev.falaris.client.setting;

public final class IntegerSetting extends Setting<Integer> {
    private final int min;
    private final int max;

    public IntegerSetting(String name, String description, int defaultValue, int min, int max) {
        super(name, description, clamp(defaultValue, min, max));
        this.min = min;
        this.max = max;
    }

    @Override
    public void set(Integer value) {
        super.set(clamp(value, min, max));
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
