package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public final class CPSCounter extends RenderModule {
    private final ModeSetting position = setting(new ModeSetting("Position", "Screen position.", "Top Left", "Top Left", "Top Right", "Bottom Left", "Bottom Right"));
    private final IntegerSetting offsetX = setting(new IntegerSetting("Offset X", "Horizontal offset.", 4, 0, 800));
    private final IntegerSetting offsetY = setting(new IntegerSetting("Offset Y", "Vertical offset.", 24, 0, 600));
    private final BooleanSetting showLeft = setting(new BooleanSetting("Show Left", "Show left-click CPS.", true));
    private final BooleanSetting showRight = setting(new BooleanSetting("Show Right", "Show right-click CPS.", true));

    private static final long[] LEFT_CLICKS = new long[20];
    private static final long[] RIGHT_CLICKS = new long[20];
    private static int leftIndex, rightIndex;

    public CPSCounter() {
        super("CPSCounter", "Shows your clicks per second.");
    }

    public static void onLeftClick() {
        LEFT_CLICKS[leftIndex] = System.currentTimeMillis();
        leftIndex = (leftIndex + 1) % 20;
    }

    public static void onRightClick() {
        RIGHT_CLICKS[rightIndex] = System.currentTimeMillis();
        rightIndex = (rightIndex + 1) % 20;
    }

    @Override
    protected void onHudRender(DrawContext ctx, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        StringBuilder sb = new StringBuilder("CPS:");
        if (showLeft.enabled()) sb.append(" §bL§f:").append(getCPS(LEFT_CLICKS));
        if (showRight.enabled()) sb.append(" §eR§f:").append(getCPS(RIGHT_CLICKS));

        String text = sb.toString();
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        int textW = client.textRenderer.getWidth(text);

        int x = switch (position.get()) {
            case "Top Right" -> sw - textW - offsetX.get() - 4;
            case "Bottom Right" -> sw - textW - offsetX.get() - 4;
            case "Bottom Left" -> offsetX.get();
            default -> offsetX.get();
        };
        int y = switch (position.get()) {
            case "Bottom Left" -> sh - offsetY.get() - 12;
            case "Bottom Right" -> sh - offsetY.get() - 12;
            default -> offsetY.get();
        };

        ctx.fill(x - 2, y - 1, x + textW + 4, y + 10, 0x60000000);
        ctx.drawText(client.textRenderer, text, x, y + 1, 0xFFFFFFFF, true);
    }

    private static int getCPS(long[] clicks) {
        long now = System.currentTimeMillis();
        int count = 0;
        for (long t : clicks) {
            if (now - t < 1000) count++;
        }
        return count;
    }
}
