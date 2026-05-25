package dev.falaris.client.tick;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.event.EventPriority;
import dev.falaris.client.event.events.ClientTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public final class TickScheduler {
    private static final TickScheduler INSTANCE = new TickScheduler();
    private final List<ScheduledTask> tasks = new ArrayList<>();
    private int currentTick;
    private boolean subscribed;

    public static TickScheduler getInstance() {
        return INSTANCE;
    }

    public void schedule(String owner, Consumer<Integer> action, int delayTicks) {
        tasks.add(new ScheduledTask(owner, action, currentTick + delayTicks, delayTicks, false));
        ensureSubscribed();
    }

    public void scheduleOnce(String owner, Runnable action, int delayTicks) {
        tasks.add(new ScheduledTask(owner, t -> action.run(), currentTick + delayTicks, 0, false));
        ensureSubscribed();
    }

    public void scheduleRepeating(String owner, Consumer<Integer> action, int intervalTicks) {
        tasks.add(new ScheduledTask(owner, action, currentTick + intervalTicks, intervalTicks, true));
        ensureSubscribed();
    }

    public void cancel(String owner) {
        tasks.removeIf(t -> t.owner.equals(owner));
        if (tasks.isEmpty() && subscribed) {
            unsubscribe();
        }
    }

    public int getCurrentTick() {
        return currentTick;
    }

    private void tick() {
        currentTick++;
        if (tasks.isEmpty()) return;
        Iterator<ScheduledTask> it = tasks.iterator();
        while (it.hasNext()) {
            ScheduledTask t = it.next();
            if (currentTick >= t.executeAt) {
                try {
                    t.action.accept(currentTick);
                } catch (Exception ignored) {}
                if (t.repeating && t.interval > 0) {
                    t.executeAt = currentTick + t.interval;
                } else {
                    it.remove();
                }
            }
        }
        if (tasks.isEmpty() && subscribed) {
            unsubscribe();
        }
    }

    private void ensureSubscribed() {
        if (subscribed) return;
        subscribed = true;
        FalarisClient.getInstance().getEventBus().subscribe(ClientTickEvent.class, EventPriority.HIGH, event -> tick());
    }

    private void unsubscribe() {
        subscribed = false;
    }

    private static class ScheduledTask {
        final String owner;
        final Consumer<Integer> action;
        int executeAt;
        final int interval;
        final boolean repeating;

        ScheduledTask(String owner, Consumer<Integer> action, int executeAt, int interval, boolean repeating) {
            this.owner = owner;
            this.action = action;
            this.executeAt = executeAt;
            this.interval = interval;
            this.repeating = repeating;
        }
    }
}
