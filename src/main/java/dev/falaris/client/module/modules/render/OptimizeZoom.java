package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;

public final class OptimizeZoom extends RenderModule {
    private final DoubleSetting zoomFactor = setting(new DoubleSetting("Zoom Factor", "Zoom multiplier.", 4.0, 2.0, 20.0));
    private final DoubleSetting smoothness = setting(new DoubleSetting("Smoothness", "Zoom transition speed.", 0.25, 0.05, 1.0));
    private final IntegerSetting maxFov = setting(new IntegerSetting("Max FOV", "Maximum FOV to zoom from.", 90, 30, 180));

    private double currentFov;
    private double targetFov;
    private boolean zoomActive;

    public OptimizeZoom() {
        super("OptimizeZoom", "Smooth zoom with adjustable FOV.");
    }

    @Override
    protected void onRenderTick(MinecraftClient client) {
        if (client.player == null) return;

        boolean shouldZoom = client.options.sneakKey.isPressed();

        if (shouldZoom && !zoomActive) {
            zoomActive = true;
            targetFov = maxFov.get() / zoomFactor.get();
        } else if (!shouldZoom && zoomActive) {
            zoomActive = false;
            targetFov = maxFov.get();
        }

        if (zoomActive) {
            currentFov += (targetFov - currentFov) * smoothness.get();
        } else if (currentFov < maxFov.get() - 0.5) {
            currentFov += (maxFov.get() - currentFov) * smoothness.get();
        } else {
            currentFov = maxFov.get();
        }

        if (zoomActive || currentFov < maxFov.get() - 1.0) {
            client.options.getFov().setValue((int) currentFov);
        }
    }
}
