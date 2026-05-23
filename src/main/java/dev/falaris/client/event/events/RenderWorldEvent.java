package dev.falaris.client.event.events;

import dev.falaris.client.event.Event;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;

public record RenderWorldEvent(WorldRenderContext context) implements Event {
}
