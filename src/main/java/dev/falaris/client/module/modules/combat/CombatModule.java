package dev.falaris.client.module.modules.combat;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.event.EventPriority;
import dev.falaris.client.event.events.ClientTickEvent;
import dev.falaris.client.module.Category;
import dev.falaris.client.module.Module;
import dev.falaris.client.rotation.RotationManager;
import dev.falaris.client.util.SafeDelay;
import net.minecraft.client.MinecraftClient;

public abstract class CombatModule extends Module {
    private final SafeDelay actionDelay = new SafeDelay();

    protected CombatModule(String name, String description) {
        super(name, description, Category.COMBAT);
    }

    @Override
    protected void onEnable() {
        track(eventBus().subscribe(ClientTickEvent.class, EventPriority.NORMAL, event -> {
            actionDelay.tick();
            onCombatTick(event.client());
        }));
    }

    protected abstract void onCombatTick(MinecraftClient client);

    protected final boolean actionReady(int minimumTicks, int jitterTicks) {
        return actionDelay.ready(minimumTicks, jitterTicks);
    }

    protected final RotationManager rotations() {
        return FalarisClient.getInstance().getRotationManager();
    }
}
