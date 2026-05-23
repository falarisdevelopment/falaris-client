package dev.falaris.client.event.events;

import dev.falaris.client.event.Event;
import net.minecraft.client.MinecraftClient;

public record ClientTickEvent(MinecraftClient client) implements Event {
}
