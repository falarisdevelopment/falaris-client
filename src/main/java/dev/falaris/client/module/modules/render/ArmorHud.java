package dev.falaris.client.module.modules.render;

import dev.falaris.client.gui.click.UiColor;
import dev.falaris.client.gui.click.UiRender;
import dev.falaris.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public final class ArmorHud extends RenderModule {
    private final BooleanSetting background = setting(new BooleanSetting("Background", "Draw a subtle panel behind armor info.", true));

    public ArmorHud() {
        super("ArmorHUD", "Displays armor status and durability on screen.");
    }

    @Override
    protected void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int x = 20, y = 20;
        int width = 120, height = 4 * 22 + 10;

        if (background.enabled()) {
            UiRender.fillRound(context, x - 10, y - 10, width, height, 6, UiColor.argb(120, 10, 10, 15));
        }

        int armorY = y;
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = client.player.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                context.drawItem(stack, x, armorY);
                context.drawStackOverlay(client.textRenderer, stack, x, armorY);

                int dur = stack.getMaxDamage() - stack.getDamage();
                context.drawText(client.textRenderer, String.valueOf(dur), x + 20, armorY + 4, 0xFFFFFF, true);
            }
            armorY += 22;
        }
    }
}
