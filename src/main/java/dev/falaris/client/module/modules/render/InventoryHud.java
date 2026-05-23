package dev.falaris.client.module.modules.render;

import dev.falaris.client.gui.click.UiColor;
import dev.falaris.client.gui.click.UiRender;
import dev.falaris.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

public final class InventoryHud extends RenderModule {
    private final BooleanSetting background = setting(new BooleanSetting("Background", "Draw panel behind info.", true));

    public InventoryHud() {
        super("InventoryHUD", "Displays inventory preview on screen.");
    }

    @Override
    protected void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int x = 20, y = 150;
        int rows = 3, cols = 9;
        int size = 18;
        int width = cols * size + 10;
        int height = rows * size + 10;

        if (background.enabled()) {
            UiRender.fillRound(context, x - 5, y - 5, width, height, 4, UiColor.argb(120, 10, 10, 15));
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int index = (r + 1) * 9 + c;
                ItemStack stack = client.player.getInventory().getStack(index);
                if (!stack.isEmpty()) {
                    int posX = x + c * size;
                    int posY = y + r * size;
                    context.drawItem(stack, posX, posY);
                    context.drawStackOverlay(client.textRenderer, stack, posX, posY);
                }
            }
        }
    }
}
