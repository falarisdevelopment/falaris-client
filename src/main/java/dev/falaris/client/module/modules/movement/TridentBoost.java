package dev.falaris.client.module.modules.movement;

import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class TridentBoost extends MovementModule {
    private final DoubleSetting speedBoost = setting(new DoubleSetting("Speed Boost", "Multiplier for riptide velocity.", 1.5, 0.5, 3.0));
    private final BooleanSetting onlyRiptide = setting(new BooleanSetting("Only Riptide", "Only boost when holding any trident.", false));

    public TridentBoost() {
        super("TridentBoost", "Boosts range and velocity of trident riptide launches.");
    }

    @Override
    protected void onMovementTick(MinecraftClient client) {
        if (client.player == null) return;

        if (!client.player.isUsingItem()) return;

        ItemStack mainHand = client.player.getMainHandStack();
        ItemStack offHand = client.player.getOffHandStack();
        boolean hasTrident = mainHand.isOf(Items.TRIDENT) || offHand.isOf(Items.TRIDENT);
        if (!hasTrident) return;

        if (!client.player.isTouchingWaterOrRain() && !client.player.isSubmergedInWater()) return;

        var vel = client.player.getVelocity();
        client.player.setVelocity(
            vel.x * speedBoost.get(),
            vel.y * speedBoost.get(),
            vel.z * speedBoost.get()
        );
    }
}
