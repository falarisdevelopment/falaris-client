package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

public final class Tracers extends RenderModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum tracer range.", 128.0, 8.0, 256.0));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Trace to players.", true));
    private final BooleanSetting hostiles = setting(new BooleanSetting("Hostiles", "Trace to hostile mobs.", true));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Show tracers through blocks.", true));
    private final BooleanSetting onlyMoving = setting(new BooleanSetting("Only When Moving", "Only show tracers when player moves.", false));
    private final ModeSetting colorMode = setting(new ModeSetting("Color Mode", "Tracer color scheme.", "Distance", "Distance", "Health", "Friend", "Fade"));
    private final BooleanSetting toggleBlink = setting(new BooleanSetting("Blink", "Blinking tracer effect.", false));

    public Tracers() {
        super("Tracers", "Meteor-style player tracers with distance-based coloring.");
    }

    @Override
    protected void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        if (onlyMoving.enabled()) {
            double dx = client.player.getX() - client.player.lastRenderX;
            double dz = client.player.getZ() - client.player.lastRenderZ;
            if (dx * dx + dz * dz < 0.001) return;
        }

        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();
        Vec3d eyes = client.player.getEyePos();

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;

        VertexConsumer lines = consumers.getBuffer(net.minecraft.client.render.RenderLayers.lines());

        var entities = client.world.getEntities();
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity) || entity == client.player) continue;
            if (!entity.isAlive()) continue;
            if (client.player.squaredDistanceTo(entity) > range.get() * range.get()) continue;
            if (entity instanceof PlayerEntity && !players.enabled()) continue;
            if (entity instanceof HostileEntity && !hostiles.enabled()) continue;

            Vec3d target = entity.getBoundingBox().getCenter();
            double dist = client.player.distanceTo(entity);

            if (!throughWalls.enabled() && !client.player.canSee(entity)) continue;

            int color = getEntityColor(entity, dist, client);

            if (toggleBlink.enabled()) {
                float phase = (System.currentTimeMillis() % 1000) / 1000.0f;
                int alpha = (int) (MathHelper.sin(phase * MathHelper.TAU) * 127 + 128);
                color = (color & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
            }

            float x1 = (float) (eyes.x - camPos.x);
            float y1 = (float) (eyes.y - camPos.y);
            float z1 = (float) (eyes.z - camPos.z);
            float x2 = (float) (target.x - camPos.x);
            float y2 = (float) (target.y - camPos.y);
            float z2 = (float) (target.z - camPos.z);

            MatrixStack.Entry entry = context.matrices().peek();

            lines.vertex(entry, x1, y1, z1).color(color);
            lines.vertex(entry, x2, y2, z2).color(color);
        }
    }

    private int getEntityColor(Entity entity, double distance, MinecraftClient client) {
        return switch (colorMode.get()) {
            case "Health" -> {
                float health = 1.0f;
                if (entity instanceof LivingEntity le) {
                    health = le.getHealth() / le.getMaxHealth();
                }
                int r = (int) (255 * (1 - health));
                int g = (int) (255 * health);
                yield 0xFF000000 | (r << 16) | (g << 8);
            }
            case "Friend" -> {
                if (entity instanceof PlayerEntity pe && dev.falaris.client.FalarisClient.getInstance().getFriendsManager().isFriend(pe.getName().getString())) {
                    yield 0xFF00FF00;
                }
                yield 0xFFFF0000;
            }
            case "Fade" -> {
                float factor = (float) MathHelper.clamp(distance / range.get(), 0.0, 1.0);
                int r = (int) (255 * (1 - factor));
                int g = (int) (255 * factor);
                int b = (int) (128 * (1 - factor));
                yield 0xFF000000 | (r << 16) | (g << 8) | b;
            }
            default -> { // Distance
                float factor = (float) MathHelper.clamp(distance / 50.0, 0.0, 1.0);
                int r = (int) (255 * factor);
                int g = (int) (255 * (1 - factor));
                yield 0xFF000000 | (r << 16) | (g << 8);
            }
        };
    }
}
