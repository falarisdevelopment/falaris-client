package dev.falaris.client.gui.click;

import net.minecraft.util.math.MathHelper;

public final class Animation {
    private float value;
    private float target;

    public Animation(float initialValue) {
        this.value = initialValue;
        this.target = initialValue;
    }

    public void setTarget(float target) {
        this.target = target;
    }

    public void tick(float speed) {
        value = MathHelper.lerp(MathHelper.clamp(speed, 0.0f, 1.0f), value, target);
    }

    public float get() {
        return value;
    }
}
