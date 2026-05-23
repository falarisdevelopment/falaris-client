package dev.falaris.client.event.events;

import dev.falaris.client.event.Event;
import net.minecraft.client.gui.DrawContext;

public record RenderHudEvent(DrawContext context, float tickDelta) implements Event {
}
