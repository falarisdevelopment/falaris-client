package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public final class Fullbright extends RenderModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Brightness mode.", "Both", "Night Vision", "Gamma", "Both"));
    private final BooleanSetting keepPotion = setting(new BooleanSetting("Keep Potion", "Refresh client night vision effect.", true));
    private Double previousGamma;

    public Fullbright() {
        super("Fullbright", "Brightens dark areas client-side.");
    }

    @Override
    protected void onRenderTick(MinecraftClient client) {
        if (client.player == null) return;

        if (mode.is("Night Vision") || mode.is("Both")) {
            if (keepPotion.enabled()) {
                client.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 260, 0, false, false, false));
            }
        }
        if (mode.is("Gamma") || mode.is("Both")) {
            if (previousGamma == null) {
                previousGamma = client.options.getGamma().getValue();
            }
            client.options.getGamma().setValue(1.0);
        }
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        if (previousGamma != null) {
            client.options.getGamma().setValue(previousGamma);
            previousGamma = null;
        }
    }
}
