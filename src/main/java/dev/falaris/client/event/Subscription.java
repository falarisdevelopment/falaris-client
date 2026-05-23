package dev.falaris.client.event;

@FunctionalInterface
public interface Subscription {
    void unsubscribe();
}
