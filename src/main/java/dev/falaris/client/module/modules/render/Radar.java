package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Radar extends RenderModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Detection radius.", 50.0, 10.0, 120.0));
    private final IntegerSetting size = setting(new IntegerSetting("Size", "Radar size in pixels.", 80, 40, 200));
    private final IntegerSetting offsetX = setting(new IntegerSetting("Offset X", "Horizontal position.", 5, 0, 800));
    private final IntegerSetting offsetY = setting(new IntegerSetting("Offset Y", "Vertical position.", 5, 0, 600));
    private final BooleanSetting showPlayers = setting(new BooleanSetting("Show Players", "Show player dots.", true));
    private final BooleanSetting showHostiles = setting(new BooleanSetting("Show Hostiles", "Show hostile entity dots.", false));
    private final BooleanSetting showSelf = setting(new BooleanSetting("Show Self", "Show own dot.", true));
    private final BooleanSetting background = setting(new BooleanSetting("Background", "Dark background.", true));
    private final BooleanSetting rotateRadar = setting(new BooleanSetting("Rotate Radar", "Rotate with player.", true));
    private final ModeSetting dotStyle = setting(new ModeSetting("Dot Style", "Dot shape.", "Circle", "Circle", "Square", "Triangle"));

    public Radar() {
        super("Radar", "Mini-radar showing nearby players on your HUD.");
    }

    @Override
    protected void onHudRender(DrawContext ctx, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        int r = size.get();
        int cx = offsetX.get() + r;
        int cy = offsetY.get() + r;

        if (background.enabled()) {
            ctx.fill(offsetX.get(), offsetY.get(), offsetX.get() + r * 2, offsetY.get() + r * 2, 0x80000000);
            ctx.fill(cx - 1, offsetY.get() + r - 1, cx + 1, offsetY.get() + r + 1, 0xFFFFFFFF);
            ctx.fill(offsetX.get() + r - 1, cy - 1, offsetX.get() + r + 1, cy + 1, 0xFFFFFFFF);
        }

        if (showSelf.enabled()) {
            drawDot(ctx, cx, cy, 0xFF00FF00, 3);
        }

        double px = client.player.getX();
        double pz = client.player.getZ();
        float playerYaw = rotateRadar.enabled() ? client.player.getYaw() : 0;

        List<PlayerEntity> players = new ArrayList<>(client.world.getPlayers());
        players.sort(Comparator.comparingDouble(p -> client.player.distanceTo(p)));

        for (PlayerEntity player : players) {
            if (player == client.player || !player.isAlive()) continue;
            double dist = client.player.distanceTo(player);
            if (dist > range.get()) continue;
            if (!showPlayers.enabled()) continue;

            double dx = player.getX() - px;
            double dz = player.getZ() - pz;

            if (rotateRadar.enabled()) {
                float rad = (float) Math.toRadians(playerYaw);
                double cos = Math.cos(rad);
                double sin = Math.sin(rad);
                double ndx = dx * cos + dz * sin;
                double ndz = -dx * sin + dz * cos;
                dx = ndx;
                dz = ndz;
            }

            double scale = r / range.get();
            int dotX = cx + (int) MathHelper.clamp(dx * scale, -r + 3, r - 3);
            int dotY = cy + (int) MathHelper.clamp(dz * scale, -r + 3, r - 3);

            boolean isFriend = dev.falaris.client.FalarisClient.getInstance().getFriendsManager().isFriend(player.getName().getString());
            int color = isFriend ? 0xFF00FF00 : 0xFFFF0000;
            drawDot(ctx, dotX, dotY, color, 2);
        }
    }

    private void drawDot(DrawContext ctx, int x, int y, int color, int radius) {
        switch (dotStyle.get()) {
            case "Circle" -> {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        if (dx * dx + dy * dy <= radius * radius) {
                            ctx.fill(x + dx, y + dy, x + dx + 1, y + dy + 1, color);
                        }
                    }
                }
            }
            case "Square" -> ctx.fill(x - radius, y - radius, x + radius + 1, y + radius + 1, color);
            case "Triangle" -> {
                ctx.fill(x, y - radius, x + 1, y - radius + 1, color);
                ctx.fill(x - radius, y, x + radius + 1, y + 1, color);
                ctx.fill(x - radius / 2, y, x + radius / 2 + 1, y + radius + 1, color);
            }
        }
    }
}
