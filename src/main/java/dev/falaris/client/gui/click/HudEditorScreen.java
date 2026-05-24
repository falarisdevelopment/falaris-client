package dev.falaris.client.gui.click;

import dev.falaris.client.module.Module;
import dev.falaris.client.module.ModuleManager;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class HudEditorScreen extends Screen {
    private static final int SNAP_GRID = 5;
    private static final int BOX_W = 120;
    private static final int BOX_H = 24;

    private final ModuleManager moduleManager;
    private final List<HudElement> elements = new ArrayList<>();

    private HudElement dragging;
    private int dragOffX, dragOffY;

    public HudEditorScreen(ModuleManager moduleManager) {
        super(Text.literal("HUD Editor"));
        this.moduleManager = moduleManager;
        buildElements();
    }

    private void buildElements() {
        elements.clear();
        for (Module mod : moduleManager.getModules()) {
            if (!mod.isEnabled()) continue;
            IntegerSetting xSetting = null;
            IntegerSetting ySetting = null;
            for (Setting<?> s : mod.getSettings()) {
                if (s.getName().equalsIgnoreCase("Offset X") && s instanceof IntegerSetting is) {
                    xSetting = is;
                }
                if (s.getName().equalsIgnoreCase("Offset Y") && s instanceof IntegerSetting is) {
                    ySetting = is;
                }
            }
            if (xSetting != null && ySetting != null) {
                elements.add(new HudElement(mod, xSetting, ySetting));
            }
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x40000000);
        int sw = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int sh = MinecraftClient.getInstance().getWindow().getScaledHeight();

        // Draw grid
        for (int x = 0; x < sw; x += SNAP_GRID * 10) {
            ctx.fill(x, 0, x + 1, sh, 0x15FFFFFF);
        }
        for (int y = 0; y < sh; y += SNAP_GRID * 10) {
            ctx.fill(0, y, sw, y + 1, 0x15FFFFFF);
        }

        ctx.drawText(textRenderer, "HUD Editor - Drag boxes to reposition, ESC to exit", 10, 10, 0xFFCCCCCC, false);

        for (HudElement elem : elements) {
            elem.render(ctx, mx, my);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        if (click.button() == 0) {
            for (HudElement elem : elements) {
                if (mx >= elem.x && mx <= elem.x + BOX_W && my >= elem.y && my <= elem.y + BOX_H) {
                    dragging = elem;
                    dragOffX = (int) mx - elem.x;
                    dragOffY = (int) my - elem.y;
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.gui.Click click, double dx, double dy) {
        if (dragging != null) {
            int nx = (int) click.x() - dragOffX;
            int ny = (int) click.y() - dragOffY;
            nx = Math.round(nx / (float) SNAP_GRID) * SNAP_GRID;
            ny = Math.round(ny / (float) SNAP_GRID) * SNAP_GRID;
            dragging.x = Math.max(0, nx);
            dragging.y = Math.max(0, ny);
            dragging.xSetting.set(dragging.x);
            dragging.ySetting.set(dragging.y);
            moduleManager.save();
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.gui.Click click) {
        dragging = null;
        return true;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void close() {
        dragging = null;
        super.close();
    }

    private static class HudElement {
        final Module mod;
        final IntegerSetting xSetting;
        final IntegerSetting ySetting;
        int x, y;

        HudElement(Module mod, IntegerSetting xSetting, IntegerSetting ySetting) {
            this.mod = mod;
            this.xSetting = xSetting;
            this.ySetting = ySetting;
            this.x = xSetting.get();
            this.y = ySetting.get();
        }

        void render(DrawContext ctx, int mx, int my) {
            boolean hovered = mx >= x && mx <= x + BOX_W && my >= y && my <= y + BOX_H;
            int bg = hovered ? 0xFF3A3C4E : 0xFF242632;
            ctx.fill(x, y, x + BOX_W, y + BOX_H, bg);
            ctx.fill(x, y, x + 3, y + BOX_H, 0xFF54B8B3);
            ctx.drawText(MinecraftClient.getInstance().textRenderer, mod.getName(), x + 10, y + (BOX_H - 8) / 2, 0xFFDCE1EB, false);
        }
    }
}
