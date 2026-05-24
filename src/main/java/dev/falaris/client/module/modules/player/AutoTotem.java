package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;

import java.util.Random;

public final class AutoTotem extends PlayerModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "When to equip a totem.", "Always", "Always", "Low Health", "Fall Risk", "Hover", "Dynamic"));
    private final DoubleSetting health = setting(new DoubleSetting("Health", "Health threshold for low-health mode.", 10.0, 1.0, 20.0));
    private final DoubleSetting fallDistance = setting(new DoubleSetting("Fall Distance", "Fall distance threshold for fall-risk mode.", 8.0, 2.0, 80.0));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Min ticks between inventory actions.", 4, 1, 30));
    private final IntegerSetting jitter = setting(new IntegerSetting("Jitter", "Random extra ticks between actions.", 6, 0, 20));
    private final BooleanSetting inventoryOnly = setting(new BooleanSetting("Inventory Only", "Only swap while an inventory container is available.", false));
    private final BooleanSetting doubleHand = setting(new BooleanSetting("Double Hand", "Also place a totem in the main hand if offhand is already filled.", false));
    private final BooleanSetting dynamicDelay = setting(new BooleanSetting("Dynamic Delay", "Faster totem when low health, slower when safe.", true));
    private final BooleanSetting legitMouse = setting(new BooleanSetting("Legit Mouse", "Simulate mouse movements for screenshare safety.", false));
    private final IntegerSetting maxActionsPerOpen = setting(new IntegerSetting("Max Actions", "Max totem swaps per inventory open.", 3, 1, 10));

    private final Random random = new Random();
    private int actionsThisOpen;
    private boolean skipNext;

    public AutoTotem() {
        super("AutoTotem", "Prestige-style auto totem with dynamic danger-based delay.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        actionsThisOpen = 0;
        skipNext = false;
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null) return;

        if (mode.is("Hover")) {
            handleHover(client);
            return;
        }

        if (dynamicDelay.enabled() && mode.is("Dynamic")) {
            float hp = client.player.getHealth() + client.player.getAbsorptionAmount();
            float max = client.player.getMaxHealth();
            float ratio = hp / max;
            int dynamicDelayTicks;
            if (ratio < 0.3) {
                dynamicDelayTicks = Math.max(1, delay.get() / 2);
            } else if (ratio < 0.6) {
                dynamicDelayTicks = delay.get();
            } else {
                dynamicDelayTicks = delay.get() + jitter.get() + random.nextInt(5);
            }
            if (!ready(dynamicDelayTicks, 0)) return;
        } else {
            if (!ready(delay.get(), jitter.get())) return;
        }

        if (skipNext) {
            skipNext = false;
            return;
        }

        if (inventoryOnly.enabled() && client.currentScreen == null) return;

        boolean offhandHasTotem = InventoryUtil.isTotem(client.player.getOffHandStack());
        boolean mainHandHasTotem = InventoryUtil.isTotem(client.player.getMainHandStack());

        if (!shouldEquip(client)) return;

        if (!offhandHasTotem) {
            int inventoryIndex = InventoryUtil.findItem(client.player, Items.TOTEM_OF_UNDYING, false);
            if (inventoryIndex != -1) {
                int screenSlot = InventoryUtil.screenSlotForInventoryIndex(inventoryIndex);
                if (client.currentScreen != null && actionsThisOpen >= maxActionsPerOpen.get()) return;
                InventoryUtil.clickMove(client, screenSlot, InventoryUtil.OFFHAND_SLOT);
                if (client.currentScreen != null) actionsThisOpen++;
                if (random.nextFloat() < 0.3f) skipNext = true;
                return;
            }
        }

        if (doubleHand.enabled() && offhandHasTotem && !mainHandHasTotem) {
            int inventoryIndex = InventoryUtil.findItem(client.player, Items.TOTEM_OF_UNDYING, false);
            if (inventoryIndex != -1) {
                int screenSlot = InventoryUtil.screenSlotForInventoryIndex(inventoryIndex);
                int mainHandSlot = 36 + client.player.getInventory().getSelectedSlot();
                if (client.currentScreen != null && actionsThisOpen >= maxActionsPerOpen.get()) return;
                InventoryUtil.clickMove(client, screenSlot, mainHandSlot);
                if (client.currentScreen != null) actionsThisOpen++;
            }
        }
    }

    private void handleHover(MinecraftClient client) {
        if (!(client.currentScreen instanceof HandledScreen<?> screen)) return;
        if (!ready(delay.get(), jitter.get())) return;

        Slot hoveredSlot = null;
        try {
            java.lang.reflect.Field field = HandledScreen.class.getDeclaredField("hoveredSlot");
            field.setAccessible(true);
            hoveredSlot = (Slot) field.get(screen);
        } catch (Exception ignored) {}

        if (hoveredSlot == null || !hoveredSlot.hasStack()) return;
        if (!InventoryUtil.isTotem(hoveredSlot.getStack())) return;

        boolean offhandHasTotem = InventoryUtil.isTotem(client.player.getOffHandStack());
        if (!offhandHasTotem) {
            InventoryUtil.clickMove(client, hoveredSlot.id, InventoryUtil.OFFHAND_SLOT);
        }
    }

    private boolean shouldEquip(MinecraftClient client) {
        return switch (mode.get()) {
            case "Always", "Dynamic" -> true;
            case "Low Health" -> client.player.getHealth() + client.player.getAbsorptionAmount() <= health.get();
            case "Fall Risk" -> client.player.fallDistance >= fallDistance.get();
            default -> true;
        };
    }
}
