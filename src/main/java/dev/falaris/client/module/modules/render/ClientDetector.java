package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

public final class ClientDetector extends RenderModule {
    private final ModeSetting displayMode = setting(new ModeSetting("Display", "What to show next to nametag.", "Icon", "Icon", "Name", "Both"));
    private final BooleanSetting showSelf = setting(new BooleanSetting("Show Self", "Show your own detected client.", false));
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Detection and render range.", 64.0, 8.0, 256.0));
    private final BooleanSetting colors = setting(new BooleanSetting("Colors", "Use colored client names/icons.", true));
    private final BooleanSetting cacheResults = setting(new BooleanSetting("Cache Results", "Cache detection results per player.", true));

    private final Map<String, DetectedClient> clientCache = new HashMap<>();
    private int cacheTick;

    public ClientDetector() {
        super("ClientDetector", "Detects other players' clients via visual indicators and shows icons next to nametags.");
    }

    @Override
    protected void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.getNetworkHandler() == null) return;

        double rangeSq = range.get() * range.get();

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof PlayerEntity player)) continue;
            if (player == client.player && !showSelf.enabled()) continue;
            if (client.player.squaredDistanceTo(player) > rangeSq) continue;

            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
            if (entry == null) continue;

            DetectedClient dc = detectClient(player, entry);
            String label = formatLabel(dc);
            if (label.isEmpty()) continue;

            drawClientLabel(context, player, label, colors.enabled() ? dc.color : 0xFFFFFFFF);
        }
    }

    private DetectedClient detectClient(PlayerEntity player, PlayerListEntry entry) {
        String uuid = player.getUuidAsString();
        if (cacheResults.enabled() && clientCache.containsKey(uuid)) {
            return clientCache.get(uuid);
        }

        DetectedClient result = heuristicDetect(entry);

        if (cacheResults.enabled()) {
            clientCache.put(uuid, result);
        }
        return result;
    }

    private DetectedClient heuristicDetect(PlayerListEntry entry) {
        SkinTextures skin = entry.getSkinTextures();
        boolean hasCape = skin.cape() != null;
        PlayerSkinType modelType = skin.model();
        boolean slim = modelType == PlayerSkinType.SLIM;

        if (hasCape) {
            String capePath = skin.cape().texturePath().getPath();

            if (capePath.contains("lunar")) {
                return new DetectedClient("Lunar", "\uD83C\uDF19", 0x44B3FF);
            }
            if (capePath.contains("badlion")) {
                return new DetectedClient("Badlion", "\uD83E\uDD81", 0xFF6B35);
            }
            if (capePath.contains("feather")) {
                return new DetectedClient("Feather", "\uD83E\uDEB8", 0x8B5CF6);
            }
            if (capePath.contains("labymod")) {
                return new DetectedClient("LabyMod", "\uD83D\uDD2C", 0x3B82F6);
            }
            if (capePath.contains("optifine")) {
                return new DetectedClient("OptiFine", "\u26A1", 0xFBBF24);
            }
            if (capePath.contains("minecraftcapes")) {
                return new DetectedClient("MC Capes", "\uD83C\uDF1F", 0xEC4899);
            }

            return new DetectedClient("Cape", "\uD83D\uDC54", 0x10B981);
        }

        if (slim) {
            return new DetectedClient("Slim", "\uD83D\uDC64", 0xA78BFA);
        }

        return new DetectedClient("Vanilla", "\uD83E\uDDCA", 0x9CA3AF);
    }

    private String formatLabel(DetectedClient dc) {
        return switch (displayMode.get()) {
            case "Icon" -> dc.icon;
            case "Name" -> dc.name;
            case "Both" -> dc.icon + " " + dc.name;
            default -> dc.icon;
        };
    }

    private void drawClientLabel(WorldRenderContext context, PlayerEntity player, String label, int color) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        MatrixStack matrices = context.matrices();

        Vec3d cam = client.gameRenderer.getCamera().getCameraPos();
        double x = player.getX() - cam.x;
        double y = player.getY() - cam.y + player.getHeight() + 0.7;
        double z = player.getZ() - cam.z;

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-client.gameRenderer.getCamera().getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(client.gameRenderer.getCamera().getPitch()));
        matrices.scale(-0.025f, -0.025f, 0.025f);

        float fw = textRenderer.getWidth(label);
        textRenderer.draw(label, -fw / 2, 0, color, false, matrices.peek().getPositionMatrix(), context.consumers(),
            TextRenderer.TextLayerType.SEE_THROUGH, 0x00000000, 0xF000F0);

        matrices.pop();
    }

    @Override
    protected void onRenderTick(MinecraftClient client) {
        cacheTick++;
        if (cacheTick > 100) {
            cacheTick = 0;
            if (cacheResults.enabled() && !clientCache.isEmpty()) {
                clientCache.clear();
            }
        }
    }

    private record DetectedClient(String name, String icon, int color) {}
}
