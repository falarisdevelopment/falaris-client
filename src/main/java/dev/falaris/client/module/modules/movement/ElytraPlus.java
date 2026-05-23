package dev.falaris.client.module.modules.movement;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public final class ElytraPlus extends MovementModule {
    private final DoubleSetting boost = setting(new DoubleSetting("Boost", "Velocity multiplier while gliding.", 1.08, 1.0, 2.5));
    private final DoubleSetting maxSpeed = setting(new DoubleSetting("Max Speed", "Maximum horizontal speed.", 2.2, 0.2, 8.0));
    private final BooleanSetting autoFirework = setting(new BooleanSetting("Auto Firework", "Uses fireworks when speed is low.", false));
    private final DoubleSetting fireworkThreshold = setting(new DoubleSetting("Firework Threshold", "Speed below which a firework is used.", 0.8, 0.1, 3.0));
    private final IntegerSetting fireworkDelay = setting(new IntegerSetting("Firework Delay", "Ticks between firework uses.", 60, 5, 200));
    private final IntegerSetting fireworkJitter = setting(new IntegerSetting("Firework Jitter", "Random extra ticks between firework uses.", 20, 0, 100));

    public ElytraPlus() {
        super("ElytraPlus", "Quality-of-life elytra boosts and optional firework assist.");
    }

    @Override
    protected void onMovementTick(MinecraftClient client) {
        if (client.player == null || !client.player.isGliding()) {
            return;
        }

        Vec3d velocity = client.player.getVelocity();
        Vec3d horizontal = new Vec3d(velocity.x, 0.0, velocity.z).multiply(boost.get());
        if (horizontal.horizontalLength() > maxSpeed.get()) {
            horizontal = horizontal.normalize().multiply(maxSpeed.get());
        }
        client.player.setVelocity(horizontal.x, velocity.y, horizontal.z);
        client.player.fallDistance = 0.0f;

        if (!autoFirework.enabled() || horizontal.horizontalLength() > fireworkThreshold.get()) {
            return;
        }

        int slot = MovementUtil.findItem(client.player, Items.FIREWORK_ROCKET);
        if (slot != -1 && client.interactionManager != null && ready(fireworkDelay.get(), fireworkJitter.get())) {
            MovementUtil.selectSlot(client.player, slot);
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            client.player.swingHand(Hand.MAIN_HAND);
        }
    }
}
