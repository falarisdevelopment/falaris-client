package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public final class AutoGap extends PlayerModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "When to eat.", "Health", "Always", "Health", "Hunger", "Both"));
    private final DoubleSetting health = setting(new DoubleSetting("Health", "Health threshold to eat at.", 12.0, 1.0, 20.0));
    private final IntegerSetting hunger = setting(new IntegerSetting("Hunger", "Hunger threshold to eat at.", 10, 1, 20));
    private final BooleanSetting preferEnchanted = setting(new BooleanSetting("Prefer Enchanted", "Use enchanted golden apples first.", true));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks between eating sessions.", 15, 1, 60));
    private final IntegerSetting jitter = setting(new IntegerSetting("Jitter", "Random extra ticks.", 5, 0, 20));
    private final BooleanSetting pauseOnAttack = setting(new BooleanSetting("Pause on Attack", "Don't start eating while attacking.", true));
    private final BooleanSetting pauseOnMove = setting(new BooleanSetting("Pause on Move", "Don't start eating while moving.", false));
    private final BooleanSetting stopOnFull = setting(new BooleanSetting("Stop on Full", "Stop eating when health is full.", true));
    private final BooleanSetting switchBack = setting(new BooleanSetting("Switch Back", "Return to previous slot after eating.", true));

    private int prevSlot = -1;
    private boolean wasEating;

    public AutoGap() {
        super("AutoGap", "Automatically eats golden apples when health or hunger is low.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null) return;

        // Handle ongoing eating
        if (client.player.isUsingItem()) {
            wasEating = true;
            if (stopOnFull.enabled() && client.player.getHealth() >= client.player.getMaxHealth()) {
                stopEating(client);
            } else if (pauseOnAttack.enabled() && client.options.attackKey.isPressed()) {
                stopEating(client);
            }
            return;
        }

        // If we just finished eating, switch back
        if (wasEating) {
            wasEating = false;
            switchBackToPrev(client);
            return;
        }

        // Check if we should eat
        if (!shouldEat(client)) return;

        // Check pause conditions
        if (pauseOnAttack.enabled() && client.options.attackKey.isPressed()) return;
        if (pauseOnMove.enabled() && (client.options.forwardKey.isPressed() || client.options.backKey.isPressed()
            || client.options.leftKey.isPressed() || client.options.rightKey.isPressed())) return;

        if (!ready(delay.get(), jitter.get())) return;

        // Find golden apple
        int slot = -1;
        if (preferEnchanted.enabled()) {
            slot = InventoryUtil.findItem(client.player, Items.ENCHANTED_GOLDEN_APPLE, true);
        }
        if (slot == -1) {
            slot = InventoryUtil.findItem(client.player, Items.GOLDEN_APPLE, true);
        }
        if (slot == -1) {
            slot = InventoryUtil.findItem(client.player, Items.ENCHANTED_GOLDEN_APPLE, false);
        }
        if (slot == -1) {
            slot = InventoryUtil.findItem(client.player, Items.GOLDEN_APPLE, false);
        }
        if (slot == -1) return;

        prevSlot = client.player.getInventory().getSelectedSlot();
        if (slot >= 0 && slot < 9) {
            client.player.getInventory().setSelectedSlot(slot);
        }
        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
    }

    private boolean shouldEat(MinecraftClient client) {
        return switch (mode.get()) {
            case "Always" -> true;
            case "Health" -> client.player.getHealth() <= health.get();
            case "Hunger" -> client.player.getHungerManager().getFoodLevel() <= hunger.get();
            case "Both" -> client.player.getHealth() <= health.get()
                || client.player.getHungerManager().getFoodLevel() <= hunger.get();
            default -> false;
        };
    }

    private void stopEating(MinecraftClient client) {
        client.player.stopUsingItem();
        client.interactionManager.stopUsingItem(client.player);
        wasEating = false;
        switchBackToPrev(client);
    }

    private void switchBackToPrev(MinecraftClient client) {
        if (switchBack.enabled() && prevSlot >= 0 && prevSlot < 9) {
            client.player.getInventory().setSelectedSlot(prevSlot);
        }
        prevSlot = -1;
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            if (client.player.isUsingItem()) {
                client.player.stopUsingItem();
                client.interactionManager.stopUsingItem(client.player);
            }
            if (prevSlot >= 0 && prevSlot < 9) {
                client.player.getInventory().setSelectedSlot(prevSlot);
            }
        }
        prevSlot = -1;
        wasEating = false;
    }
}
