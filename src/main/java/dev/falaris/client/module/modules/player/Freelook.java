package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;

public final class Freelook extends PlayerModule {
    private final BooleanSetting holdMode = setting(new BooleanSetting("Hold Mode", "Hold the freelook key while active.", false));
    private final DoubleSetting sensitivity = setting(new DoubleSetting("Sensitivity", "Freelook sensitivity.", 1.0, 0.1, 3.0));
    private final BooleanSetting resetOnDisable = setting(new BooleanSetting("Reset on Disable", "Return camera to body rotation.", true));

    private float cameraYaw, cameraPitch;
    private boolean active;
    private boolean wasActive;
    private double lastMouseX, lastMouseY;
    private boolean firstTick = true;

    public Freelook() {
        super("Freelook", "Detaches camera movement from player body rotation.");
    }

    @Override
    protected void onEnable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        cameraYaw = client.player.getYaw();
        cameraPitch = client.player.getPitch();
        firstTick = true;
        active = true;
        wasActive = true;
        client.options.setPerspective(Perspective.FIRST_PERSON);
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && wasActive && resetOnDisable.enabled()) {
            client.player.setYaw(cameraYaw);
            client.player.setPitch(cameraPitch);
        }
        wasActive = false;
        active = false;
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null || client.options == null) return;

        if (holdMode.enabled()) {
            active = isFreelookHeld(client);
        }

        client.options.setPerspective(Perspective.FIRST_PERSON);

        if (!active) {
            if (wasActive && resetOnDisable.enabled()) {
                cameraYaw = MathHelper.lerp(0.3f, cameraYaw, client.player.getYaw());
                cameraPitch = MathHelper.lerp(0.3f, cameraPitch, client.player.getPitch());
                if (Math.abs(cameraYaw - client.player.getYaw()) < 0.5f) {
                    cameraYaw = client.player.getYaw();
                    cameraPitch = client.player.getPitch();
                }
            } else {
                cameraYaw = client.player.getYaw();
                cameraPitch = client.player.getPitch();
            }
            wasActive = false;
        } else {
            wasActive = true;
            double mx = client.mouse.getX();
            double my = client.mouse.getY();
            if (firstTick) {
                lastMouseX = mx;
                lastMouseY = my;
                firstTick = false;
            }
            double dx = (mx - lastMouseX) * sensitivity.get() * 0.3;
            double dy = (my - lastMouseY) * sensitivity.get() * 0.3;
            lastMouseX = mx;
            lastMouseY = my;
            cameraYaw += (float) dx;
            cameraPitch = (float) MathHelper.clamp(cameraPitch - dy, -90.0, 90.0);
        }

        client.player.setYaw(cameraYaw);
        client.player.setPitch(cameraPitch);
        client.player.headYaw = cameraYaw;
        client.player.lastYaw = cameraYaw;
        client.player.lastPitch = cameraPitch;
    }

    private boolean isFreelookHeld(MinecraftClient client) {
        try {
            var key = client.options.sneakKey;
            return key != null && key.isPressed();
        } catch (Exception e) {
            return client.mouse.isCursorLocked();
        }
    }
}
