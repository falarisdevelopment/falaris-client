package dev.falaris.client.gui.click;

import dev.falaris.client.config.JsonObject;
import dev.falaris.client.module.Category;
import dev.falaris.client.module.Module;
import dev.falaris.client.module.ModuleManager;
import dev.falaris.client.setting.*;
import dev.falaris.client.util.AnimationUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ClickGuiScreen extends Screen {
    private static final Theme[] THEMES = {
        new Theme("Aqua",   0xFF4BC9FF, 0xCC1A1A1A, 0xCC222222, 0xCC2D2D2D, 0xCC383838),
        new Theme("Blue",   0xFF4B8BFF, 0xCC1A1A1A, 0xCC222222, 0xCC2D2D2D, 0xCC383838),
        new Theme("Purple", 0xFFA855F7, 0xCC1A1A1A, 0xCC222222, 0xCC2D2D2D, 0xCC383838),
        new Theme("Green",  0xFF4BC95B, 0xCC1A1A1A, 0xCC222222, 0xCC2D2D2D, 0xCC383838),
        new Theme("Red",    0xFFFF5555, 0xCC1A1A1A, 0xCC222222, 0xCC2D2D2D, 0xCC383838),
        new Theme("Pink",   0xFFFF69B4, 0xCC1A1A1A, 0xCC222222, 0xCC2D2D2D, 0xCC383838),
    };
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT2 = 0xFFAAAAAA;
    private static final int TEXT3 = 0xFF666666;
    private static final int RED = 0xFFFF5555;

    private static int currentTheme;
    public static int guiKeyCode = GLFW.GLFW_KEY_RIGHT_SHIFT;

    private final ModuleManager moduleManager;
    private Category selectedCat = Category.COMBAT;
    private Module settingsMod;
    private boolean settingsListening;
    private float modulesScroll, settingsScroll;
    private String searchText = "";
    private boolean searchFocused;

    // Window geometry
    private int winX, winY, winW, winH;
    private boolean dragging;
    private int dragOffX, dragOffY;
    private Setting<?> draggingSlider;
    private final AnimationUtil.Animation fadeAnim = new AnimationUtil.Animation(0f, 3f);
    private final AnimationUtil.Animation scaleAnim = new AnimationUtil.Animation(0.85f, 3f);
    private float animAccent;
    private int themeFlashTicks;

    public static boolean guiKeyHeld;

    static { loadTheme(); }

    private static Path guiPath() { return FabricLoader.getInstance().getConfigDir().resolve("falaris-client").resolve("gui.json"); }
    private static void loadTheme() {
        try { Path p = guiPath(); if (Files.exists(p)) { JsonObject.parse(Files.readString(p)).intValue("theme").ifPresent(i -> currentTheme = i); } }
        catch (Exception e) {}
    }
    private static void saveTheme() {
        try { Map<String, Object> m = new LinkedHashMap<>(); m.put("theme", (long) currentTheme); Files.createDirectories(guiPath().getParent()); Files.writeString(guiPath(), JsonObject.stringify(m)); }
        catch (Exception e) {}
    }

    public ClickGuiScreen(ModuleManager mm) {
        super(Text.literal("Falaris"));
        moduleManager = mm;
    }

    @Override
    protected void init() {
        winW = MathHelper.clamp(width * 36 / 100, 320, 520);
        winH = MathHelper.clamp(height * 70 / 100, 300, 480);
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
        fadeAnim.setTarget(1f);
        scaleAnim.setTarget(1f);
    }

    @Override public boolean shouldPause() { return false; }

    private Theme theme() { return THEMES[MathHelper.clamp(currentTheme, 0, THEMES.length - 1)]; }

    // Layout constants
    private int headerH() { return 24; }
    private int searchH() { return 18; }
    private int catH() { return 20; }
    private int itemH() { return 15; }
    private int pad() { return 4; }

    private int contentTop() { return winY + headerH(); }
    private int searchTop() { return contentTop() + 2; }
    private int catsTop() { return searchTop() + searchH() + 2; }
    private int modulesTop() { return catsTop() + catH() + 2; }
    private int modulesBottom() { return winY + winH - pad(); }
    private int modulesAvail() { return modulesBottom() - modulesTop(); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        fadeAnim.update(delta);
        scaleAnim.update(delta);
        float open = fadeAnim.getValue();
        float scale = scaleAnim.getValue();
        animAccent = (animAccent + delta * 0.002f) % 1.0f;
        if (themeFlashTicks > 0) themeFlashTicks--;

        // Glass overlay with blur-like effect
        int overlayAlpha = (int) (0x60 * open);
        ctx.fill(0, 0, width, height, (overlayAlpha << 24));

        // Dark vignette edges
        ctx.fill(0, 0, width, 1, 0x40 << 24);
        ctx.fill(0, height - 1, width, height, 0x40 << 24);

        Theme t = theme();
        int pulse = open < 0.99f ? 0 : (int) (Math.sin(animAccent * Math.PI * 2) * 12);
        float sc = 0.92f + 0.08f * scale;
        int cx = width / 2, cy = height / 2;
        int ww = (int) (winW * sc), hh = (int) (winH * sc);
        int xx = cx - ww / 2, yy = cy - hh / 2;

        // Shadow layers
        int shadowColor = 0x40000000;
        for (int i = 3; i >= 0; i--) {
            int s = i * 2;
            ctx.fill(xx - s, yy - s, xx + ww + s, yy + hh + s, shadowColor);
            shadowColor = (shadowColor & 0x00FFFFFF) | (((shadowColor >>> 24) / 2) << 24);
        }

        // Window bg with glass translucency
        ctx.fill(xx, yy, xx + ww, yy + hh, t.bg);
        // Inner glass highlight
        ctx.fill(xx + 1, yy + 1, xx + ww - 1, yy + 2, 0x15FFFFFF);

        // Border with animated accent
        int accentAnim = t.accent;
        if (pulse != 0) {
            int r = MathHelper.clamp(((accentAnim >> 16) & 0xFF) + pulse, 0, 255);
            int g = MathHelper.clamp(((accentAnim >> 8) & 0xFF) + pulse / 2, 0, 255);
            int b = MathHelper.clamp((accentAnim & 0xFF) - pulse, 0, 255);
            accentAnim = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        ctx.fill(xx, yy, xx + ww, yy + 1, accentAnim);
        ctx.fill(xx, yy + hh - 1, xx + ww, yy + hh, t.surface2);
        ctx.fill(xx, yy, xx + 1, yy + hh, t.surface2);
        ctx.fill(xx + ww - 1, yy, xx + ww, yy + hh, t.surface2);

        // Remember window position for interaction
        winX = xx; winY = yy; winW = ww; winH = hh;

        // Header bar
        ctx.fill(winX + 1, winY + 1, winX + winW - 1, winY + headerH(), t.bg2);
        ctx.drawText(textRenderer, "Falaris", winX + 6, winY + 7, t.accent, true);
        ctx.drawText(textRenderer, "[" + glfwKeyName(guiKeyCode) + "]", winX + 60, winY + 7, TEXT3, true);

        // Theme dots
        int dotX = winX + winW - 120;
        for (int i = 0; i < THEMES.length; i++) {
            int col = THEMES[i].accent;
            boolean sel = i == currentTheme % THEMES.length;
            boolean hover = inside(mx, my, dotX, winY + 7, 11, 11);
            int bg = sel ? 0xFFFFFFFF : (hover ? 0x88FFFFFF : withAlpha(col, 40));
            ctx.fill(dotX - 1, winY + 6, dotX + 12, winY + 19, bg);
            ctx.fill(dotX, winY + 7, dotX + 11, winY + 18, col);
            if (hover) {
                ctx.drawText(textRenderer, THEMES[i].name, dotX - textRenderer.getWidth(THEMES[i].name) / 2 + 5, winY - 2, TEXT, true);
            }
            dotX += 14;
        }

        // Flash on theme change
        if (themeFlashTicks > 0) {
            int flashAlpha = Math.min(255, themeFlashTicks * 15);
            ctx.fill(winX, winY, winX + winW, winY + headerH(), (flashAlpha << 24) | (t.accent & 0x00FFFFFF));
        }

        // Close
        boolean closeH = inside(mx, my, winX + winW - 16, winY + 5, 12, 14);
        ctx.drawText(textRenderer, "\u2715", winX + winW - 14, winY + 7, closeH ? RED : TEXT2, true);

        // Search bar
        int sx = winX + 6, sw = winW - 12;
        boolean searchHov = inside(mx, my, sx, searchTop(), sw, searchH());
        ctx.fill(sx, searchTop(), sx + sw, searchTop() + searchH(), searchFocused ? t.surface : (searchHov ? t.surface2 : t.bg2));
        String searchDisplay = searchFocused && searchText.isEmpty() ? "" : searchText;
        String searchLabel = searchDisplay.isEmpty() && !searchFocused ? "\u2315 Search..." : searchDisplay;
        if (searchFocused && searchText.isEmpty()) {
            ctx.drawText(textRenderer, "\u2315 Type...", sx + 2, searchTop() + 3, TEXT3, true);
        } else {
            ctx.drawText(textRenderer, "\u2315 " + searchDisplay, sx + 2, searchTop() + 3, searchFocused ? TEXT : TEXT3, true);
        }

        // Category tabs
        int catX = winX + 4;
        int tabW = (winW - 8) / Category.values().length;
        for (Category c : Category.values()) {
            boolean sel = c == selectedCat || (!searchText.isEmpty());
            boolean hov = inside(mx, my, catX, catsTop(), tabW, catH());
            ctx.fill(catX, catsTop(), catX + tabW, catsTop() + catH(), sel ? t.surface : (hov ? t.surface2 : t.bg));
            String label = c.getDisplayName();
            int tw = textRenderer.getWidth(label);
            int tx = catX + (tabW - tw) / 2;
            ctx.drawText(textRenderer, label, tx, catsTop() + 4, sel ? t.accent : (hov ? TEXT : TEXT2), true);
            catX += tabW;
        }

        // Content area
        if (settingsMod != null) {
            renderSettings(ctx, mx, my, t);
        } else {
            renderModules(ctx, mx, my, t);
        }
    }

    private void renderModules(DrawContext ctx, int mx, int my, Theme t) {
        List<Module> mods = moduleManager.search(searchText, searchText.isEmpty() ? selectedCat : null);
        int top = modulesTop(), avail = modulesAvail();
        int max = Math.max(0, mods.size() * itemH() - avail);
        modulesScroll = MathHelper.clamp(modulesScroll, 0, max);
        if (!mods.isEmpty() && max == 0) modulesScroll = 0;
        int yo = top - (int) modulesScroll;
        for (Module mod : mods) {
            if (yo + itemH() < top || yo > modulesBottom()) { yo += itemH(); continue; }
            boolean hover = inside(mx, my, winX + 4, yo, winW - 8, itemH());
            boolean on = mod.isEnabled();
            ctx.fill(winX + 4, yo, winX + winW - 4, yo + itemH(), on ? t.surface : (hover ? t.surface2 : t.bg));
            ctx.drawText(textRenderer, mod.getName(), winX + 7, yo + 3, on ? TEXT : (hover ? TEXT : TEXT2), true);
            ctx.fill(winX + winW - 12, yo + 3, winX + winW - 4, yo + 12, on ? t.accent : TEXT3);
            if (on) ctx.fill(winX + winW - 10, yo + 5, winX + winW - 6, yo + 10, 0xFFFFFFFF);
            yo += itemH();
        }
    }

    private void renderSettings(DrawContext ctx, int mx, int my, Theme t) {
        int top = modulesTop(), avail = modulesAvail();
        boolean backH = inside(mx, my, winX + 4, top, 50, 14);
        ctx.fill(winX + 4, top, winX + 54, top + 14, backH ? t.surface2 : t.surface);
        ctx.drawText(textRenderer, "\u2190 " + settingsMod.getName(), winX + 7, top + 3, backH ? t.accent : TEXT2, true);

        int sy = top + 18;
        String kt;
        if (settingsListening) kt = "> Press key...";
        else {
            int kc = settingsMod.getKeyCode();
            kt = kc < 0 ? "None" : Objects.requireNonNullElse(GLFW.glfwGetKeyName(kc, 0), "Key" + kc);
        }
        boolean bindH = inside(mx, my, winX + 4, sy, winW - 8, 14);
        ctx.fill(winX + 4, sy, winX + winW - 4, sy + 14, bindH ? t.surface2 : t.surface);
        ctx.drawText(textRenderer, "Bind: " + kt, winX + 7, sy + 3, settingsListening ? t.accent : TEXT2, true);

        int sY = sy + 18, mh = modulesBottom() - sY;
        List<Setting<?>> sets = settingsMod.getSettings();
        int max = Math.max(0, sets.size() * itemH() - mh);
        settingsScroll = MathHelper.clamp(settingsScroll, 0, max);
        int so = sY - (int) settingsScroll;
        for (Setting<?> s : sets) {
            if (so + itemH() < sY || so > modulesBottom()) { so += itemH(); continue; }
            ctx.drawText(textRenderer, s.getName(), winX + 6, so + 1, TEXT2, true);
            int valRight = winX + winW - 8;
            if (s instanceof BooleanSetting bs) {
                ctx.fill(valRight - 12, so + 2, valRight, so + 12, bs.enabled() ? t.accent : TEXT3);
                float kx = valRight - 12 + (bs.enabled() ? 7 : 0);
                ctx.fill((int) kx, so + 3, (int) (kx + 7), so + 11, 0xFFFFFFFF);
            } else if (s instanceof ModeSetting ms) {
                String v = ms.get();
                ctx.drawText(textRenderer, v, valRight - textRenderer.getWidth(v), so + 1, t.accent, true);
            } else if (s instanceof DoubleSetting ds) {
                float p = (float) ((ds.get() - ds.min()) / (ds.max() - ds.min()));
                ctx.fill(winX + 6, so + 11, valRight, so + 13, t.surface);
                if (p > 0.005f) ctx.fill(winX + 6, so + 11, winX + 6 + (int) ((valRight - winX - 6) * p), so + 13, t.accent);
                ctx.drawText(textRenderer, String.format("%.1f", ds.get()), valRight - textRenderer.getWidth(String.format("%.1f", ds.get())), so + 1, t.accent, true);
            } else if (s instanceof IntegerSetting is) {
                float p = (float) (is.get() - is.min()) / (is.max() - is.min());
                ctx.fill(winX + 6, so + 11, valRight, so + 13, t.surface);
                if (p > 0.005f) ctx.fill(winX + 6, so + 11, winX + 6 + (int) ((valRight - winX - 6) * p), so + 13, t.accent);
                ctx.drawText(textRenderer, String.valueOf(is.get()), valRight - textRenderer.getWidth(String.valueOf(is.get())), so + 1, t.accent, true);
            }
            so += itemH();
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int btn = click.button();

        if (inside(mx, my, winX + winW - 16, winY + 4, 12, 14)) { close(); return true; }
        if (inside(mx, my, winX + 1, winY + 1, winW - 2, headerH())) {
            dragging = true; dragOffX = (int) (mx - winX); dragOffY = (int) (my - winY); return true;
        }

        // Theme dots
        int dotX = winX + winW - 120;
        for (int i = 0; i < THEMES.length; i++) {
            if (inside(mx, my, dotX - 1, winY + 6, 14, 14)) {
                currentTheme = i;
                saveTheme();
                themeFlashTicks = 12;
                return true;
            }
            dotX += 14;
        }

        // Search
        if (inside(mx, my, winX + 6, searchTop(), winW - 12, searchH())) { searchFocused = true; return true; }

        // Category tabs
        int cx = winX + 4;
        int tabW = (winW - 8) / Category.values().length;
        for (Category cat : Category.values()) {
            if (inside(mx, my, cx, catsTop(), tabW, catH())) { selectedCat = cat; modulesScroll = 0; searchText = ""; return true; }
            cx += tabW;
        }

        // Settings
        if (settingsMod != null) {
            int top = modulesTop();
            if (inside(mx, my, winX + 4, top, 50, 14)) { closeSettings(); return true; }
            int sy = top + 18;
            if (inside(mx, my, winX + 4, sy, winW - 8, 14)) { settingsListening = !settingsListening; return true; }
            int sY = sy + 18;
            int so = sY - (int) settingsScroll;
            for (Setting<?> s : settingsMod.getSettings()) {
                if (so + itemH() < sY || so > modulesBottom()) { so += itemH(); continue; }
                if (inside(mx, my, winX + 4, so, winW - 8, itemH())) {
                    if (s instanceof BooleanSetting bs) { bs.set(!bs.enabled()); moduleManager.save(); }
                    else if (s instanceof ModeSetting ms) {
                        int idx = ms.modes().indexOf(ms.get());
                        idx = btn == 1 ? idx - 1 : idx + 1;
                        if (idx >= ms.modes().size()) idx = 0; if (idx < 0) idx = ms.modes().size() - 1;
                        ms.set(ms.modes().get(idx)); moduleManager.save();
                    } else if (s instanceof DoubleSetting ds) {
                        int valRight = winX + winW - 8;
                        if (inside(mx, my, winX + 6, so + 9, valRight - winX - 6, 6)) {
                            draggingSlider = s;
                            updateSliderFromMouse(mx, ds, winX + 6, valRight);
                        } else {
                            double step = (ds.max() - ds.min()) / 100.0;
                            ds.set(MathHelper.clamp(ds.get() + (btn == 1 ? -step : step), ds.min(), ds.max()));
                        }
                        moduleManager.save();
                    } else if (s instanceof IntegerSetting is) {
                        int valRight = winX + winW - 8;
                        if (inside(mx, my, winX + 6, so + 9, valRight - winX - 6, 6)) {
                            draggingSlider = s;
                            updateSliderFromMouse(mx, is, winX + 6, valRight);
                        } else {
                            is.set(MathHelper.clamp(is.get() + (btn == 1 ? -1 : 1), is.min(), is.max()));
                        }
                        moduleManager.save();
                    }
                    return true;
                }
                so += itemH();
            }
            return true;
        }

        // Module list
        if (!inside(mx, my, winX, winY, winW, winH)) { if (settingsMod != null) closeSettings(); return true; }
        List<Module> mods = moduleManager.search(searchText, searchText.isEmpty() ? selectedCat : null);
        int avail = modulesAvail();
        int max = Math.max(0, mods.size() * itemH() - avail);
        modulesScroll = MathHelper.clamp(modulesScroll, 0, max);
        int top = modulesTop(), yo = top - (int) modulesScroll;
        for (Module mod : mods) {
            if (yo + itemH() < top || yo > modulesBottom()) { yo += itemH(); continue; }
            if (inside(mx, my, winX + 4, yo, winW - 8, itemH())) {
                if (btn == 0) { mod.toggle(); moduleManager.save(); }
                else if (btn == 1) { openSettings(mod); }
                return true;
            }
            yo += itemH();
        }
        return true;
    }

    @Override public boolean mouseReleased(net.minecraft.client.gui.Click click) { dragging = false; draggingSlider = null; return true; }

    @Override
    public boolean mouseDragged(net.minecraft.client.gui.Click click, double dx, double dy) {
        if (dragging) {
            winX = MathHelper.clamp((int) click.x() - dragOffX, -4, width - winW + 4);
            winY = MathHelper.clamp((int) click.y() - dragOffY, -4, height - winH + 4);
        }
        if (draggingSlider != null) {
            int valRight = winX + winW - 8;
            if (draggingSlider instanceof DoubleSetting ds) {
                updateSliderFromMouse(click.x(), ds, winX + 6, valRight);
                moduleManager.save();
            } else if (draggingSlider instanceof IntegerSetting is) {
                updateSliderFromMouse(click.x(), is, winX + 6, valRight);
                moduleManager.save();
            }
        }
        return true;
    }

    private void updateSliderFromMouse(double mx, DoubleSetting s, int barX, int barRight) {
        double p = MathHelper.clamp((mx - barX) / (double) (barRight - barX), 0.0, 1.0);
        double val = s.min() + p * (s.max() - s.min());
        s.set(val);
    }

    private void updateSliderFromMouse(double mx, IntegerSetting s, int barX, int barRight) {
        double p = MathHelper.clamp((mx - barX) / (double) (barRight - barX), 0.0, 1.0);
        int val = (int) Math.round(s.min() + p * (s.max() - s.min()));
        s.set(val);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horiz, double vert) {
        if (!inside(mx, my, winX, winY, winW, winH)) return true;
        if (settingsMod != null) {
            int max = Math.max(0, settingsMod.getSettings().size() * itemH() - (modulesBottom() - modulesTop() - 36));
            settingsScroll = MathHelper.clamp(settingsScroll - (float) vert * 15, 0, max);
        } else {
            List<Module> mods = moduleManager.search(searchText, searchText.isEmpty() ? selectedCat : null);
            int max = Math.max(0, mods.size() * itemH() - modulesAvail());
            modulesScroll = MathHelper.clamp(modulesScroll - (float) vert * 15, 0, max);
        }
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput key) {
        int kc = key.key();
        if (settingsListening && settingsMod != null) {
            settingsMod.setKeyCode(kc == 256 ? -1 : kc);
            moduleManager.save(); settingsListening = false; return true;
        }
        if (searchFocused) {
            if (kc == 259 && !searchText.isEmpty()) { searchText = searchText.substring(0, searchText.length() - 1); modulesScroll = 0; }
            else if (kc == 256 || kc == 257) searchFocused = false;
            return true;
        }
        // If typing a printable key outside search, auto-focus search
        if (!searchFocused && isPrintable(kc)) {
            searchFocused = true;
            char ch = (char) kc;
            if (isSearchChar(String.valueOf(ch))) { searchText = String.valueOf(ch).toLowerCase(); modulesScroll = 0; }
            return true;
        }
        if (kc == 256) { if (settingsMod != null) closeSettings(); else close(); return true; }
        return super.keyPressed(key);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharInput ch) {
        String c = new String(Character.toChars(ch.codepoint()));
        if (c.isEmpty()) return true;
        if (searchFocused && isSearchChar(c)) { searchText += c; modulesScroll = 0; return true; }
        if (!searchFocused && isSearchChar(c)) {
            searchFocused = true;
            searchText = c;
            modulesScroll = 0;
            return true;
        }
        return super.charTyped(ch);
    }

    private static boolean isSearchChar(String c) {
        if (c.length() != 1) return false;
        char ch = c.charAt(0);
        return Character.isLetterOrDigit(ch) || Character.isWhitespace(ch) || ch == '_' || ch == '-' || ch == '.';
    }

    private static boolean isPrintable(int keyCode) {
        return (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z)
            || (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9)
            || keyCode == GLFW.GLFW_KEY_SPACE
            || keyCode == GLFW.GLFW_KEY_MINUS
            || keyCode == GLFW.GLFW_KEY_PERIOD;
    }

    @Override public void close() { settingsMod = null; super.close(); }

    private void openSettings(Module mod) { settingsMod = mod; settingsScroll = 0; settingsListening = false; }
    private void closeSettings() { settingsMod = null; settingsListening = false; settingsScroll = 0; }

    private static int withAlpha(int c, int a) { return (a & 0xFF) << 24 | (c & 0xFFFFFF); }

    private record Theme(String name, int accent, int bg, int bg2, int surface, int surface2) {}

    private static boolean inside(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static String glfwKeyName(int code) {
        if (code <= 0) return "None";
        String n = GLFW.glfwGetKeyName(code, 0);
        if (n != null) return n;
        if (code == GLFW.GLFW_KEY_RIGHT_SHIFT) return "RShift";
        if (code == GLFW.GLFW_KEY_LEFT_SHIFT) return "LShift";
        if (code == GLFW.GLFW_KEY_RIGHT_CONTROL) return "RCtrl";
        if (code == GLFW.GLFW_KEY_LEFT_CONTROL) return "LCtrl";
        if (code == GLFW.GLFW_KEY_RIGHT_ALT) return "RAlt";
        if (code == GLFW.GLFW_KEY_LEFT_ALT) return "LAlt";
        if (code == GLFW.GLFW_KEY_ESCAPE) return "Esc";
        return "Key" + code;
    }
}
