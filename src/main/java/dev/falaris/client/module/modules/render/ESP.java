package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class ESP extends RenderModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Max render range.", 96.0, 8.0, 256.0));
    private final DoubleSetting expand = setting(new DoubleSetting("Expand", "Box expansion.", 0.1, 0.0, 0.5));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Show players.", true));
    private final BooleanSetting hostiles = setting(new BooleanSetting("Hostiles", "Show hostile mobs.", true));
    private final BooleanSetting passives = setting(new BooleanSetting("Passives", "Show passive mobs.", false));
    private final BooleanSetting invisibles = setting(new BooleanSetting("Invisibles", "Show invisible.", true));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Show behind walls.", true));
    private final BooleanSetting healthColor = setting(new BooleanSetting("Health Color", "Color by health.", false));
    private final BooleanSetting showItems = setting(new BooleanSetting("Show Items", "ESP for ground items.", false));
    private final ModeSetting style = setting(new ModeSetting("Style", "ESP style.", "Box", "Box", "Outline", "2D", "Both"));

    public ESP() {
        super("ESP", "Prestige-style entity boxes with health colors and 2D tags.");
    }

    @Override
    protected void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        Camera camera = client.gameRenderer.getCamera();
        Vec3d cam = camera.getCameraPos();

        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player || !entity.isAlive()) continue;
            if (!invisibles.enabled() && entity.isInvisible()) continue;
            if (client.player.squaredDistanceTo(entity) > range.get() * range.get()) continue;

            if (entity instanceof LivingEntity le) {
                if (!allowed(le)) continue;
                RenderUtil.Color c = healthColor.enabled() ? healthColor(le) : color(le);
                Box bb = le.getBoundingBox().expand(expand.get());
                boolean wall = throughWalls.enabled();
                String s = style.get();

                if (s.equals("Box") || s.equals("Both"))
                    RenderUtil.drawBox(context, bb, c, wall);
                if (s.equals("Outline") || s.equals("Both"))
                    RenderUtil.drawBox(context, bb, RenderUtil.Color.rgba((int)(c.red()*255), (int)(c.green()*255), (int)(c.blue()*255), 60), wall);
                if (s.equals("2D") || s.equals("Both"))
                    draw2DTag(context, le, c, cam);
            } else if (showItems.enabled() && entity instanceof net.minecraft.entity.ItemEntity ie) {
                if (!ie.getStack().isEmpty())
                    RenderUtil.drawBox(context, ie.getBoundingBox().expand(0.2), RenderUtil.Color.rgba(255, 220, 80, 180), throughWalls.enabled());
            }
        }
    }

    private void draw2DTag(WorldRenderContext context, LivingEntity entity, RenderUtil.Color c, Vec3d cam) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        MatrixStack ms = context.matrices();
        double x = entity.getX() - cam.x;
        double y = entity.getY() - cam.y + entity.getHeight() + 0.5;
        double z = entity.getZ() - cam.z;

        ms.push();
        ms.translate(x, y, z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-client.gameRenderer.getCamera().getYaw()));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(client.gameRenderer.getCamera().getPitch()));
        ms.scale(-0.025f, -0.025f, 0.025f);

        String hp = entity instanceof PlayerEntity pe ? String.format("%.0f", pe.getHealth()) : "";
        String text = hp.isEmpty() ? String.valueOf((int) entity.getHealth()) : hp + "❤";
        float tw = tr.getWidth(text);
        var cons = context.consumers();
        if (cons == null) { ms.pop(); return; }
        Matrix4f mat = ms.peek().getPositionMatrix();
        tr.draw(text, -tw / 2, 0, c.asArgb(), false, mat, cons, net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH, 0x44000000, 0xF000F0);
        ms.pop();
    }

    private RenderUtil.Color healthColor(LivingEntity e) {
        float hp = e.getHealth() / e.getMaxHealth();
        int r = (int) (255 * (1 - hp));
        int g = (int) (255 * hp);
        return RenderUtil.Color.rgba(r, g, 60, 200);
    }

    private boolean allowed(Entity e) {
        if (e instanceof PlayerEntity) return players.enabled();
        if (e instanceof HostileEntity) return hostiles.enabled();
        if (e instanceof PassiveEntity) return passives.enabled();
        return false;
    }

    private RenderUtil.Color color(Entity entity) {
        if (entity instanceof PlayerEntity) return RenderUtil.Color.rgba(122, 156, 255, 200);
        if (entity instanceof HostileEntity) return RenderUtil.Color.rgba(255, 92, 122, 200);
        return RenderUtil.Color.rgba(120, 235, 170, 200);
    }
}
