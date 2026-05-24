package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public final class AutoGap extends PlayerModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "When to eat.", "Low Health", "Always", "Low Health"));
    private final DoubleSetting health = setting(new DoubleSetting("Health", "Health threshold to eat at.", 10.0, 1.0, 20.0));
    private final BooleanSetting preferEnchanted = setting(new BooleanSetting("Prefer Enchanted", "Use enchanted golden apples first.", true));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks between actions.", 10, 1, 40));
    private final IntegerSetting jitter = setting(new IntegerSetting("Jitter", "Random extra ticks.", 5, 0, 20));

    private int prevSlot = -1;
    private boolean startingUse;

    public AutoGap() {
        super("AutoGap", "Auto-eats golden apples when health is low.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null) return;

        if (mode.is("Low Health") && client.player.getHealth() > health.get()) return;

        if (client.player.isUsingItem()) return;
        if (!ready(delay.get(), jitter.get())) return;

        // Reset item use cooldown via reflection
        try {
            java.lang.reflect.Field cooldownField = MinecraftClient.class.getDeclaredField("itemUseCooldown");
            cooldownField.setAccessible(true);
            cooldownField.setInt(client, 0);
        } catch (Exception ignored) {
        }

        int slot = -1;
        if (preferEnchanted.enabled()) {
            slot = InventoryUtil.findItem(client.player, Items.ENCHANTED_GOLDEN_APPLE, true);
        }
        if (slot == -1) {
            slot = InventoryUtil.findItem(client.player, Items.GOLDEN_APPLE, true);
        }
        if (slot == -1) return;

        prevSlot = client.player.getInventory().getSelectedSlot();
        client.player.getInventory().setSelectedSlot(slot);
        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        startingUse = true;
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            if (client.player.isUsingItem()) {
                client.player.stopUsingItem();
                client.interactionManager.stopUsingItem(client.player);
            }
            if (prevSlot != -1) {
                client.player.getInventory().setSelectedSlot(prevSlot);
            }
        }
        prevSlot = -1;
        startingUse = false;
    }
}
