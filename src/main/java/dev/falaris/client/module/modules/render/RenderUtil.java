package dev.falaris.client.module.modules.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

final class RenderUtil {
    private RenderUtil() {
    }

    static void drawBox(WorldRenderContext context, Box box, Color color) {
        // 1.21.11 moved low-level world line drawing into the render state pipeline.
        // Keep this as a no-op until a dedicated render pipeline is added.
    }

    static void drawBlockBox(WorldRenderContext context, BlockPos pos, Color color) {
        drawBox(context, new Box(pos), color);
    }

    static void drawEntityBox(WorldRenderContext context, Entity entity, Color color, double expand) {
        drawBox(context, entity.getBoundingBox().expand(expand), color);
    }

    static void drawNametag(WorldRenderContext context, Entity entity, List<String> lines, Color background, int textColor, double yOffset, double scale) {
        // See drawBox: world-space text now needs an extraction/render state path.
    }

    static String compactItem(net.minecraft.item.ItemStack stack) {
        if (stack.isEmpty()) {
            return "-";
        }
        String name = stack.getName().getString();
        return stack.getCount() > 1 ? name + " x" + stack.getCount() : name;
    }

    record Color(float red, float green, float blue, float alpha) {
        static Color rgba(int red, int green, int blue, int alpha) {
            return new Color(red / 255.0f, green / 255.0f, blue / 255.0f, alpha / 255.0f);
        }

        int asArgb() {
            int a = Math.round(alpha * 255.0f) & 255;
            int r = Math.round(red * 255.0f) & 255;
            int g = Math.round(green * 255.0f) & 255;
            int b = Math.round(blue * 255.0f) & 255;
            return a << 24 | r << 16 | g << 8 | b;
        }
    }
}
