package dev.falaris.client.module.modules.misc;

import dev.falaris.client.event.EventPriority;
import dev.falaris.client.event.events.ClientTickEvent;
import dev.falaris.client.module.Category;
import dev.falaris.client.module.Module;
import dev.falaris.client.util.SafeDelay;
import net.minecraft.client.MinecraftClient;

public abstract class MiscModule extends Module {
    private final SafeDelay delay = new SafeDelay();

    protected MiscModule(String name, String description) {
        super(name, description, Category.MISC);
    }

    @Override
    protected void onEnable() {
        track(eventBus().subscribe(ClientTickEvent.class, EventPriority.NORMAL, event -> {
            delay.tick();
            onMiscTick(event.client());
        }));
    }

    protected abstract void onMiscTick(MinecraftClient client);

    protected final boolean ready(int minimumTicks, int jitterTicks) {
        return delay.ready(minimumTicks, jitterTicks);
    }
}
