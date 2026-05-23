package dev.falaris.client.gui.click;

import net.minecraft.client.gui.DrawContext;

public final class UiRender {
    private UiRender() {
    }

    public static void fillRound(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        int diameter = radius * 2;
        context.fill(x + radius, y, x + width - radius, y + height, color);
        context.fill(x, y + radius, x + width, y + height - radius, color);

        fillCircleQuarter(context, x + radius, y + radius, radius, color, Corner.TOP_LEFT);
        fillCircleQuarter(context, x + width - radius - 1, y + radius, radius, color, Corner.TOP_RIGHT);
        fillCircleQuarter(context, x + radius, y + height - radius - 1, radius, color, Corner.BOTTOM_LEFT);
        fillCircleQuarter(context, x + width - radius - 1, y + height - radius - 1, radius, color, Corner.BOTTOM_RIGHT);

        if (width < diameter || height < diameter) {
            context.fill(x, y, x + width, y + height, color);
        }
    }

    public static void fillHorizontalGradient(DrawContext context, int x, int y, int width, int height, int leftColor, int rightColor) {
        for (int offset = 0; offset < width; offset++) {
            float progress = width <= 1 ? 0.0f : offset / (float) (width - 1);
            context.fill(x + offset, y, x + offset + 1, y + height, mix(leftColor, rightColor, progress));
        }
    }

    public static void fillVerticalGradient(DrawContext context, int x, int y, int width, int height, int topColor, int bottomColor) {
        for (int offset = 0; offset < height; offset++) {
            float progress = height <= 1 ? 0.0f : offset / (float) (height - 1);
            context.fill(x, y + offset, x + width, y + offset + 1, mix(topColor, bottomColor, progress));
        }
    }

    private static int mix(int from, int to, float amount) {
        int a = (int) (((from >> 24 & 255) + ((to >> 24 & 255) - (from >> 24 & 255)) * amount));
        int r = (int) (((from >> 16 & 255) + ((to >> 16 & 255) - (from >> 16 & 255)) * amount));
        int g = (int) (((from >> 8 & 255) + ((to >> 8 & 255) - (from >> 8 & 255)) * amount));
        int b = (int) (((from & 255) + ((to & 255) - (from & 255)) * amount));
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static void fillCircleQuarter(DrawContext context, int centerX, int centerY, int radius, int color, Corner corner) {
        for (int offsetY = 0; offsetY <= radius; offsetY++) {
            for (int offsetX = 0; offsetX <= radius; offsetX++) {
                if (offsetX * offsetX + offsetY * offsetY > radius * radius) {
                    continue;
                }

                int drawX = switch (corner) {
                    case TOP_LEFT, BOTTOM_LEFT -> centerX - offsetX;
                    case TOP_RIGHT, BOTTOM_RIGHT -> centerX + offsetX;
                };
                int drawY = switch (corner) {
                    case TOP_LEFT, TOP_RIGHT -> centerY - offsetY;
                    case BOTTOM_LEFT, BOTTOM_RIGHT -> centerY + offsetY;
                };
                context.fill(drawX, drawY, drawX + 1, drawY + 1, color);
            }
        }
    }

    private enum Corner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }
}
