package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class Waifu extends RenderModule {
    private final DoubleSetting scale = setting(new DoubleSetting("Scale", "Size multiplier.", 1.0, 0.5, 3.0));
    private final IntegerSetting offsetX = setting(new IntegerSetting("Offset X", "Horizontal position offset.", 20, -500, 500));
    private final IntegerSetting offsetY = setting(new IntegerSetting("Offset Y", "Vertical position offset.", 50, -500, 500));
    private final ModeSetting style = setting(new ModeSetting("Style", "Waifu appearance.", "Marin", "Marin", "Neko", "Maid", "ZeroTwo", "Rem", "Megumin"));

    public Waifu() {
        super("Waifu", "Displays a popular anime waifu on your HUD.");
    }

    @Override
    protected void onHudRender(DrawContext ctx, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        double s = scale.get();
        int baseX = (int) (offsetX.get() * s);
        int baseY = (int) (offsetY.get() * s);

        int skin = 0xFFFFD8BE;
        int[] colors = getColors(style.get());

        int headSize = (int) (18 * s);
        int bodyW = (int) (14 * s);
        int bodyH = (int) (20 * s);

        int headX = baseX;
        int headY = baseY;
        int bodyX = headX + headSize / 2 - bodyW / 2;
        int bodyY = headY + headSize + (int) (2 * s);

        drawHair(ctx, headX, headY, headSize, s, colors[0]);
        drawFaceBase(ctx, headX, headY, headSize, s, skin);
        drawEyes(ctx, headX, headY, headSize, s, colors);
        drawMouth(ctx, headX, headY, headSize, s);
        drawBlush(ctx, headX, headY, headSize, s, colors[0]);
        drawBody(ctx, bodyX, bodyY, bodyW, bodyH, s, colors[1], skin, colors[0]);
    }

    private int[] getColors(String waifu) {
        return switch (waifu) {
            case "Marin" -> new int[]{0xFFF5A5C8, 0xFFFF6B9D};
            case "ZeroTwo" -> new int[]{0xFFFF6B9D, 0xFFE83D4A};
            case "Rem" -> new int[]{0xFF4A7DB5, 0xFF1A1A2E};
            case "Megumin" -> new int[]{0xFF2C2C2C, 0xFF8B0000};
            case "Neko" -> new int[]{0xFFFFB347, 0xFFFF6B9D};
            case "Maid" -> new int[]{0xFF2C2C2C, 0xFF1A1A2E};
            default -> new int[]{0xFFC4A882, 0xFF8B5E3C};
        };
    }

    private void drawHair(DrawContext ctx, int x, int y, int size, double s, int hairColor) {
        int padding = (int) (3 * s);
        int top = y - (int) (4 * s);
        int bottom = y + size / 3;
        ctx.fill(x - padding, top, x + size + padding, bottom, hairColor);

        int sideLockW = (int) (5 * s);
        int sideLockH = (int) (12 * s);
        ctx.fill(x - padding, y + (int) (2 * s), x - padding + sideLockW, y + (int) (2 * s) + sideLockH, hairColor);
        ctx.fill(x + size - sideLockW, y + (int) (2 * s), x + size + padding - sideLockW + sideLockW, y + (int) (2 * s) + sideLockH, hairColor);

        int fringeW = (int) (12 * s);
        int fringeH = (int) (6 * s);
        ctx.fill(x + (size - (int) fringeW) / 2, y - (int) (2 * s), x + (size - (int) fringeW) / 2 + fringeW, y - (int) (2 * s) + fringeH, hairColor);

        for (int i = 0; i < 3; i++) {
            int tw = (int) (4 * s);
            int th = (int) (3 * s);
            int tx = x + (size - tw * 3) / 2 + i * tw;
            ctx.fill(tx, y - (int) (4 * s) + i, tx + tw, y - (int) (4 * s) + i + th, darken(hairColor, 0.7f));
        }
    }

    private void drawFaceBase(DrawContext ctx, int x, int y, int size, double s, int skinColor) {
        int round = (int) (2 * s);
        ctx.fill(x, y + round, x + size, y + size - round, skinColor);
        ctx.fill(x + round, y, x + size - round, y + size, skinColor);
    }

    private void drawEyes(DrawContext ctx, int x, int y, int size, double s, int[] colors) {
        int eyeW = (int) (6 * s);
        int eyeH = (int) (5 * s);
        int eyeY = y + (int) (7 * s);

        int leftEyeX = x + (int) (2 * s);
        int rightEyeX = x + size - (int) (2 * s) - eyeW;

        int eyeColor = colors.length > 2 ? colors[2] : 0xFF9B59B6;
        ctx.fill(leftEyeX, eyeY, leftEyeX + eyeW, eyeY + eyeH, 0xFFFFFFFF);
        ctx.fill(rightEyeX, eyeY, rightEyeX + eyeW, eyeY + eyeH, 0xFFFFFFFF);

        int pupilW = (int) (4 * s);
        int pupilH = (int) (4 * s);
        int pupilY = eyeY + 1;
        int pupilXl = leftEyeX + 1;
        int pupilXr = rightEyeX + 1;
        ctx.fill(pupilXl, pupilY, pupilXl + pupilW, pupilY + pupilH, eyeColor);
        ctx.fill(pupilXr, pupilY, pupilXr + pupilW, pupilY + pupilH, eyeColor);

        int hlW = (int) (2 * s);
        int hlH = (int) (2 * s);
        ctx.fill(pupilXl + 1, pupilY, pupilXl + 1 + hlW, pupilY + hlH, 0xFFFFFFFF);
        ctx.fill(pupilXr + 1, pupilY, pupilXr + 1 + hlW, pupilY + hlH, 0xFFFFFFFF);

        int contourW = eyeW + 2;
        ctx.fill(leftEyeX - 1, eyeY, leftEyeX - 1 + contourW, eyeY + 1, 0xFF2C2C2C);
        ctx.fill(rightEyeX - 1, eyeY, rightEyeX - 1 + contourW, eyeY + 1, 0xFF2C2C2C);
    }

    private void drawMouth(DrawContext ctx, int x, int y, int size, double s) {
        int mx = x + size / 2;
        int my = y + (int) (12 * s);
        int mw = (int) (3 * s);
        ctx.fill(mx - mw / 2, my, mx + mw / 2, my + 1, 0xFFFF6B9D);
    }

    private void drawBlush(DrawContext ctx, int x, int y, int size, double s, int hairColor) {
        int blushW = (int) (4 * s);
        int blushH = (int) (2 * s);
        int blushY = y + (int) (9 * s);
        ctx.fill(x - 1, blushY, x - 1 + blushW, blushY + blushH, 0x40FF6B9D);
        ctx.fill(x + size - blushW + 1, blushY, x + size + 1, blushY + blushH, 0x40FF6B9D);
    }

    private void drawBody(DrawContext ctx, int x, int y, int w, int h, double s, int outfitColor, int skinColor, int hairColor) {
        ctx.fill(x, y + (int) (2 * s), x + w, y + h - (int) (6 * s), outfitColor);

        int armW = (int) (4 * s);
        int armH = (int) (14 * s);
        ctx.fill(x - armW, y + (int) (4 * s), x, y + (int) (4 * s) + armH, outfitColor);
        ctx.fill(x + w, y + (int) (4 * s), x + w + armW, y + (int) (4 * s) + armH, outfitColor);

        int handW = (int) (3 * s);
        int handH = (int) (3 * s);
        ctx.fill(x - armW + 1, y + (int) (4 * s) + armH - handH, x - armW + 1 + handW, y + (int) (4 * s) + armH - handH + handH, skinColor);
        ctx.fill(x + w - 1, y + (int) (4 * s) + armH - handH, x + w - 1 + handW, y + (int) (4 * s) + armH - handH + handH, skinColor);

        int skirtH = (int) (6 * s);
        ctx.fill(x - 2, y + h - skirtH, x + w + 2, y + h, outfitColor);

        int legW = (int) (4 * s);
        int legH = (int) (8 * s);
        ctx.fill(x + (int) (2 * s), y + h, x + (int) (2 * s) + legW, y + h + legH, skinColor);
        ctx.fill(x + w - (int) (2 * s) - legW, y + h, x + w - (int) (2 * s), y + h + legH, skinColor);

        int tailW = (int) (3 * s);
        int tailH = (int) (10 * s);
        ctx.fill(x + w + armW, y - (int) (2 * s), x + w + armW + tailW, y - (int) (2 * s) + tailH, hairColor);
    }

    private int darken(int color, float factor) {
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
