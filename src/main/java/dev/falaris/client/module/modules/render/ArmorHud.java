package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public final class ArmorHud extends RenderModule {
    private final ModeSetting display = setting(new ModeSetting("Display", "Display mode.", "Horizontal", "Horizontal", "Vertical", "Compact", "Off"));
    private final BooleanSetting showDurability = setting(new BooleanSetting("Durability", "Show numeric durability.", true));
    private final BooleanSetting showHands = setting(new BooleanSetting("Show Hands", "Show main hand and offhand items.", true));
    private final BooleanSetting background = setting(new BooleanSetting("Background", "Draw dark background behind items.", true));
    private final IntegerSetting offsetX = setting(new IntegerSetting("Offset X", "Horizontal offset.", 4, 0, 800));
    private final IntegerSetting offsetY = setting(new IntegerSetting("Offset Y", "Vertical offset.", 50, 0, 600));

    public ArmorHud() {
        super("ArmorHUD", "Displays armor and hand items with durability.");
    }

    @Override
    protected void onHudRender(DrawContext ctx, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || display.is("Off")) return;

        EquipmentSlot[] armorSlots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        boolean compact = display.is("Compact");
        int dir = display.is("Vertical") ? 0 : 1;
        int spacing = compact ? 18 : 20;
        int x = offsetX.get();
        int y = offsetY.get();

        // Offhand
        if (showHands.enabled() && !client.player.getOffHandStack().isEmpty()) {
            renderItem(ctx, client.player.getEquippedStack(EquipmentSlot.OFFHAND), x, y, client);
            x += dir * spacing;
            y += (1 - dir) * spacing;
        }

        // Armor
        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = client.player.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                renderItem(ctx, stack, x, y, client);
            } else if (!compact) {
                renderEmpty(ctx, x, y);
            }
            x += dir * spacing;
            y += (1 - dir) * spacing;
        }

        // Main hand
        if (showHands.enabled() && !client.player.getMainHandStack().isEmpty()) {
            renderItem(ctx, client.player.getMainHandStack(), x, y, client);
        }
    }

    private void renderItem(DrawContext ctx, ItemStack stack, int x, int y, MinecraftClient client) {
        if (stack.isEmpty()) return;

        if (background.enabled()) {
            ctx.fill(x - 1, y - 1, x + 17, y + 19, 0x50000000);
        }

        ctx.drawItem(stack, x, y);

        if (showDurability.enabled() && stack.isDamageable()) {
            Integer dmg = stack.get(net.minecraft.component.DataComponentTypes.DAMAGE);
            int max = stack.getMaxDamage();
            if (dmg != null && max > 0) {
                int remaining = max - dmg;
                int pct = remaining * 100 / max;
                int barColor = pct > 60 ? 0xFF55AA55 : (pct > 30 ? 0xFFFFAA00 : 0xFFFF5555);
                int barBg = 0x80000000;
                int barW = 14;
                int barH = 2;
                int bx = x + 1;
                int by = y + 16;
                ctx.fill(bx, by, bx + barW, by + barH, barBg);
                int fillW = Math.max(1, barW * pct / 100);
                ctx.fill(bx, by, bx + fillW, by + barH, barColor);
            }
        }
    }

    private void renderEmpty(DrawContext ctx, int x, int y) {
        if (background.enabled()) {
            ctx.fill(x - 1, y - 1, x + 17, y + 17, 0x30000000);
        }
    }
}
