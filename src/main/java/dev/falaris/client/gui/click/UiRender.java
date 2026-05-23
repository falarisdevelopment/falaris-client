package dev.falaris.client.gui.click;

import net.minecraft.client.gui.DrawContext;

public final class UiRender {
    private UiRender() {
    }

    public static void fillRound(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        if (width < radius * 2 || height < radius * 2) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + width, y + height - radius, color);

        fillCircleQuarter(context, x + radius, y + radius, radius, color, Corner.TOP_LEFT);
        fillCircleQuarter(context, x + width - radius - 1, y + radius, radius, color, Corner.TOP_RIGHT);
        fillCircleQuarter(context, x + radius, y + height - radius - 1, radius, color, Corner.BOTTOM_LEFT);
        fillCircleQuarter(context, x + width - radius - 1, y + height - radius - 1, radius, color, Corner.BOTTOM_RIGHT);
    }

    public static void fillHorizontalGradient(DrawContext context, int x, int y, int width, int height, int leftColor, int rightColor) {
        context.fillGradient(x, y, x + width, y + height, leftColor, rightColor);
    }

    public static void fillVerticalGradient(DrawContext context, int x, int y, int width, int height, int topColor, int bottomColor) {
        context.fillGradient(x, y, x + width, y + height, topColor, bottomColor);
    }

    public static int mix(int from, int to, float amount) {
        int a1 = (from >> 24) & 0xff;
        int r1 = (from >> 16) & 0xff;
        int g1 = (from >> 8) & 0xff;
        int b1 = from & 0xff;
        int a2 = (to >> 24) & 0xff;
        int r2 = (to >> 16) & 0xff;
        int g2 = (to >> 8) & 0xff;
        int b2 = to & 0xff;
        int a = (int) (a1 + (a2 - a1) * amount);
        int r = (int) (r1 + (r2 - r1) * amount);
        int g = (int) (g1 + (g2 - g1) * amount);
        int b = (int) (b1 + (b2 - b1) * amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void fillCircleQuarter(DrawContext context, int centerX, int centerY, int radius, int color, Corner corner) {
        for (int i = 0; i < radius; i++) {
            int width = (int) Math.sqrt(radius * radius - i * i);
            int drawY = switch (corner) {
                case TOP_LEFT, TOP_RIGHT -> centerY - i - 1;
                case BOTTOM_LEFT, BOTTOM_RIGHT -> centerY + i;
            };
            int drawX = switch (corner) {
                case TOP_LEFT, BOTTOM_LEFT -> centerX - width;
                case TOP_RIGHT, BOTTOM_RIGHT -> centerX;
            };
            context.fill(drawX, drawY, drawX + width, drawY + 1, color);
        }
    }

    private enum Corner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }
}
