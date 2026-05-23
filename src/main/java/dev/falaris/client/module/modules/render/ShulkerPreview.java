package dev.falaris.client.module.modules.render;

import dev.falaris.client.event.events.RenderHudEvent;
import dev.falaris.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ShulkerPreview extends RenderModule {
    private final BooleanSetting requireShift = setting(new BooleanSetting("Require Shift", "Only show preview while holding shift.", true));
    private static final Identifier SHULKER_GUI = Identifier.of("minecraft", "textures/gui/container/shulker_box.png");

    public ShulkerPreview() {
        super("ShulkerPreview", "Displays the contents of shulker boxes in your inventory.");
    }

    @Override
    protected void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !(client.currentScreen instanceof HandledScreen<?> screen)) {
            return;
        }

        if (requireShift.enabled() && !client.options.sneakKey.isPressed()) {
            return;
        }

        Slot hoveredSlot = null;
        try {
            java.lang.reflect.Field field = HandledScreen.class.getDeclaredField("hoveredSlot");
            field.setAccessible(true);
            hoveredSlot = (Slot) field.get(screen);
        } catch (Exception ignored) {}
        
        if (hoveredSlot == null || !hoveredSlot.hasStack()) {
            return;
        }

        ItemStack stack = hoveredSlot.getStack();
        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container == null) {
            return;
        }

        drawPreview(context, container);
    }

    private void drawPreview(DrawContext context, ContainerComponent container) {
        int x = context.getScaledWindowWidth() / 2 + 10;
        int y = context.getScaledWindowHeight() / 2 - 40;

        // Draw background
        context.drawTexturedQuad(SHULKER_GUI, x, y, 176, 76, 0.0f, 0.0f, 1.0f, 1.0f);
        context.drawText(MinecraftClient.getInstance().textRenderer, "Shulker Box", x + 8, y + 6, 0x404040, false);

        int i = 0;
        for (ItemStack itemStack : container.iterateNonEmpty()) {
            int slotX = x + 8 + (i % 9) * 18;
            int slotY = y + 18 + (i / 9) * 18;
            context.drawItem(itemStack, slotX, slotY);
            context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, itemStack, slotX, slotY);
            i++;
        }

        // no matrix stack push/pop needed for 2D preview
    }
}
