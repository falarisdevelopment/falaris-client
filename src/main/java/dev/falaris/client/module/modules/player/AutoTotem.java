package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;

public final class AutoTotem extends PlayerModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "When to equip a totem.", "Always", "Always", "Low Health", "Fall Risk"));
    private final DoubleSetting health = setting(new DoubleSetting("Health", "Health threshold for low-health mode.", 10.0, 1.0, 20.0));
    private final DoubleSetting fallDistance = setting(new DoubleSetting("Fall Distance", "Fall distance threshold for fall-risk mode.", 8.0, 2.0, 80.0));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks between inventory actions.", 2, 1, 20));
    private final IntegerSetting jitter = setting(new IntegerSetting("Jitter", "Random extra ticks between actions.", 2, 0, 10));
    private final BooleanSetting inventoryOnly = setting(new BooleanSetting("Inventory Only", "Only swap while an inventory container is available.", false));

    public AutoTotem() {
        super("AutoTotem", "Keeps a totem in the offhand based on health and fall risk.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null || InventoryUtil.isTotem(client.player.getOffHandStack()) || !shouldEquip(client)) {
            return;
        }
        if (inventoryOnly.enabled() && client.currentScreen == null) {
            return;
        }
        if (!ready(delay.get(), jitter.get())) {
            return;
        }

        int inventoryIndex = InventoryUtil.findItem(client.player, Items.TOTEM_OF_UNDYING, false);
        int screenSlot = InventoryUtil.screenSlotForInventoryIndex(inventoryIndex);
        InventoryUtil.clickMove(client, screenSlot, InventoryUtil.OFFHAND_SLOT);
    }

    private boolean shouldEquip(MinecraftClient client) {
        if (mode.is("Always")) {
            return true;
        }
        if (mode.is("Low Health")) {
            return client.player.getHealth() + client.player.getAbsorptionAmount() <= health.get();
        }
        return client.player.fallDistance >= fallDistance.get();
    }
}
