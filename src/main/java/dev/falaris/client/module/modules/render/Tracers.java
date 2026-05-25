package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class Tracers extends RenderModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Max tracer range.", 128.0, 8.0, 512.0));
    private final DoubleSetting lineWidth = setting(new DoubleSetting("Line Width", "Tracer thickness.", 1.5, 0.5, 4.0));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Tracers to players.", true));
    private final BooleanSetting hostiles = setting(new BooleanSetting("Hostiles", "Tracers to hostile mobs.", false));
    private final BooleanSetting passives = setting(new BooleanSetting("Passives", "Tracers to passive mobs.", false));
    private final BooleanSetting invisibles = setting(new BooleanSetting("Invisibles", "Show invisible.", false));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Tracers through blocks.", true));
    private final ModeSetting colorMode = setting(new ModeSetting("Color Mode", "Tracer coloring.", "Friend", "Distance", "Health", "Friend", "Team", "Rainbow"));
    private final ModeSetting style = setting(new ModeSetting("Style", "Tracer origin.", "Head", "Head", "Feet", "Middle", "Crosshair"));

    public Tracers() {
        super("Tracers", "Prestige-style player tracers with multiple origins and color modes.");
    }

    @Override
    protected void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();
        Vec3d origin = getOrigin(client, camPos);
        double rangeSq = range.get() * range.get();

        List<TracerTarget> targets = new ArrayList<>();
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof LivingEntity) || entity == client.player || !entity.isAlive()) continue;
            if (!invisibles.enabled() && entity.isInvisible()) continue;
            if (client.player.squaredDistanceTo(entity) > rangeSq) continue;
            if (entity instanceof PlayerEntity && !players.enabled()) continue;
            boolean isHostile = !(entity instanceof PlayerEntity);
            if (isHostile && !hostiles.enabled()) continue;
            if (!(entity instanceof PlayerEntity) && !isHostile && !passives.enabled()) continue;
            if (!throughWalls.enabled() && !client.player.canSee(entity)) continue;

            Vec3d target = entity.getBoundingBox().getCenter().subtract(camPos);
            double dist = client.player.distanceTo(entity);
            int color = getColor(entity, dist, client);
            targets.add(new TracerTarget(target, color));
        }

        if (targets.isEmpty()) return;

        VertexConsumerProvider consumers = context.consumers();
        if (consumers == null) return;
        VertexConsumer buffer = consumers.getBuffer(RenderLayers.lines());
        MatrixStack.Entry entry = context.matrices().peek();
        float ox = (float) origin.x, oy = (float) origin.y, oz = (float) origin.z;

        for (TracerTarget t : targets) {
            float r = ((t.color >> 16) & 0xFF) / 255f;
            float g = ((t.color >> 8) & 0xFF) / 255f;
            float b = (t.color & 0xFF) / 255f;
            float a = ((t.color >> 24) & 0xFF) / 255f;
            buffer.vertex(entry, ox, oy, oz).color(r, g, b, a).normal(entry, 0, 1, 0).lineWidth(1.0f);
            buffer.vertex(entry, (float) t.pos.x, (float) t.pos.y, (float) t.pos.z).color(r, g, b, a).normal(entry, 0, 1, 0).lineWidth(1.0f);
        }
    }

    private Vec3d getOrigin(MinecraftClient client, Vec3d camPos) {
        return switch (style.get()) {
            case "Feet" -> new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ()).subtract(camPos);
            case "Middle" -> new Vec3d(client.player.getX(), client.player.getY() + client.player.getHeight() / 2, client.player.getZ()).subtract(camPos);
            case "Crosshair" -> {
                var hit = client.crosshairTarget;
                yield hit != null ? hit.getPos().subtract(camPos) : client.player.getEyePos().subtract(camPos);
            }
            default -> client.player.getEyePos().subtract(camPos);
        };
    }

    private int getColor(Entity entity, double distance, MinecraftClient client) {
        return switch (colorMode.get()) {
            case "Health" -> {
                if (entity instanceof LivingEntity le) {
                    float hp = le.getHealth() / le.getMaxHealth();
                    int r = (int) (255 * (1 - hp));
                    int g = (int) (255 * hp);
                    yield 0xFF000000 | (r << 16) | (g << 8);
                }
                yield 0xFFFFFFFF;
            }
            case "Friend" -> {
                if (entity instanceof PlayerEntity pe && dev.falaris.client.FalarisClient.getInstance().getFriendsManager().isFriend(pe.getName().getString()))
                    yield 0xFF00FF00;
                yield 0xFFFF5555;
            }
            case "Team" -> {
                if (entity instanceof PlayerEntity pe) {
                    var team = pe.getScoreboardTeam();
                    if (team != null && team.getColor() != null) {
                        var fc = team.getColor().getColorValue();
                        if (fc != null) yield 0xFF000000 | fc;
                    }
                }
                yield 0xFFFFFFFF;
            }
            case "Rainbow" -> {
                long time = System.currentTimeMillis() / 20;
                float hue = ((time + (long)(distance * 10)) % 360L) / 360f;
                int rgb = java.awt.Color.HSBtoRGB(hue, 0.8f, 0.95f);
                yield 0xFF000000 | (rgb & 0x00FFFFFF);
            }
            default -> {
                float factor = (float) MathHelper.clamp(distance / 50.0, 0.0, 1.0);
                int r = (int) (255 * factor);
                int g = (int) (255 * (1 - factor));
                yield 0xFF000000 | (r << 16) | (g << 8);
            }
        };
    }

    private record TracerTarget(Vec3d pos, int color) {}
}
