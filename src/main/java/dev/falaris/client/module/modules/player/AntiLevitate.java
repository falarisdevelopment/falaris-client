package dev.falaris.client.module.modules.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;

public final class AntiLevitate extends PlayerModule {
    public AntiLevitate() {
        super("AntiLevitate", "Removes shulker levitation in end cities.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null) return;
        client.player.removeStatusEffect(StatusEffects.LEVITATION);
    }
}
