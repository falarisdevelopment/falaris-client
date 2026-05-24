package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.DoubleSetting;
import net.minecraft.client.MinecraftClient;

public final class Hitboxes extends CombatModule {
    private static float currentExpansion = 0.0f;

    private final DoubleSetting expand = setting(new DoubleSetting("Expand", "Entity hitbox expansion.", 0.5, 0.0, 3.0));
    private final DoubleSetting reach = setting(new DoubleSetting("Reach", "Extended reach distance.", 3.5, 3.0, 6.0));

    public Hitboxes() {
        super("Hitboxes", "Extends entity hitboxes and reach.");
    }

    public static float getExpansion() {
        return currentExpansion;
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        currentExpansion = expand.get().floatValue();
        if (client.interactionManager == null || client.player == null) return;
        try {
            float val = reach.get().floatValue();
            var field = client.interactionManager.getClass().getDeclaredField("reachDistance");
            field.setAccessible(true);
            field.set(client.interactionManager, val);
        } catch (Exception ignored) {
            try {
                float val = reach.get().floatValue();
                var field = net.minecraft.client.network.ClientPlayerInteractionManager.class.getDeclaredField("field_3714");
                field.setAccessible(true);
                field.set(client.interactionManager, val);
            } catch (Exception ignored2) {}
        }
    }
}
