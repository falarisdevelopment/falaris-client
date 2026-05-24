package dev.falaris.client.module.modules.render;

import dev.falaris.client.gui.click.UiRender;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class Keystrokes extends RenderModule {
    private final IntegerSetting offsetX = setting(new IntegerSetting("Offset X", "Horizontal position.", 4, 0, 800));
    private final IntegerSetting offsetY = setting(new IntegerSetting("Offset Y", "Vertical position.", 100, 0, 600));
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Key display mode.", "WASD", "WASD", "WASD+Mouse", "WASD+Mouse+Space"));
    private final IntegerSetting keySize = setting(new IntegerSetting("Key Size", "Size of each key.", 22, 14, 40));

    private static final int KEY_PRESSED = 0x60FFFFFF;
    private static final int KEY_IDLE = 0x40000000;

    public Keystrokes() {
        super("Keystrokes", "Shows WASD and mouse button presses on screen.");
    }

    @Override
    protected void onHudRender(DrawContext ctx, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int ks = keySize.get();
        int gap = 2;
        int x = offsetX.get();
        int y = offsetY.get();

        drawKey(ctx, x + ks + gap, y, "W", client.options.forwardKey.isPressed(), ks);
        drawKey(ctx, x, y + ks + gap, "A", client.options.leftKey.isPressed(), ks);
        drawKey(ctx, x + ks + gap, y + ks + gap, "S", client.options.backKey.isPressed(), ks);
        drawKey(ctx, x + (ks + gap) * 2, y + ks + gap, "D", client.options.rightKey.isPressed(), ks);

        if (!mode.is("WASD")) {
            int my = y + (ks + gap) * 2 + 2;
            drawKey(ctx, x, my, "LMB", client.options.attackKey.isPressed(), ks * 2 + gap);
            drawKey(ctx, x + (ks + gap) * 2 + gap, my, "RMB", client.options.useKey.isPressed(), ks * 2 + gap);
        }

        if (mode.is("WASD+Mouse+Space")) {
            int sy = y + (ks + gap) * 2 + (mode.is("WASD+Mouse+Space") ? ks + gap + 2 : 0) + 2;
            drawKey(ctx, x, sy, "SPACE", client.options.jumpKey.isPressed(), (ks + gap) * 3);
        }
    }

    private void drawKey(DrawContext ctx, int x, int y, String label, boolean pressed, int width) {
        int bg = pressed ? KEY_PRESSED : KEY_IDLE;
        int border = pressed ? 0x30FFFFFF : 0x20000000;
        UiRender.fillRound(ctx, x, y, width, keySize.get(), 3, bg);
        ctx.fill(x, y, x + width, y + 1, border);
        var tr = MinecraftClient.getInstance().textRenderer;
        int tx = x + width / 2 - tr.getWidth(label) / 2;
        int ty = y + keySize.get() / 2 - 4;
        ctx.drawText(tr, label, tx, ty, pressed ? 0xFF000000 : 0xFFCCCCCC, true);
    }
}
