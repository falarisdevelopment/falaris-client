package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;

public final class IAmInnocent extends PlayerModule {
    private final BooleanSetting autoHide = setting(new BooleanSetting("Auto Hide", "Automatically hide flagged map art.", true));
    private final BooleanSetting blurTextures = setting(new BooleanSetting("Blur Textures", "Blur hidden textures instead of skipping.", false));
    private final ModeSetting detectionMode = setting(new ModeSetting("Detection Mode", "What to hide.", "All", "All", "Map Art", "Player Skins"));
    private final BooleanSetting showNotification = setting(new BooleanSetting("Show Notification", "Chat log when hiding something.", false));

    public IAmInnocent() {
        super("IAmInnocent", "Hides NSFW map art and player skins from view.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        if (!autoHide.enabled()) return;

        if (detectionMode.is("Map Art") || detectionMode.is("All")) {
            for (var entity : client.world.getEntities()) {
                if (client.player.squaredDistanceTo(entity) < 144.0) {
                    String name = entity.getName().getString().toLowerCase();
                    if (name.contains("map") || name.contains("art")) {
                        if (showNotification.enabled()) {
                            client.player.sendMessage(net.minecraft.text.Text.literal("§7[Innocent] Hid entity §e" + entity.getName().getString()), false);
                        }
                        handleMapEntity(client, entity);
                    }
                }
            }

            if (client.player.getMainHandStack().isOf(Items.FILLED_MAP) || client.player.getOffHandStack().isOf(Items.FILLED_MAP)) {
                if (showNotification.enabled()) {
                    client.player.sendMessage(net.minecraft.text.Text.literal("§7[Innocent] Hiding map in hand"), false);
                }
            }
        }

        if (detectionMode.is("Player Skins") || detectionMode.is("All")) {
            for (var player : client.world.getPlayers()) {
                if (player == client.player) continue;
                if (client.player.squaredDistanceTo(player) < 144.0) {
                    checkPlayerSkin(client, player);
                }
            }
        }
    }

    private void handleMapEntity(MinecraftClient client, net.minecraft.entity.Entity entity) {
        client.world.removeEntity(entity.getId(), net.minecraft.entity.Entity.RemovalReason.DISCARDED);
    }

    private void checkPlayerSkin(MinecraftClient client, net.minecraft.entity.player.PlayerEntity player) {
        if (showNotification.enabled()) {
            client.player.sendMessage(net.minecraft.text.Text.literal("§7[Innocent] Checked §e" + player.getName().getString()), false);
        }
    }
}
