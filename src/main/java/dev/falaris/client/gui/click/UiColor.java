package dev.falaris.client.gui.click;

public final class UiColor {
    private UiColor() {
    }

    public static int rgb(int red, int green, int blue) {
        return argb(255, red, green, blue);
    }

    public static int argb(int alpha, int red, int green, int blue) {
        return (alpha & 255) << 24
                | (red & 255) << 16
                | (green & 255) << 8
                | (blue & 255);
    }
}
