package dev.falaris.client.notification;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.event.EventPriority;
import dev.falaris.client.event.events.RenderHudEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class NotificationManager {
    private static final NotificationManager INSTANCE = new NotificationManager();
    private final List<Notification> active = new ArrayList<>();
    private boolean subscribed;

    public static NotificationManager getInstance() {
        return INSTANCE;
    }

    public void show(String title, String subtitle, int durationMs) {
        active.add(new Notification(title, subtitle, System.currentTimeMillis(), durationMs));
        ensureSubscribed();
    }

    public void toggle(String moduleName, boolean on) {
        show(moduleName, on ? "ON" : "OFF", 2000);
    }

    public void tick() {
        Iterator<Notification> it = active.iterator();
        long now = System.currentTimeMillis();
        while (it.hasNext()) {
            Notification n = it.next();
            long elapsed = now - n.startTime;
            if (elapsed > n.duration + 500) {
                it.remove();
            }
        }
        if (active.isEmpty() && subscribed) {
            unsubscribe();
        }
    }

    private void ensureSubscribed() {
        if (subscribed) return;
        subscribed = true;
        FalarisClient.getInstance().getEventBus().subscribe(RenderHudEvent.class, EventPriority.LOW, event -> {
            render(event.context());
            tick();
        });
    }

    private void unsubscribe() {
        subscribed = false;
    }

    private void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        var module = FalarisClient.getInstance().getModuleManager().find("notifications");
        if (module.isEmpty() || !module.get().isEnabled()) return;
        TextRenderer tr = client.textRenderer;
        if (tr == null) return;
        long now = System.currentTimeMillis();
        int scw = client.getWindow().getScaledWidth();
        int y = 4;

        for (Notification n : active) {
            long elapsed = now - n.startTime;
            float progress = Math.min(1.0f, (float) elapsed / n.duration);
            int alpha;
            int offsetX;
            if (elapsed < 200) {
                float f = elapsed / 200f;
                alpha = (int) (f * 200);
                offsetX = (int) ((1f - f) * 60);
            } else if (elapsed > n.duration - 200) {
                float f = (elapsed - (n.duration - 200)) / 200f;
                alpha = (int) ((1f - f) * 200);
                offsetX = (int) (f * 60);
            } else {
                alpha = 200;
                offsetX = 0;
            }
            alpha = Math.max(0, Math.min(255, alpha));
            String text = n.title + "  " + n.subtitle;
            int tw = tr.getWidth(text);
            int x = scw - tw - 10 - offsetX;
            int h = 14;
            int bgColor = (alpha << 24) | 0x1A1A1A;
            context.fill(x, y, x + tw + 10, y + h, bgColor);
            int accentColor = (alpha << 24) | 0x5555FF;
            context.fill(x, y, x + 2, y + h, accentColor);
            int textColor = (alpha << 24) | 0xFFFFFF;
            context.drawText(tr, text, x + 6, y + 3, textColor, false);
            y += h + 2;
        }
    }

    private record Notification(String title, String subtitle, long startTime, int duration) {}
}
