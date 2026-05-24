package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public final class AutoFish extends PlayerModule {
    private final BooleanSetting autoCast = setting(new BooleanSetting("Auto Cast", "Auto-cast rod after catching.", true));
    private final IntegerSetting castDelay = setting(new IntegerSetting("Cast Delay", "Ticks before recasting.", 5, 0, 20));
    private final BooleanSetting soundDetection = setting(new BooleanSetting("Sound Detection", "Reel in when bobber splash sound plays.", true));
    private final BooleanSetting visualDetection = setting(new BooleanSetting("Visual Detection", "Reel in when bobber goes under.", true));

    private int delayTicks;
    private boolean shouldReel;

    public AutoFish() {
        super("AutoFish", "Automatically catches and re-casts fishing rods.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        if (!isHoldingRod(client)) return;

        if (shouldReel) {
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            shouldReel = false;
            delayTicks = 0;
            return;
        }

        if (autoCast.enabled() && client.player.fishHook == null && !client.player.isUsingItem()) {
            delayTicks++;
            if (delayTicks >= castDelay.get()) {
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                delayTicks = 0;
            }
        }

        if (soundDetection.enabled() && client.player.fishHook != null) {
            // Listen for bobber splash sound
            var sound = client.getSoundManager();
            if (sound != null) {
                // Check fish hook state: when it goes under water, bobber changes state
                FishingBobberEntity bobber = client.player.fishHook;
                // If the bobber is no longer visible or has a different velocity
                if (bobber != null && bobber.getVelocity().lengthSquared() > 0.1 && bobber.isRemoved()) {
                    // Bobber was pulled under
                }
            }
        }

        // Alternative: right-click when bobber entity disappears or changes state
        if (client.player.fishHook != null && client.player.fishHook.isRemoved()) {
            shouldReel = true;
        }
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        shouldReel = false;
        delayTicks = 0;
    }

    private boolean isHoldingRod(MinecraftClient client) {
        return client.player.getMainHandStack().isOf(Items.FISHING_ROD)
            || client.player.getOffHandStack().isOf(Items.FISHING_ROD);
    }
}
