package dev.falaris.client.util;

import java.util.Random;

public final class SafeDelay {
    private final Random random = new Random();
    private int ticksUntilNextAction;

    public boolean ready(int minimumTicks, int jitterTicks) {
        if (ticksUntilNextAction > 0) {
            return false;
        }

        ticksUntilNextAction = minimumTicks + random.nextInt(Math.max(1, jitterTicks + 1));
        return true;
    }

    public void tick() {
        if (ticksUntilNextAction > 0) {
            ticksUntilNextAction--;
        }
    }

    public int getTicksUntilNextAction() {
        return ticksUntilNextAction;
    }
}
