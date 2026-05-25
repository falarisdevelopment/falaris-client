package dev.falaris.client.module.modules.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.List;

public final class RenderUtil {
    private RenderUtil() {
    }

    public static void drawBox(WorldRenderContext context, Box box, Color color, boolean throughWalls) {
        MatrixStack matrices = context.matrices();
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cam = camera.getCameraPos();
        double x1 = box.minX - cam.x;
        double y1 = box.minY - cam.y;
        double z1 = box.minZ - cam.z;
        double x2 = box.maxX - cam.x;
        double y2 = box.maxY - cam.y;
        double z2 = box.maxZ - cam.z;

        boolean depthDisabled = false;
        if (throughWalls) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            depthDisabled = true;
        }

        VertexConsumer buffer = consumers.getBuffer(RenderLayers.lines());
        MatrixStack.Entry entry = matrices.peek();

        drawLine(buffer, entry, x1, y1, z1, x2, y1, z1, color);
        drawLine(buffer, entry, x2, y1, z1, x2, y1, z2, color);
        drawLine(buffer, entry, x2, y1, z2, x1, y1, z2, color);
        drawLine(buffer, entry, x1, y1, z2, x1, y1, z1, color);

        drawLine(buffer, entry, x1, y2, z1, x2, y2, z1, color);
        drawLine(buffer, entry, x2, y2, z1, x2, y2, z2, color);
        drawLine(buffer, entry, x2, y2, z2, x1, y2, z2, color);
        drawLine(buffer, entry, x1, y2, z2, x1, y2, z1, color);

        drawLine(buffer, entry, x1, y1, z1, x1, y2, z1, color);
        drawLine(buffer, entry, x2, y1, z1, x2, y2, z1, color);
        drawLine(buffer, entry, x2, y1, z2, x2, y2, z2, color);
        drawLine(buffer, entry, x1, y1, z2, x1, y2, z2, color);

        if (depthDisabled) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
    }

    public static void drawBox(WorldRenderContext context, Box box, Color color) {
        drawBox(context, box, color, false);
    }

    private static void drawLine(VertexConsumer buffer, MatrixStack.Entry entry, double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        buffer.vertex(entry, (float)x1, (float)y1, (float)z1).color(color.red, color.green, color.blue, color.alpha).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, (float)x2, (float)y2, (float)z2).color(color.red, color.green, color.blue, color.alpha).normal(entry, 0, 1, 0).lineWidth(1.0f);
    }

    public static void drawBlockBox(WorldRenderContext context, BlockPos pos, Color color) {
        drawBox(context, new Box(pos), color, false);
    }

    static void drawBlockBox(WorldRenderContext context, BlockPos pos, Color color, boolean throughWalls) {
        drawBox(context, new Box(pos), color, throughWalls);
    }

    static void drawEntityBox(WorldRenderContext context, Entity entity, Color color, double expand) {
        drawBox(context, entity.getBoundingBox().expand(expand), color, false);
    }

    static void drawEntityBox(WorldRenderContext context, Entity entity, Color color, double expand, boolean throughWalls) {
        drawBox(context, entity.getBoundingBox().expand(expand), color, throughWalls);
    }

    static void drawNametag(WorldRenderContext context, Entity entity, List<String> lines, Color background, int textColor, double yOffset, double scale) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        MatrixStack matrices = context.matrices();
        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();
        
        double x = entity.getX() - camPos.x;
        double y = entity.getY() - camPos.y + entity.getHeight() + yOffset;
        double z = entity.getZ() - camPos.z;

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.scale((float)-scale, (float)-scale, (float)scale);

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumerProvider consumers = context.consumers();
        
        float currentY = 0;
        for (String line : lines) {
            float width = textRenderer.getWidth(line);
            textRenderer.draw(line, -width / 2, currentY, textColor, false, matrix, consumers, TextRenderer.TextLayerType.NORMAL, background.asArgb(), 0xF000F0);
            currentY += textRenderer.fontHeight + 1;
        }

        matrices.pop();
    }

    static String compactItem(net.minecraft.item.ItemStack stack) {
        if (stack.isEmpty()) {
            return "-";
        }
        String name = stack.getName().getString();
        return stack.getCount() > 1 ? name + " x" + stack.getCount() : name;
    }

    public record Color(float red, float green, float blue, float alpha) {
        public static Color rgba(int red, int green, int blue, int alpha) {
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
