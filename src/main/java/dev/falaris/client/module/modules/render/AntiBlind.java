package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;

public final class AntiBlind extends RenderModule {
    private final BooleanSetting blind = setting(new BooleanSetting("Blindness", "Remove blindness effect.", true));
    private final BooleanSetting nausea = setting(new BooleanSetting("Nausea", "Remove nausea effect.", true));
    private final BooleanSetting darkness = setting(new BooleanSetting("Darkness", "Remove darkness effect.", true));
    private final BooleanSetting fire = setting(new BooleanSetting("Fire", "Remove fire overlay.", true));

    public AntiBlind() {
        super("AntiBlind", "Removes blindness, nausea, and darkness effects.");
    }

    @Override
    protected void onRenderTick(MinecraftClient client) {
        if (client.player == null) return;

        if (blind.enabled() && client.player.hasStatusEffect(StatusEffects.BLINDNESS)) {
            client.player.removeStatusEffect(StatusEffects.BLINDNESS);
        }
        if (nausea.enabled() && client.player.hasStatusEffect(StatusEffects.NAUSEA)) {
            client.player.removeStatusEffect(StatusEffects.NAUSEA);
        }
        if (darkness.enabled() && client.player.hasStatusEffect(StatusEffects.DARKNESS)) {
            client.player.removeStatusEffect(StatusEffects.DARKNESS);
        }
    }
}
