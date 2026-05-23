package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class Tracers extends RenderModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum tracer range.", 128.0, 8.0, 256.0));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Trace to players.", true));
    private final BooleanSetting hostiles = setting(new BooleanSetting("Hostiles", "Trace to hostile mobs.", true));

    public Tracers() {
        super("Tracers", "Draws thin 3D tracer paths toward entities.");
    }

    @Override
    protected void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        Vec3d eyes = client.player.getEyePos();
        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof LivingEntity) || entity == client.player || client.player.squaredDistanceTo(entity) > range.get() * range.get()) {
                continue;
            }
            if (entity instanceof PlayerEntity && !players.enabled() || entity instanceof HostileEntity && !hostiles.enabled()) {
                continue;
            }

            drawSegmentedTracer(context, eyes, entity.getBoundingBox().getCenter(), entity instanceof PlayerEntity
                    ? RenderUtil.Color.rgba(122, 156, 255, 180)
                    : RenderUtil.Color.rgba(255, 92, 122, 180));
        }
    }

    private void drawSegmentedTracer(WorldRenderContext context, Vec3d start, Vec3d end, RenderUtil.Color color) {
        Vec3d delta = end.subtract(start);
        for (int i = 0; i < 20; i++) {
            Vec3d point = start.add(delta.multiply(i / 20.0));
            RenderUtil.drawBox(context, Box.of(point, 0.035, 0.035, 0.035), color);
        }
    }
}
