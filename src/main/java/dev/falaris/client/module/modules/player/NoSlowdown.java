package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;

public final class NoSlowdown extends PlayerModule {
    private static boolean active;

    private final ModeSetting mode = setting(new ModeSetting("Mode", "NoSlowdown mode.", "Vanilla", "Vanilla", "Legit", "Grim", "Vulcan"));
    private final DoubleSetting slowFactor = setting(new DoubleSetting("Slow Factor", "Movement speed while using items (Legit mode: 0.1-1.0).", 0.8, 0.1, 1.0));
    private final BooleanSetting onlySword = setting(new BooleanSetting("Only Sword", "Only apply when blocking with sword.", true));
    private final BooleanSetting consumeItems = setting(new BooleanSetting("Consume Items", "Apply when eating/drinking.", true));

    public NoSlowdown() {
        super("NoSlowdown", "Prevents slowdown while using items. Vanilla = full cancel, Legit = reduced slowdown.");
    }

    public static boolean shouldCancelSlowdown() {
        return active;
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null) return;
        active = isEnabled() && client.player.isUsingItem();

        if (!active) return;

        boolean isSwordBlock = client.player.getActiveItem().isOf(net.minecraft.item.Items.SHIELD)
            || isSwordItem(client.player.getActiveItem().getItem());
        boolean isEating = client.player.getActiveItem().contains(net.minecraft.component.DataComponentTypes.FOOD);

        if (onlySword.enabled() && !isSwordBlock) {
            active = false;
            return;
        }
        if (!consumeItems.enabled() && isEating) {
            active = false;
            return;
        }

        String m = mode.get();
        if (m.equals("Legit")) {
            // Mixin applies 5.0x counter in travel() head, so we undo part of it here
            float factor = slowFactor.get().floatValue();
            float restore = 1.0f - (1.0f - factor) * 0.8f;
            if (restore < 1.0f) {
                client.player.setVelocity(client.player.getVelocity().multiply(restore, 1.0, restore));
            }
        }
    }

    private boolean isSwordItem(net.minecraft.item.Item item) {
        return item == net.minecraft.item.Items.WOODEN_SWORD || item == net.minecraft.item.Items.STONE_SWORD
            || item == net.minecraft.item.Items.IRON_SWORD || item == net.minecraft.item.Items.GOLDEN_SWORD
            || item == net.minecraft.item.Items.DIAMOND_SWORD || item == net.minecraft.item.Items.NETHERITE_SWORD;
    }
}
