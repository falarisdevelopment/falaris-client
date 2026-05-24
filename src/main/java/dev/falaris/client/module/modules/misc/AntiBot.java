package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AntiBot extends MiscModule {
    private final BooleanSetting removeInvisible = setting(new BooleanSetting("Remove Invisible", "Remove invisible entities.", true));
    private final BooleanSetting removeNoName = setting(new BooleanSetting("Remove No Name", "Remove entities without nametag.", false));
    private final BooleanSetting removeDuplicates = setting(new BooleanSetting("Remove Duplicates", "Remove duplicate UUID entities.", true));
    private final BooleanSetting blockCombat = setting(new BooleanSetting("Block Combat Targeting", "Prevent bot targeting in combat modules.", true));
    private final ModeSetting detectionMode = setting(new ModeSetting("Detection Mode", "Bot detection method.", "Smart", "Smart", "Strict", "Prediction", "Off"));
    private final DoubleSetting predictionThreshold = setting(new DoubleSetting("Prediction Threshold", "Max position deviation before flagged.", 0.5, 0.1, 2.0));
    private final BooleanSetting namePatternCheck = setting(new BooleanSetting("Name Pattern Check", "Flag bots with auto-generated names.", true));
    private final BooleanSetting groundCheck = setting(new BooleanSetting("Ground Check", "Flag entities floating above blocks.", false));
    private final BooleanSetting duplicateCheck = setting(new BooleanSetting("Duplicate Check", "Flag exact position duplicates.", true));

    private final Set<UUID> knownPlayers = new HashSet<>();
    private final Map<UUID, Vec3d> previousPositions = new HashMap<>();
    private final Set<UUID> flaggedBots = new HashSet<>();
    private int tabRefreshTicks;

    public AntiBot() {
        super("AntiBot", "Filters bots from targeting and rendering using prediction, name patterns, tab list, and movement analysis.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        knownPlayers.clear();
        previousPositions.clear();
        flaggedBots.clear();
        tabRefreshTicks = 0;
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        if (detectionMode.is("Off")) return;

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof PlayerEntity player) || player == client.player) continue;

            boolean isBot = false;

            // Duplicate UUID check
            if (removeDuplicates.enabled()) {
                if (knownPlayers.contains(player.getUuid())) continue;
                knownPlayers.add(player.getUuid());
            }

            // Name pattern check
            if (namePatternCheck.enabled() && !isBot) {
                String name = player.getName().getString();
                if (isGeneratedName(name)) isBot = true;
            }

            // Prediction-based detection
            if (detectionMode.is("Prediction") && !isBot) {
                Vec3d prev = previousPositions.get(player.getUuid());
                Vec3d current = new Vec3d(player.getX(), player.getY(), player.getZ());
                if (prev != null) {
                    double expectedSpeed = 0.25;
                    double dist = prev.distanceTo(current);
                    if (dist > expectedSpeed + predictionThreshold.get()) isBot = true;
                }
                previousPositions.put(player.getUuid(), current);
            }

            // Invisible check
            if (removeInvisible.enabled() && player.isInvisible() && !isBot) isBot = true;

            // Strict: line of sight
            if (detectionMode.is("Strict") && !isBot) {
                if (!player.canSee(client.player)) isBot = true;
            }

            // Ground check
            if (groundCheck.enabled() && !isBot) {
                if (!player.isOnGround() && player.getVelocity().y == 0 && player.fallDistance == 0) isBot = true;
            }

            // Duplicate position check
            if (duplicateCheck.enabled() && !isBot) {
                for (Entity other : client.world.getEntities()) {
                    if (other == player || !(other instanceof PlayerEntity)) continue;
                    if (other.getX() == player.getX() && other.getY() == player.getY() && other.getZ() == player.getZ()
                        && !other.getUuid().equals(player.getUuid())) {
                        isBot = true;
                        break;
                    }
                }
            }

            if (isBot) {
                flaggedBots.add(player.getUuid());
                handleBot(client, player);
            }
        }
    }

    public boolean isBot(Entity entity) {
        if (detectionMode.is("Off")) return false;
        if (!blockCombat.enabled()) return false;
        if (!(entity instanceof PlayerEntity)) return false;
        if (flaggedBots.contains(entity.getUuid())) return true;
        if (removeInvisible.enabled() && entity.isInvisible()) return true;
        return false;
    }

    private boolean isGeneratedName(String name) {
        if (name.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) return true;
        if (name.length() >= 8 && name.matches("[a-zA-Z0-9]+") && !name.matches(".*[aeiouAEIOU].*")) return true;
        if (name.startsWith("Bot_") || name.startsWith("bot_") || name.endsWith("_NPC") || name.endsWith("_bot")) return true;
        if (name.matches(".*[0-9]{4,}")) return true;
        return false;
    }

    private void handleBot(MinecraftClient client, PlayerEntity bot) {
        if (removeInvisible.enabled() && bot.isInvisible()) {
            client.world.removeEntity(bot.getId(), Entity.RemovalReason.DISCARDED);
        }
    }
}
