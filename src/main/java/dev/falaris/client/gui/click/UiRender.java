package dev.falaris.client.gui.click;

import net.minecraft.client.gui.DrawContext;

public final class UiRender {
    private UiRender() {}

    private static final int GLOW_LAYERS = 4;
    private static final int[] GLOW_ALPHAS;
    static {
        GLOW_ALPHAS = new int[GLOW_LAYERS];
        for (int i = 0; i < GLOW_LAYERS; i++) {
            GLOW_ALPHAS[i] = (GLOW_LAYERS - i) * 25 / GLOW_LAYERS;
        }
    }

    public static void fillRound(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w < 1 || h < 1) return;
        if (w < r * 2 || h < r * 2) { ctx.fill(x, y, x + w, y + h, color); return; }
        ctx.fill(x + r, y, x + w - r, y + h, color);
        ctx.fill(x, y + r, x + w, y + h - r, color);
        corner(ctx, x + r, y + r, r, color, 0);
        corner(ctx, x + w - r - 1, y + r, r, color, 1);
        corner(ctx, x + r, y + h - r - 1, r, color, 2);
        corner(ctx, x + w - r - 1, y + h - r - 1, r, color, 3);
    }

    public static void fillGlow(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        int baseA = (color >> 24) & 0xFF;
        int rgb = color & 0xFFFFFF;
        for (int i = 0; i < GLOW_LAYERS; i++) {
            int a = baseA * GLOW_ALPHAS[i] / 25;
            int o = i + 1;
            fillRound(ctx, x - o, y - o, w + o * 2, h + o * 2, r + o, (a << 24) | rgb);
        }
    }

    public static void fillGradH(DrawContext ctx, int x, int y, int w, int h, int left, int right) {
        ctx.fillGradient(x, y, x + w, y + h, left, right);
    }

    public static void fillGradV(DrawContext ctx, int x, int y, int w, int h, int top, int bottom) {
        ctx.fillGradient(x, y, x + w, y + h, top, bottom);
    }

    public static void toggle(DrawContext ctx, int x, int y, int w, int h, float progress) {
        int bg = mix(UiColor.toggleOff(), UiColor.green(), progress);
        fillRound(ctx, x, y, w, h, h / 2, bg);
        int ks = h - 4;
        int kx = (int) (x + 2 + (w - ks - 4) * progress);
        fillRound(ctx, kx, y + 2, ks, ks, ks / 2, UiColor.rgb(240, 242, 248));
    }

    public static void slider(DrawContext ctx, int x, int y, int w, int h, float progress, int accent) {
        fillRound(ctx, x, y, w, h, h / 2, UiColor.sliderBg());
        if (progress > 0.005f) {
            fillRound(ctx, x, y, (int) (w * progress), h, h / 2, accent);
        }
        int kx = x + (int) (w * progress);
        int ks = h + 4;
        fillRound(ctx, kx - ks / 2, y - 2, ks, ks, ks / 2, UiColor.rgb(235, 237, 242));
        fillGlow(ctx, kx - ks / 2, y - 2, ks, ks, ks / 2, UiColor.argb(30, 130, 100, 210));
    }

    public static int mix(int from, int to, float t) {
        int a1 = (from >> 24) & 0xFF, r1 = (from >> 16) & 0xFF, g1 = (from >> 8) & 0xFF, b1 = from & 0xFF;
        int a2 = (to >> 24) & 0xFF, r2 = (to >> 16) & 0xFF, g2 = (to >> 8) & 0xFF, b2 = to & 0xFF;
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void corner(DrawContext ctx, int cx, int cy, int r, int color, int quadrant) {
        for (int i = 0; i < r; i++) {
            int w = (int) Math.sqrt(r * r - i * i);
            int dy = (quadrant == 0 || quadrant == 1) ? cy - i - 1 : cy + i;
            int dx = (quadrant == 0 || quadrant == 2) ? cx - w : cx;
            ctx.fill(dx, dy, dx + w, dy + 1, color);
        }
    }
}
