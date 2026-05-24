package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public final class Coordinates extends RenderModule {
    private final ModeSetting position = setting(new ModeSetting("Position", "Screen position.", "Top Left", "Top Left", "Top Right", "Bottom Left", "Bottom Right"));
    private final IntegerSetting offsetX = setting(new IntegerSetting("Offset X", "Horizontal offset.", 4, 0, 800));
    private final IntegerSetting offsetY = setting(new IntegerSetting("Offset Y", "Vertical offset.", 4, 0, 600));
    private final BooleanSetting showDirection = setting(new BooleanSetting("Show Direction", "Show cardinal direction.", true));
    private final BooleanSetting showNether = setting(new BooleanSetting("Show Nether", "Show nether coordinates overlay.", false));

    public Coordinates() {
        super("Coordinates", "Displays your current position, direction, and nether coordinates.");
    }

    @Override
    protected void onHudRender(DrawContext ctx, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();
        String dir = getDirection(client.player.getYaw());

        String coords = String.format("XYZ: %.0f / %.0f / %.0f", x, y, z);
        String nether;
        if (showNether.enabled() && !client.world.getRegistryKey().getValue().getPath().contains("nether")) {
            nether = String.format("Nether: %.0f / %.0f / %.0f", x / 8, y, z / 8);
        } else if (showNether.enabled() && client.world.getRegistryKey().getValue().getPath().contains("nether")) {
            nether = String.format("Overworld: %.0f / %.0f / %.0f", x * 8, y, z * 8);
        } else {
            nether = null;
        }

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        int ox = offsetX.get();
        int oy = offsetY.get();

        int xPos = switch (position.get()) {
            case "Top Right" -> sw - client.textRenderer.getWidth(coords) - ox - 4;
            case "Bottom Left" -> ox;
            case "Bottom Right" -> sw - client.textRenderer.getWidth(coords) - ox - 4;
            default -> ox;
        };
        int yPos = switch (position.get()) {
            case "Bottom Left" -> sh - oy - 30;
            case "Bottom Right" -> sh - oy - 30;
            default -> oy;
        };

        ctx.fill(xPos - 2, yPos - 2, xPos + client.textRenderer.getWidth(coords) + 4, yPos + 10, 0x60000000);
        ctx.drawText(client.textRenderer, coords, xPos, yPos, 0xFF88CCFF, true);

        if (showDirection.enabled()) {
            int dy = yPos + 12;
            ctx.fill(xPos - 2, dy - 2, xPos + client.textRenderer.getWidth(dir) + 4, dy + 10, 0x60000000);
            ctx.drawText(client.textRenderer, dir, xPos, dy, 0xFF88FFAA, true);
        }

        if (nether != null) {
            int ny = yPos + (showDirection.enabled() ? 24 : 12);
            ctx.fill(xPos - 2, ny - 2, xPos + client.textRenderer.getWidth(nether) + 4, ny + 10, 0x60000000);
            ctx.drawText(client.textRenderer, nether, xPos, ny, 0xFFFFAA66, true);
        }
    }

    private String getDirection(float yaw) {
        yaw = MathHelper.wrapDegrees(yaw);
        if (yaw > -22.5 && yaw <= 22.5) return "South §7(S+Z)";
        if (yaw > 22.5 && yaw <= 67.5) return "South West §7(SW)";
        if (yaw > 67.5 && yaw <= 112.5) return "West §7(W-X)";
        if (yaw > 112.5 && yaw <= 157.5) return "North West §7(NW)";
        if (yaw > 157.5 || yaw <= -157.5) return "North §7(N-Z)";
        if (yaw > -157.5 && yaw <= -112.5) return "North East §7(NE)";
        if (yaw > -112.5 && yaw <= -67.5) return "East §7(E+X)";
        if (yaw > -67.5 && yaw <= -22.5) return "South East §7(SE)";
        return "Unknown";
    }
}
