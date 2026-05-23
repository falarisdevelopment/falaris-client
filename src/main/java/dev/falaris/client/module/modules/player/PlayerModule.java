package dev.falaris.client.module.modules.player;

import dev.falaris.client.event.EventPriority;
import dev.falaris.client.event.events.ClientTickEvent;
import dev.falaris.client.module.Category;
import dev.falaris.client.module.Module;
import dev.falaris.client.util.SafeDelay;
import net.minecraft.client.MinecraftClient;

public abstract class PlayerModule extends Module {
    private final SafeDelay delay = new SafeDelay();

    protected PlayerModule(String name, String description) {
        super(name, description, Category.PLAYER);
    }

    @Override
    protected void onEnable() {
        track(eventBus().subscribe(ClientTickEvent.class, EventPriority.NORMAL, event -> {
            delay.tick();
            onPlayerTick(event.client());
        }));
    }

    protected abstract void onPlayerTick(MinecraftClient client);

    protected final boolean ready(int minimumTicks, int jitterTicks) {
        return delay.ready(minimumTicks, jitterTicks);
    }
}
