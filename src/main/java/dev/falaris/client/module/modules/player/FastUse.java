package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;

public final class FastUse extends PlayerModule {
    private final IntegerSetting speed = setting(new IntegerSetting("Speed", "Use speed multiplier in ticks.", 0, 0, 4));
    private final BooleanSetting fastPlace = setting(new BooleanSetting("Fast Place", "Remove block placement delay.", true));
    private final IntegerSetting itemUseTime = setting(new IntegerSetting("Item Use Time", "Ticks to cancel item use at.", 16, 0, 36));

    public FastUse() {
        super("FastUse", "Speeds up item usage like eating, blocking, and building.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null) return;

        // Fast item use (eating, potions, etc.)
        if (client.player.isUsingItem() && client.player.getItemUseTime() >= speed.get()) {
            client.player.stopUsingItem();
            client.interactionManager.stopUsingItem(client.player);
        }

        // Fast block placement - reset right click delay timer
        if (fastPlace.enabled()) {
            try {
                java.lang.reflect.Field cooldownField = MinecraftClient.class.getDeclaredField("itemUseCooldown");
                cooldownField.setAccessible(true);
                cooldownField.setInt(client, 0);
            } catch (Exception ignored) {
            }
        }
    }
}
