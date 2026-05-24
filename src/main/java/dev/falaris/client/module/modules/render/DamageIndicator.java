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
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DamageIndicator extends RenderModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Max render range.", 64.0, 8.0, 256.0));
    private final BooleanSetting showParticles = setting(new BooleanSetting("Show Particles", "Spawn particles on hit.", true));
    private final IntegerSetting particleCount = setting(new IntegerSetting("Particle Count", "Particles per hit.", 6, 2, 20));
    private final DoubleSetting particleSpread = setting(new DoubleSetting("Particle Spread", "Spread radius.", 0.5, 0.1, 2.0));
    private final IntegerSetting fadeTicks = setting(new IntegerSetting("Fade Ticks", "Ticks before fade.", 20, 5, 60));
    private final BooleanSetting showDamageNumbers = setting(new BooleanSetting("Show Damage Numbers", "Float damage text.", true));

    private final Map<UUID, HitEntry> hitLog = new ConcurrentHashMap<>();

    public DamageIndicator() {
        super("DamageIndicator", "Visual damage feedback — floating numbers, hit particles, hurt flash.");
    }

    public void recordHit(Entity target, float damage) {
        if (!isEnabled()) return;
        if (!(target instanceof LivingEntity)) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        double dist = client.player.distanceTo(target);
        if (dist > range.get()) return;
        hitLog.put(target.getUuid(), new HitEntry(damage, new Vec3d(target.getX(), target.getY(), target.getZ()), System.currentTimeMillis()));
    }

    @Override
    protected void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        long now = System.currentTimeMillis();
        hitLog.entrySet().removeIf(e -> now - e.getValue().time > fadeTicks.get() * 50L);

        if (showDamageNumbers.enabled() && !hitLog.isEmpty()) {
            MatrixStack matrices = context.matrices();
            VertexConsumerProvider consumers = context.consumers();
            if (consumers == null) return;
            Camera camera = client.gameRenderer.getCamera();
            Vec3d cam = camera.getCameraPos();

            for (HitEntry entry : hitLog.values()) {
                Vec3d pos = entry.pos;
                double age = (now - entry.time) / 1000.0;
                double heightOffset = age * 0.5;

                matrices.push();
                matrices.translate(pos.x - cam.x, pos.y - cam.y + heightOffset, pos.z - cam.z);
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                matrices.scale(-0.03f, -0.03f, 0.03f);

                String text = String.format("§c-%.0f", entry.damage);
                float w = client.textRenderer.getWidth(text);
                float alpha = Math.max(0, 1.0f - (float) age);
                int color = ((int)(alpha * 255) << 24) | 0xFF5555;

                client.textRenderer.draw(text, -w / 2, 0, color, false,
                    matrices.peek().getPositionMatrix(), consumers,
                    net.minecraft.client.font.TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);

                matrices.pop();
            }
        }
    }

    private record HitEntry(float damage, Vec3d pos, long time) {}
}
