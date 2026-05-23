package dev.falaris.client.module.modules.render;

import dev.falaris.client.event.EventPriority;
import dev.falaris.client.event.events.ClientTickEvent;
import dev.falaris.client.event.events.RenderWorldEvent;
import dev.falaris.client.module.Category;
import dev.falaris.client.module.Module;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;

public abstract class RenderModule extends Module {
    protected RenderModule(String name, String description) {
        super(name, description, Category.RENDER);
    }

    @Override
    protected void onEnable() {
        track(eventBus().subscribe(ClientTickEvent.class, EventPriority.NORMAL, event -> onRenderTick(event.client())));
        track(eventBus().subscribe(RenderWorldEvent.class, EventPriority.NORMAL, event -> onWorldRender(event.context())));
    }

    protected void onRenderTick(MinecraftClient client) {
    }

    protected void onWorldRender(WorldRenderContext context) {
    }
}
