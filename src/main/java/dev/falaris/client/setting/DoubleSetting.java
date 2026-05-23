package dev.falaris.client.setting;

public final class DoubleSetting extends Setting<Double> {
    private final double min;
    private final double max;

    public DoubleSetting(String name, String description, double defaultValue, double min, double max) {
        super(name, description, clamp(defaultValue, min, max));
        this.min = min;
        this.max = max;
    }

    @Override
    public void set(Double value) {
        super.set(clamp(value, min, max));
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
