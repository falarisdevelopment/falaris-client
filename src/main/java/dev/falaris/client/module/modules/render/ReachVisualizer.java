package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

public final class ReachVisualizer extends RenderModule {
    private final DoubleSetting reach = setting(new DoubleSetting("Reach", "Reach distance to visualize.", 3.0, 1.0, 6.0));
    private final BooleanSetting showCircle = setting(new BooleanSetting("Show Circle", "Draw reach circle on ground.", true));
    private final BooleanSetting showLine = setting(new BooleanSetting("Show Line", "Draw line from you to crosshair target.", true));
    private final IntegerSetting segments = setting(new IntegerSetting("Segments", "Circle smoothness.", 48, 12, 96));
    private final DoubleSetting colorR = setting(new DoubleSetting("Red", "Circle color red.", 0.0, 0.0, 1.0));
    private final DoubleSetting colorG = setting(new DoubleSetting("Green", "Circle color green.", 1.0, 0.0, 1.0));
    private final DoubleSetting colorB = setting(new DoubleSetting("Blue", "Circle color blue.", 0.8, 0.0, 1.0));
    private final DoubleSetting lineR = setting(new DoubleSetting("Line Red", "Line color red.", 1.0, 0.0, 1.0));
    private final DoubleSetting lineG = setting(new DoubleSetting("Line Green", "Line color green.", 0.3, 0.0, 1.0));
    private final DoubleSetting lineB = setting(new DoubleSetting("Line Blue", "Line color blue.", 0.3, 0.0, 1.0));

    public ReachVisualizer() {
        super("ReachVisualizer", "Shows a reach circle on the ground and a line to your crosshair target.");
    }

    @Override
    protected void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        MatrixStack matrices = context.matrices();
        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        Camera camera = client.gameRenderer.getCamera();
        Vec3d cam = camera.getCameraPos();
        float r = reach.get().floatValue();

        if (showCircle.enabled()) {
            drawCircle(context, client, cam, r);
        }

        if (showLine.enabled()) {
            drawTargetLine(context, client, cam);
        }
    }

    private void drawCircle(WorldRenderContext context, MinecraftClient client, Vec3d cam, float radius) {
        VertexConsumer buffer = context.consumers().getBuffer(RenderLayers.lines());
        MatrixStack.Entry entry = context.matrices().peek();

        double px = client.player.getX() - cam.x;
        double py = client.player.getY() - cam.y;
        double pz = client.player.getZ() - cam.z;

        int segs = segments.get();
        double r = radius;
        float cr = colorR.get().floatValue();
        float cg = colorG.get().floatValue();
        float cb = colorB.get().floatValue();

        for (int i = 0; i < segs; i++) {
            double a1 = Math.PI * 2 * i / segs;
            double a2 = Math.PI * 2 * (i + 1) / segs;

            double x1 = px + Math.cos(a1) * r;
            double z1 = pz + Math.sin(a1) * r;
            double x2 = px + Math.cos(a2) * r;
            double z2 = pz + Math.sin(a2) * r;

            buffer.vertex(entry, (float) x1, (float) py, (float) z1)
                .color(cr, cg, cb, 0.6f).normal(entry, 0, 1, 0).lineWidth(1.5f);
            buffer.vertex(entry, (float) x2, (float) py, (float) z2)
                .color(cr, cg, cb, 0.6f).normal(entry, 0, 1, 0).lineWidth(1.5f);
        }
    }

    private void drawTargetLine(WorldRenderContext context, MinecraftClient client, Vec3d cam) {
        if (!(client.crosshairTarget instanceof EntityHitResult ehr)) return;

        VertexConsumer buffer = context.consumers().getBuffer(RenderLayers.lines());
        MatrixStack.Entry entry = context.matrices().peek();

        Vec3d start = client.player.getCameraPosVec(1.0f).subtract(cam);
        Vec3d end = ehr.getEntity().getBoundingBox().getCenter().subtract(cam);

        float lr = lineR.get().floatValue();
        float lg = lineG.get().floatValue();
        float lb = lineB.get().floatValue();

        buffer.vertex(entry, (float) start.x, (float) start.y, (float) start.z)
            .color(lr, lg, lb, 0.5f).normal(entry, 0, 1, 0).lineWidth(1.0f);
        buffer.vertex(entry, (float) end.x, (float) end.y, (float) end.z)
            .color(lr, lg, lb, 0.3f).normal(entry, 0, 1, 0).lineWidth(1.0f);
    }
}
