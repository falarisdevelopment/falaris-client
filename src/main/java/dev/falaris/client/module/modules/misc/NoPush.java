package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;

public final class NoPush extends MiscModule {
    private final BooleanSetting entityPush = setting(new BooleanSetting("Entity Push", "Prevent entities from pushing you.", true));
    private final BooleanSetting waterPush = setting(new BooleanSetting("Water Push", "Prevent water pushing.", true));
    private final BooleanSetting blockPush = setting(new BooleanSetting("Block Push", "Prevent block pushing (pistons).", true));

    public NoPush() {
        super("NoPush", "Prevents being pushed by entities, water, and blocks.");
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.player == null) return;

        if (entityPush.enabled()) {
            client.player.setVelocity(client.player.getVelocity().multiply(1.0, 0.0, 1.0));
        }

        if (waterPush.enabled() && client.player.isTouchingWater()) {
            var vel = client.player.getVelocity();
            client.player.setVelocity(vel.x * 0.9, vel.y, vel.z * 0.9);
        }
    }
}
