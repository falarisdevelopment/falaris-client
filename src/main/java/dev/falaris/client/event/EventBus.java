package dev.falaris.client.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class EventBus {
    private final Map<Class<?>, List<Subscriber<?>>> subscribers = new ConcurrentHashMap<>();

    public <T extends Event> Subscription subscribe(Class<T> eventType, EventPriority priority, Consumer<T> listener) {
        Subscriber<T> subscriber = new Subscriber<>(eventType, priority, listener);
        subscribers.computeIfAbsent(eventType, ignored -> new ArrayList<>()).add(subscriber);
        subscribers.get(eventType).sort((first, second) -> second.priority().compareTo(first.priority()));

        return () -> subscribers.computeIfPresent(eventType, (ignored, list) -> {
            list.remove(subscriber);
            return list.isEmpty() ? null : list;
        });
    }

    public void post(Event event) {
        List<Subscriber<?>> listeners = subscribers.get(event.getClass());
        if (listeners == null) {
            return;
        }

        for (Subscriber<?> subscriber : List.copyOf(listeners)) {
            subscriber.dispatch(event);
        }
    }

    private record Subscriber<T extends Event>(Class<T> type, EventPriority priority, Consumer<T> listener) {
        private void dispatch(Event event) {
            if (type.isInstance(event)) {
                listener.accept(type.cast(event));
            }
        }
    }
}
