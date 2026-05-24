package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import net.minecraft.client.MinecraftClient;

public final class NoRender extends RenderModule {
    private final BooleanSetting noHurtCam = setting(new BooleanSetting("No Hurt Cam", "Disables the hurt camera tilt.", true));
    private final BooleanSetting noWeather = setting(new BooleanSetting("No Weather", "Hides rain and snow.", true));
    private final BooleanSetting noParticles = setting(new BooleanSetting("No Particles", "Hides block break particles.", false));
    private final BooleanSetting noFireOverlay = setting(new BooleanSetting("No Fire", "Hides fire overlay on screen.", true));
    private final BooleanSetting noWaterOverlay = setting(new BooleanSetting("No Water", "Hides water overlay.", false));
    private final BooleanSetting noBossBar = setting(new BooleanSetting("No Boss Bar", "Hides boss bars.", false));
    private final BooleanSetting noScoreboard = setting(new BooleanSetting("No Scoreboard", "Hides scoreboard.", false));
    private final BooleanSetting noPotionIcons = setting(new BooleanSetting("No Potion Icons", "Hides potion effect icons.", false));
    private final BooleanSetting noArmor = setting(new BooleanSetting("No Armor", "Hides armor rendering on players.", false));

    public NoRender() {
        super("NoRender", "Disables various render elements client-side.");
    }

    @Override
    protected void onRenderTick(MinecraftClient client) {
        if (client.player == null) return;

        if (noWeather.enabled() && client.world != null) {
            client.world.setRainGradient(0);
            client.world.setThunderGradient(0);
        }
    }

    public boolean shouldRemoveHurtCam() { return isEnabled() && noHurtCam.enabled(); }
    public boolean shouldRemoveWeather() { return isEnabled() && noWeather.enabled(); }
    public boolean shouldHideFire() { return isEnabled() && noFireOverlay.enabled(); }
    public boolean shouldHideWater() { return isEnabled() && noWaterOverlay.enabled(); }
    public boolean shouldHideBossBar() { return isEnabled() && noBossBar.enabled(); }
    public boolean shouldHideScoreboard() { return isEnabled() && noScoreboard.enabled(); }
    public boolean shouldHidePotionIcons() { return isEnabled() && noPotionIcons.enabled(); }
    public boolean shouldHideArmor() { return isEnabled() && noArmor.enabled(); }
    public boolean shouldHideParticles() { return isEnabled() && noParticles.enabled(); }
}
