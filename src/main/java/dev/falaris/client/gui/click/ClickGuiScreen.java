package dev.falaris.client.gui.click;

import dev.falaris.client.module.Category;
import dev.falaris.client.module.Module;
import dev.falaris.client.module.ModuleManager;
import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import dev.falaris.client.setting.Setting;
import dev.falaris.client.setting.KeybindSetting;
import dev.falaris.client.setting.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClickGuiScreen extends Screen {
    private static final int MIN_W = 400;
    private static final int MIN_H = 350;
    private static final int SIDEBAR_W = 150;
    private static final int CAT_ITEM_H = 30;
    private static final int MOD_ITEM_H = 28;
    private static final int MOD_GAP = 3;
    private static final int TITLE_H = 38;
    private static final int RESIZE_H = 10;
    private static final int WIN_RADIUS = 10;
    private static final int GRID_SNAP = 10;

    private int winW = 500;
    private int winH = 400;

    // Fixed dark theme — no cycle
    private static int accent() { return argb(255, 84, 184, 179); }
    private static int accentDim() { return argb(255, 60, 150, 145); }
    private static int bg() { return argb(255, 14, 16, 20); }
    private static int bg2() { return argb(255, 18, 20, 26); }
    private static int bg3() { return argb(255, 22, 24, 32); }
    private static int surface() { return argb(255, 30, 32, 42); }
    private static int surface2() { return argb(255, 36, 38, 50); }
    private static int text() { return argb(255, 215, 220, 230); }
    private static int textSoft() { return argb(255, 160, 165, 185); }
    private static int textMuted() { return argb(255, 105, 110, 135); }
    private static int SHADOW = argb(80, 0, 0, 0);
    private static int green() { return argb(255, 80, 220, 120); }
    private static int red() { return argb(255, 220, 80, 80); }

    private final ModuleManager moduleManager;

    private int winX, winY;
    private boolean dragging;
    private int dragOffX, dragOffY;
    private boolean resizing;
    private int resizeOffX, resizeOffY;

    // "Virtual categories" for tools
    private static final String VIRTUAL_PRESETS = "__presets__";
    private static final String VIRTUAL_FRIENDS = "__friends__";
    private static final String VIRTUAL_IGNORES = "__ignores__";

    private String selectedCategory = Category.COMBAT.name();
    private Module settingsModule;
    private float scrollOffset;
    private float settingsScrollOffset;
    private float settingsFieldX;

    private final Animation scrollAnim = new Animation(0.0f, 0.15f);
    private final Animation settingsScrollAnim = new Animation(0.0f, 0.12f);
    private final Animation settingsAnim = new Animation(0.0f, 0.12f);
    private final Animation openAnim = new Animation(0.0f, 0.06f);

    private Setting<?> draggingSetting;
    private boolean draggingSlider;
    private boolean listeningForKey;
    private String listeningKeyText = "";

    private String searchQuery = "";
    private boolean searchFocused;
    private String tooltipText;
    private int tooltipX, tooltipY;

    private final Map<String, Animation> toggleAnims = new HashMap<>();
    private final Map<String, Animation> hoverAnims = new HashMap<>();
    private final Map<String, Animation> catHoverAnims = new HashMap<>();
    private final Map<String, Animation> settingFadeAnims = new HashMap<>();

    private List<Module> cachedModules = List.of();
    private boolean cacheDirty = true;

    // Friends input
    private String friendInput = "";
    private boolean friendInputFocused;
    // Ignores input
    private String ignoreInput = "";
    private boolean ignoreInputFocused;

    // Sidebar items: real categories + virtual
    private static final String[] SIDEBAR_ITEMS = {
        "COMBAT", "MOVEMENT", "RENDER", "PLAYER", "WORLD", "MISC", "CLIENT",
        VIRTUAL_PRESETS, VIRTUAL_FRIENDS, VIRTUAL_IGNORES
    };
    private static final String[] SIDEBAR_LABELS = {
        "Combat", "Movement", "Render", "Player", "World", "Misc", "Client",
        "Presets", "Friends", "Ignores"
    };
    private static final String[] SIDEBAR_ICONS = {
        "\u2694", "\u27A4", "\u25CF", "\u2666", "\u2295", "\u2699", "\u2605",
        "\u2699", "\u2605", "\u26D4"
    };

    public ClickGuiScreen(ModuleManager moduleManager) {
        super(Text.literal("Falaris"));
        this.moduleManager = moduleManager;
        center();
        openAnim.setTarget(1.0f);
        for (String s : SIDEBAR_ITEMS) {
            catHoverAnims.put(s, new Animation(0.0f, 0.15f));
        }
    }

    private void center() {
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;
    }

    @Override
    protected void init() { center(); }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void tick() {
        openAnim.tick();
        settingsAnim.tick();
        scrollAnim.tick();
        settingsScrollAnim.tick();
        cacheDirty = true;

        if (settingsModule != null) settingsAnim.setTarget(1.0f);
        else settingsAnim.setTarget(0.0f);
        settingsFieldX = settingsAnim.get() * 220;

        for (Module m : moduleManager.getModules()) {
            String id = m.getId();
            toggleAnims.computeIfAbsent(id, k -> new Animation(m.isEnabled() ? 1.0f : 0.0f, 0.15f))
                .setTarget(m.isEnabled() ? 1.0f : 0.0f);
            toggleAnims.get(id).tick();
            hoverAnims.computeIfAbsent(id, k -> new Animation(0.0f, 0.12f)).tick();
        }
        for (String s : SIDEBAR_ITEMS) {
            Animation a = catHoverAnims.get(s);
            if (a != null) a.tick();
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        float open = Animation.easeOutBack(openAnim.get());
        if (open < 0.005f) return;
        super.render(ctx, mx, my, delta);
        drawWindow(ctx, mx, my, open);
        if (tooltipText != null && !tooltipText.isEmpty()) {
            int tw = textRenderer.getWidth(tooltipText);
            int tx = Math.min(tooltipX, width - tw - 8);
            int ty = tooltipY - 14;
            if (ty < 0) ty = tooltipY + 14;
            ctx.fill(tx - 4, ty, tx + tw + 4, ty + 10, bg());
            ctx.drawText(textRenderer, tooltipText, tx, ty + 1, textMuted(), false);
            tooltipText = null;
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        int a = (int) (Animation.easeOutQuint(openAnim.get()) * 70);
        ctx.fill(0, 0, width, height, (a << 24));
    }

    private void drawWindow(DrawContext ctx, int mx, int my, float open) {
        int a = (int) (open * 255);
        int r = WIN_RADIUS;

        for (int i = 4; i > 0; i--) {
            int alpha = 18 - i * 3;
            UiRender.fillRound(ctx, winX - i, winY - i, winW + i * 2, winH + i * 2, r + i, (alpha << 24));
        }
        UiRender.fillRound(ctx, winX + 4, winY + winH, winW - 8, 6, 0, SHADOW);
        UiRender.fillRound(ctx, winX, winY + winH + 4, winW, 6, 0, (50 << 24));

        UiRender.fillRound(ctx, winX, winY, winW, winH, r, bg());
        UiRender.fillRound(ctx, winX, winY, winW, TITLE_H, r, bg2());
        ctx.fill(winX, winY + TITLE_H - 4, winX + winW, winY + TITLE_H, bg2());

        drawFLogo(ctx, winX + 10, winY + 9, a);

        // Gradient title text "Falaris"
        String title = "Falaris";
        float titleScale = 1.3f;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(titleScale, titleScale);
        int tx = (int) ((winX + 32) / titleScale);
        int ty = (int) ((winY + 10) / titleScale);
        for (int i = 0; i < title.length(); i++) {
            float t = title.length() > 1 ? (float) i / (title.length() - 1) : 0.5f;
            int col = UiRender.mix(accent(), text(), 1.0f - t * 0.3f);
            ctx.drawText(textRenderer, String.valueOf(title.charAt(i)), tx, ty, col, true);
            tx += textRenderer.getWidth(String.valueOf(title.charAt(i)));
        }
        ctx.getMatrices().popMatrix();
        ctx.drawText(textRenderer, Text.literal("CLIENT"), winX + 115, winY + 13, textMuted(), true);

        // Close button with rounded bg on hover
        boolean closeHovered = inside(mx, my, winX + winW - 24, winY + 8, 18, 18);
        if (closeHovered) {
            UiRender.fillRound(ctx, winX + winW - 24, winY + 8, 18, 18, 4, surface());
        }
        ctx.drawText(textRenderer, Text.literal("\u2715"), winX + winW - 20, winY + 12, closeHovered ? accent() : textSoft(), true);

        drawSidebar(ctx, mx, my, a);
        drawModuleList(ctx, mx, my, a);

        if (settingsFieldX > 5) {
            drawSettings(ctx, mx, my, a);
        }
        drawResizeHandle(ctx, mx, my, a);
    }

    private void drawResizeHandle(DrawContext ctx, int mx, int my, int a) {
        int rx = winX + winW - RESIZE_H;
        int ry = winY + winH - RESIZE_H;
        boolean hover = inside(mx, my, rx, ry, RESIZE_H, RESIZE_H);
        int col = UiRender.mix(surface(), accent(), hover ? 0.6f : 0.0f);
        ctx.fill(rx + 2, ry + 6, rx + 8, ry + 8, col);
        ctx.fill(rx + 4, ry + 4, rx + 8, ry + 6, col);
        ctx.fill(rx + 6, ry + 2, rx + 8, ry + 4, col);
    }

    private void drawFLogo(DrawContext ctx, int x, int y, int a) {
        ctx.fill(x + 2, y, x + 6, y + 15, accent());
        ctx.fill(x + 2, y, x + 13, y + 4, accent());
        ctx.fill(x + 2, y + 7, x + 11, y + 10, accent());
    }

    private boolean isVirtual(String cat) {
        return VIRTUAL_PRESETS.equals(cat) || VIRTUAL_FRIENDS.equals(cat) || VIRTUAL_IGNORES.equals(cat);
    }

    private void drawSidebar(DrawContext ctx, int mx, int my, int a) {
        int sx = winX;
        int sy = winY + TITLE_H;
        int sw = SIDEBAR_W;
        ctx.fill(sx, sy, sx + sw, winY + winH, bg3());

        int sidebarH = winH - TITLE_H;
        int contentH = SIDEBAR_ITEMS.length * CAT_ITEM_H;
        int maxScroll = Math.max(0, contentH - sidebarH);
        scrollAnim.setTarget(MathHelper.clamp(scrollOffset, 0, maxScroll));
        float smooth = scrollAnim.get();

        ctx.enableScissor(sx, sy, sx + sw, winY + winH);

        for (int i = 0; i < SIDEBAR_ITEMS.length; i++) {
            String item = SIDEBAR_ITEMS[i];
            int cy = sy + 6 + i * CAT_ITEM_H - (int) smooth;
            if (cy + CAT_ITEM_H < sy || cy > sy + sidebarH) continue;

            boolean hovered = inside(mx, my, sx + 4, cy, sw - 8, CAT_ITEM_H - 4);
            boolean selected = item.equals(selectedCategory);
            catHoverAnims.get(item).setTarget(hovered || selected ? 1.0f : 0.0f);
            float hov = Animation.easeOutQuart(catHoverAnims.get(item).get());

            if (hov > 0.01f) {
                int bbg = UiRender.mix(bg3(), surface(), hov * 0.7f);
                int pad = selected ? 0 : 2;
                UiRender.fillRound(ctx, sx + pad, cy, sw - pad * 2, CAT_ITEM_H - 4, 6, bbg);
                if (selected) {
                    UiRender.fillGlow(ctx, sx + 4, cy, sw - 8, CAT_ITEM_H - 4, 6, UiColor.withAlpha(accent(), (int)(22 * hov)));
                }
            }
            if (selected) {
                UiRender.fillRound(ctx, sx + 2, cy + 5, 4, CAT_ITEM_H - 14, 2, accent());
            }

            String icon = SIDEBAR_ICONS[i % SIDEBAR_ICONS.length];
            int iconColor = isVirtual(item) ? (selected ? green() : textMuted()) : (selected ? accent() : textSoft());
            ctx.drawText(textRenderer, icon, sx + 14, cy + (CAT_ITEM_H - 8) / 2, iconColor, true);
            ctx.drawText(textRenderer, SIDEBAR_LABELS[i], sx + 36, cy + (CAT_ITEM_H - 8) / 2, selected ? text() : textSoft(), true);

            // Module count badge (only for real categories)
                if (!isVirtual(item)) {
                try {
                    Category cat = Category.valueOf(item);
                    int count = moduleManager.search("", cat).size();
                    String countStr = String.valueOf(count);
                    int cw = textRenderer.getWidth(countStr);
                    int bx = sx + sw - 22 - cw, by2 = cy + (CAT_ITEM_H - 12) / 2;
                    UiRender.fillRound(ctx, bx, by2, cw + 8, 12, 6, selected ? UiColor.withAlpha(accent(), 180) : surface());
                    ctx.drawText(textRenderer, countStr, bx + 4, by2 + 2, selected ? bg3() : textMuted(), false);
                } catch (Exception ignored) {}
            }

            if (selected) {
                ctx.drawText(textRenderer, "\u276F", sx + sw - 12, cy + (CAT_ITEM_H - 8) / 2, accent(), true);
            }
        }
        ctx.disableScissor();
    }

    private void drawModuleList(DrawContext ctx, int mx, int my, int a) {
        int modX = winX + SIDEBAR_W;
        int modY = winY + TITLE_H;
        int modW = winW - SIDEBAR_W - (int) settingsFieldX;
        int modH = winH - TITLE_H;

        UiRender.fillRound(ctx, modX, modY, modW, modH, 0, bg2());
        ctx.enableScissor(modX, modY, modX + modW, modY + modH);

        // Virtual category content
        if (VIRTUAL_PRESETS.equals(selectedCategory)) {
            drawPresetsContent(ctx, mx, my, modX, modY, modW, modH);
            ctx.disableScissor();
            return;
        }
        if (VIRTUAL_FRIENDS.equals(selectedCategory)) {
            drawFriendsContent(ctx, mx, my, modX, modY, modW, modH);
            ctx.disableScissor();
            return;
        }
        if (VIRTUAL_IGNORES.equals(selectedCategory)) {
            drawIgnoresContent(ctx, mx, my, modX, modY, modW, modH);
            ctx.disableScissor();
            return;
        }

        // Category header
        String catLabel = selectedCategory.substring(0, 1) + selectedCategory.substring(1).toLowerCase();
        ctx.drawText(textRenderer, catLabel, modX + 12, modY + 8, text(), true);
        String countStr = getModules().size() + " modules";
        ctx.drawText(textRenderer, countStr, modX + 12 + textRenderer.getWidth(catLabel) + 8, modY + 8, textMuted(), false);

        // Header separator
        int sepY = modY + 24;
        ctx.fill(modX + 10, sepY, modX + modW - 10, sepY + 1, surface());

        // Search bar
        int searchY = sepY + 8;
        int searchH = 16;
        UiRender.fillRound(ctx, modX + 10, searchY, modW - 20, searchH, 4, surface());
        ctx.drawText(textRenderer, "\u2315", modX + 14, searchY + 3, textMuted(), true);
        String display = searchFocused && searchQuery.isEmpty() ? "" : searchQuery;
        String show = display.isEmpty() && !searchFocused ? "Search..." : display;
        ctx.drawText(textRenderer, show, modX + 26, searchY + 3, display.isEmpty() && !searchFocused ? textMuted() : text(), false);
        if (searchFocused && (System.currentTimeMillis() / 600) % 2 == 0) {
            int cx = modX + 26 + textRenderer.getWidth(display);
            ctx.fill(cx, searchY + 2, cx + 1, searchY + searchH - 2, text());
        }

        int listY = searchY + searchH + 8;
        int listH = modH - (listY - modY);
        List<Module> modules = getModules();
        int contentH = modules.size() * (MOD_ITEM_H + MOD_GAP);
        int maxScroll = Math.max(0, contentH - listH);
        float targetScroll = MathHelper.clamp(scrollOffset, 0, maxScroll);
        scrollAnim.setTarget(targetScroll);
        float smoothScroll = scrollAnim.get();

        for (int i = 0; i < modules.size(); i++) {
            Module mod = modules.get(i);
            int by = listY + i * (MOD_ITEM_H + MOD_GAP) - (int) smoothScroll;
            if (by + MOD_ITEM_H < listY || by > listY + listH) continue;

            boolean hovered = inside(mx, my, modX + 10, by, modW - 20, MOD_ITEM_H);
            hoverAnims.get(mod.getId()).setTarget(hovered ? 1.0f : 0.0f);
            float hov = Animation.easeOutQuart(hoverAnims.get(mod.getId()).get());
            float toggle = Animation.easeOutQuart(toggleAnims.get(mod.getId()).get());

            int bg = UiRender.mix(bg3(), surface2(), hov * 0.6f);
            UiRender.fillRound(ctx, modX + 10, by, modW - 20, MOD_ITEM_H, 6, bg);
            if (hov > 0.3f) {
                UiRender.fillGlow(ctx, modX + 10, by, modW - 20, MOD_ITEM_H, 6, UiColor.withAlpha(accent(), (int)(12 * hov)));
            }

            int stripColor = UiRender.mix(textMuted(), accent(), toggle);
            UiRender.fillRound(ctx, modX + 12, by + 4, 3, MOD_ITEM_H - 8, 1, stripColor);

            ctx.drawText(textRenderer, mod.getName(), modX + 22, by + (MOD_ITEM_H - 8) / 2, UiRender.mix(textSoft(), text(), toggle), true);

            // Description tooltip inline on hover
            if (hovered) {
                String desc = mod.getDescription();
                if (desc != null && !desc.isEmpty()) {
                    int dw = textRenderer.getWidth(desc);
                    int dx = Math.min(modX + 22, modX + modW - dw - 12);
                    ctx.fill(dx - 2, by + MOD_ITEM_H, dx + dw + 2, by + MOD_ITEM_H + 10, bg3());
                    ctx.drawText(textRenderer, desc, dx, by + MOD_ITEM_H + 1, textMuted(), false);
                }
                tooltipText = "Right-click for settings";
                tooltipX = mx + 10;
                tooltipY = my;
            }

            // Toggle switch (bigger, sleeker)
            int tw = 28, th = 12;
            int tx = modX + modW - 50, ty = by + (MOD_ITEM_H - th) / 2;
            int tBg = UiRender.mix(surface(), accent(), toggle);
            UiRender.fillRound(ctx, tx, ty, tw, th, th / 2, tBg);
            int ks = th - 3;
            int kx2 = (int) (tx + 2 + (tw - ks - 4) * toggle);
            UiRender.fillRound(ctx, kx2, ty + 1, ks, ks, ks / 2, text());
            // Inner glow on toggle knob
            UiRender.fillRound(ctx, kx2 + 1, ty + 2, ks - 2, ks - 2, (ks - 2) / 2, UiColor.withAlpha(text(), 30));
        }

        if (modules.isEmpty()) {
            ctx.drawText(textRenderer, "No modules", modX + 20, listY + listH / 2 - 4, textMuted(), false);
        }
        ctx.disableScissor();
    }

    private void drawSettings(DrawContext ctx, int mx, int my, int a) {
        if (settingsModule == null) return;
        int px = winX + winW - (int) settingsFieldX;
        int py = winY + TITLE_H;
        int pw = (int) settingsFieldX;
        int ph = winH - TITLE_H;

        UiRender.fillRound(ctx, px, py, pw, ph, 0, bg3());
        ctx.fill(px, py, px + 2, py + ph, UiColor.withAlpha(accent(), 40));
        ctx.enableScissor(px, py, px + pw, py + ph);

        // Module name + close indicator
        ctx.drawText(textRenderer, settingsModule.getName(), px + 10, py + 10, text(), true);
        ctx.drawText(textRenderer, "Settings", px + 10 + textRenderer.getWidth(settingsModule.getName()) + 6, py + 10, textMuted(), false);

        // Keybind button
        int keyY = py + 26;
        String keyText;
        if (listeningForKey) keyText = "> Press a key...";
        else {
            int kc = settingsModule.getKeyCode();
            keyText = kc >= 0 ? "Key: " + keyName(kc) : "Key: None";
        }
        boolean keyHover = inside(mx, my, px + 8, keyY, pw - 16, 14);
        int keyBg = keyHover ? (listeningForKey ? UiRender.mix(surface2(), accent(), 0.3f) : surface2()) : surface();
        UiRender.fillRound(ctx, px + 8, keyY, pw - 16, 14, 4, keyBg);
        ctx.drawText(textRenderer, keyText, px + 12, keyY + 3, listeningForKey ? accent() : textSoft(), true);

        // Separator
        ctx.fill(px + 8, keyY + 20, px + pw - 8, keyY + 21, surface());

        List<Setting<?>> settings = settingsModule.getSettings();
        int sy = keyY + 28;
        int maxY = py + ph - 10;
        int maxContent = settings.size() * 26 + 4;
        int maxS = Math.max(0, maxContent - (ph - 56));
        float targetScroll = MathHelper.clamp(settingsScrollOffset, 0, maxS);
        settingsScrollAnim.setTarget(targetScroll);
        float smoothSetScroll = settingsScrollAnim.get();

        // Scrollbar indicator
        if (maxS > 0) {
            float progress = maxS > 0 ? settingsScrollOffset / maxS : 0;
            float barH = (float) (ph - 56) / maxContent * (ph - 56);
            barH = Math.max(8, barH);
            float barY = py + 56 + progress * ((ph - 56) - barH);
            UiRender.fillRound(ctx, px + pw - 4, (int) barY, 3, (int) barH, 1, UiColor.withAlpha(accent(), 100));
        }

        if (settings.isEmpty()) {
            ctx.drawText(textRenderer, "No settings", px + 10, sy + 2, textMuted(), false);
        } else {
            for (int i = 0; i < settings.size(); i++) {
                Setting<?> s = settings.get(i);
                int curY = sy + i * 26 - (int) smoothSetScroll;
                if (curY + 24 < keyY + 28 || curY > maxY) continue;
                String sid = settingsModule.getId() + ":" + s.getName();
                settingFadeAnims.computeIfAbsent(sid, k -> new Animation(1.0f, 0.08f)).tick();
                float fade = Animation.easeOutQuart(settingFadeAnims.get(sid).get());
                if (fade < 0.01f) continue;

                int alphaShifted = ((int)(a * fade)) << 24;
                int nameCol = (textSoft() & 0x00FFFFFF) | alphaShifted;
                int surfCol = (surface() & 0x00FFFFFF) | alphaShifted;
                int accentCol = (accent() & 0x00FFFFFF) | alphaShifted;
                int txtCol = (text() & 0x00FFFFFF) | alphaShifted;

                ctx.drawText(textRenderer, s.getName(), px + 8, curY + 1, nameCol, false);

                if (s instanceof BooleanSetting bs) {
                    float anim = bs.enabled() ? 1.0f : 0.0f;
                    int tx = px + pw - 26, ty = curY + 1;
                    int tw2 = 18, th2 = 9;
                    int bgC = UiRender.mix(surfCol, accentCol, anim);
                    UiRender.fillRound(ctx, tx, ty, tw2, th2, th2 / 2, bgC);
                    int ks2 = th2 - 3;
                    int kx2 = (int) (tx + 1 + (tw2 - ks2 - 2) * anim);
                    UiRender.fillRound(ctx, kx2, ty + 1, ks2, ks2, ks2 / 2, txtCol);
                } else if (s instanceof ModeSetting ms) {
                    String val = ms.get();
                    int fw = textRenderer.getWidth(val);
                    UiRender.fillRound(ctx, px + pw - fw - 16, curY + 1, fw + 8, 10, 5, surfCol);
                    ctx.drawText(textRenderer, val, px + pw - fw - 12, curY + 2, accentCol, false);
                } else if (s instanceof DoubleSetting ds) {
                    float p = (float) ((ds.get() - ds.min()) / (ds.max() - ds.min()));
                    int sx = px + 8, sY = curY + 12, sw = pw - 16, sh = 3;
                    UiRender.fillRound(ctx, sx, sY, sw, sh, 1, surfCol);
                    if (p > 0.005f) UiRender.fillRound(ctx, sx, sY, (int) (sw * p), sh, 1, accentCol);
                    // Slider knob
                    int knobX = (int) (sx + sw * p - 3);
                    UiRender.fillRound(ctx, knobX, sY - 2, 6, sh + 4, 3, accentCol);
                    ctx.drawText(textRenderer, String.format("%.1f", ds.get()), px + pw - textRenderer.getWidth(String.format("%.1f", ds.get())) - 8, curY + 1, accentCol, false);
                } else if (s instanceof IntegerSetting is) {
                    float p = (float) (is.get() - is.min()) / (is.max() - is.min());
                    int sx = px + 8, sY = curY + 12, sw = pw - 16, sh = 3;
                    UiRender.fillRound(ctx, sx, sY, sw, sh, 1, surfCol);
                    if (p > 0.005f) UiRender.fillRound(ctx, sx, sY, (int) (sw * p), sh, 1, accentCol);
                    int knobX = (int) (sx + sw * p - 3);
                    UiRender.fillRound(ctx, knobX, sY - 2, 6, sh + 4, 3, accentCol);
                    ctx.drawText(textRenderer, String.valueOf(is.get()), px + pw - textRenderer.getWidth(String.valueOf(is.get())) - 8, curY + 1, accentCol, false);
                }
            }
        }
        ctx.disableScissor();
    }

    private void drawPresetsContent(DrawContext ctx, int mx, int my, int x, int y, int w, int h) {
        ctx.drawText(textRenderer, "Anticheat Presets", x + 10, y + 10, text(), true);
        ctx.drawText(textRenderer, "Click to apply bypass preset to all modules", x + 10, y + 22, textMuted(), false);

        String[][] presets = {
            {"GrimAC", "Grim anticheat bypass profile"},
            {"Vulcan", "Vulcan anticheat bypass profile"},
            {"Polar", "Polar anticheat bypass profile"},
            {"Vanilla", "No bypass (raw mode)"},
            {"Legit", "Human-like safe settings"}
        };
        int py = y + 40;
        for (String[] preset : presets) {
            boolean hovered = inside(mx, my, x + 10, py, w - 40, 22);
            int bgC = hovered ? surface2() : surface();
            UiRender.fillRound(ctx, x + 10, py, w - 40, 22, 4, bgC);
            ctx.drawText(textRenderer, preset[0], x + 18, py + 6, hovered ? accent() : textSoft(), true);
            if (hovered) {
                tooltipText = preset[1];
                tooltipX = mx + 10;
                tooltipY = my;
            }
            py += 26;
        }
    }

    private void drawFriendsContent(DrawContext ctx, int mx, int my, int x, int y, int w, int h) {
        ctx.drawText(textRenderer, "Friends", x + 10, y + 10, text(), true);

        int inputH = 16;
        boolean inputHover = inside(mx, my, x + 10, y + 26, w - 40, inputH);
        UiRender.fillRound(ctx, x + 10, y + 26, w - 40, inputH, 4, inputHover ? surface2() : surface());
        String display = friendInputFocused && friendInput.isEmpty() ? "" : friendInput;
        String show = display.isEmpty() && !friendInputFocused ? "Add friend... (Enter)" : display;
        ctx.drawText(textRenderer, show, x + 14, y + 28, textMuted(), false);

        ctx.drawText(textRenderer, "Friend List:", x + 10, y + 50, textSoft(), true);
        var friends = dev.falaris.client.FalarisClient.getInstance().getFriendsManager().getAll();
        int fy = y + 64;
        for (String friend : friends) {
            boolean nameHover = inside(mx, my, x + 14, fy, w - 40, 12);
            boolean removeHover = inside(mx, my, x + w - 60, fy, 14, 12);
            ctx.drawText(textRenderer, "\u2605 " + friend, x + 14, fy, green(), true);
            if (nameHover || removeHover) {
                ctx.drawText(textRenderer, "\u2716", x + w - 60, fy, red(), true);
                if (removeHover) {
                    tooltipText = "Remove friend";
                    tooltipX = mx + 10;
                    tooltipY = my;
                }
            }
            fy += 14;
        }
        if (friends.isEmpty()) {
            ctx.drawText(textRenderer, "No friends added", x + 14, fy + 4, textMuted(), false);
        }
    }

    private void drawIgnoresContent(DrawContext ctx, int mx, int my, int x, int y, int w, int h) {
        ctx.drawText(textRenderer, "Ignores", x + 10, y + 10, text(), true);

        int inputH = 16;
        boolean inputHover = inside(mx, my, x + 10, y + 26, w - 40, inputH);
        UiRender.fillRound(ctx, x + 10, y + 26, w - 40, inputH, 4, inputHover ? surface2() : surface());
        String display = ignoreInputFocused && ignoreInput.isEmpty() ? "" : ignoreInput;
        String show = display.isEmpty() && !ignoreInputFocused ? "Add ignore... (Enter)" : display;
        ctx.drawText(textRenderer, show, x + 14, y + 28, textMuted(), false);

        ctx.drawText(textRenderer, "Ignore List:", x + 10, y + 50, textSoft(), true);
        var ignores = dev.falaris.client.FalarisClient.getInstance().getIgnoresManager().getAll();
        int fy = y + 64;
        for (String ignore : ignores) {
            boolean nameHover = inside(mx, my, x + 14, fy, w - 40, 12);
            boolean removeHover = inside(mx, my, x + w - 60, fy, 14, 12);
            ctx.drawText(textRenderer, "\u26D4 " + ignore, x + 14, fy, red(), true);
            if (nameHover || removeHover) {
                ctx.drawText(textRenderer, "\u2716", x + w - 60, fy, red(), true);
                if (removeHover) {
                    tooltipText = "Remove ignore";
                    tooltipX = mx + 10;
                    tooltipY = my;
                }
            }
            fy += 14;
        }
        if (ignores.isEmpty()) {
            ctx.drawText(textRenderer, "No ignores added", x + 14, fy + 4, textMuted(), false);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int btn = click.button();

        // Resize handle
        if (btn == 0 && inside(mx, my, winX + winW - RESIZE_H, winY + winH - RESIZE_H, RESIZE_H, RESIZE_H)) {
            resizing = true;
            resizeOffX = (int) mx - winX;
            resizeOffY = (int) my - winY;
            return true;
        }

        // Drag title bar
        if (btn == 0 && inside(mx, my, winX, winY, winW, TITLE_H)) {
            dragging = true;
            dragOffX = (int) mx - winX;
            dragOffY = (int) my - winY;
            return true;
        }

        // Close button
        if (btn == 0 && inside(mx, my, winX + winW - 24, winY + 8, 18, 18)) {
            close();
            return true;
        }

        // Settings panel interactions
        if (settingsFieldX > 5) {
            int px = winX + winW - (int) settingsFieldX;
            int py = winY + TITLE_H;
            int pw = (int) settingsFieldX;
            if (inside(mx, my, px, py, pw, winH - TITLE_H)) {
                int keyY = py + 24;
                if (inside(mx, my, px + 8, keyY, pw - 16, 12)) {
                    listeningForKey = !listeningForKey;
                    if (listeningForKey) listeningKeyText = "";
                    return true;
                }
                int sy = py + 44;
                for (Setting<?> s : settingsModule.getSettings()) {
                    if (inside(mx, my, px + 8, sy, pw - 16, 22)) {
                        handleSetting(s, mx, my, btn, px + 8, pw - 16);
                        return true;
                    }
                    sy += 24;
                }
                return true;
            }
        }

        // Sidebar item click
        int sx = winX;
        int sy = winY + TITLE_H;
        int sidebarH = winH - TITLE_H;
        int contentH = SIDEBAR_ITEMS.length * CAT_ITEM_H;
        int maxScroll = Math.max(0, contentH - sidebarH);
        float smooth = MathHelper.clamp(scrollOffset, 0, maxScroll);

        for (int i = 0; i < SIDEBAR_ITEMS.length; i++) {
            int cy = sy + 8 + i * CAT_ITEM_H - (int) smooth;
            if (inside(mx, my, sx + 4, cy, SIDEBAR_W - 8, CAT_ITEM_H - 4)) {
                String item = SIDEBAR_ITEMS[i];
                if (!item.equals(selectedCategory)) {
                    selectedCategory = item;
                    settingsModule = null;
                    scrollOffset = 0;
                    cacheDirty = true;
                }
                return true;
            }
        }

        // Module list interactions (only for real categories)
        if (!isVirtual(selectedCategory)) {
            int modX = winX + SIDEBAR_W;
            int modY = winY + TITLE_H;
            int modW = winW - SIDEBAR_W - (int) settingsFieldX;
            int listY = modY + 28;
            List<Module> modules = getModules();
            int listH = winH - TITLE_H - 22;
            int modContentH = modules.size() * (MOD_ITEM_H + MOD_GAP);
            int modMaxScroll = Math.max(0, modContentH - listH);
            float modTargetScroll = MathHelper.clamp(scrollOffset, 0, modMaxScroll);
            scrollAnim.setTarget(modTargetScroll);

            for (int i = 0; i < modules.size(); i++) {
                Module mod = modules.get(i);
                int by = listY + i * (MOD_ITEM_H + MOD_GAP) - (int) scrollAnim.get();
                if (by + MOD_ITEM_H < listY || by > listY + listH) continue;

                if (inside(mx, my, modX + 8, by, modW - 16, MOD_ITEM_H)) {
                    if (btn == 0) {
                        mod.toggle();
                        moduleManager.save();
                    } else if (btn == 1) {
                        // Right-click: open inline settings
                        settingsModule = settingsModule == mod ? null : mod;
                        settingsScrollOffset = 0;
                    }
                    return true;
                }
            }

            // Presets content clicks
        } else if (VIRTUAL_PRESETS.equals(selectedCategory)) {
            String[][] presets = {{"GrimAC"}, {"Vulcan"}, {"Polar"}, {"Vanilla"}, {"Legit"}};
            int py = winY + TITLE_H + 40;
            for (int i = 0; i < presets.length; i++) {
                if (inside(mx, my, winX + SIDEBAR_W + 10, py, winW - SIDEBAR_W - (int) settingsFieldX - 40, 22)) {
                    applyAnticheatPreset(presets[i][0]);
                    return true;
                }
                py += 26;
            }

        } else if (VIRTUAL_FRIENDS.equals(selectedCategory)) {
            int inputY = winY + TITLE_H + 26;
            if (inside(mx, my, winX + SIDEBAR_W + 10, inputY, winW - SIDEBAR_W - (int) settingsFieldX - 40, 16)) {
                friendInputFocused = !friendInputFocused;
                return true;
            }
            // Remove friend on hover × click
            var friends = dev.falaris.client.FalarisClient.getInstance().getFriendsManager().getAll();
            int fy = winY + TITLE_H + 64;
            for (int i = 0; i < friends.size(); i++) {
                String friend = friends.get(i);
                int nameEnd = winX + SIDEBAR_W + (int) settingsFieldX + winW - SIDEBAR_W - 60;
                if (inside(mx, my, nameEnd, fy, 14, 12)) {
                    dev.falaris.client.FalarisClient.getInstance().getFriendsManager().remove(friend);
                    dev.falaris.client.FalarisClient.getInstance().getConfigManager().saveFriends(
                        dev.falaris.client.FalarisClient.getInstance().getFriendsManager());
                    return true;
                }
                fy += 14;
            }

        } else if (VIRTUAL_IGNORES.equals(selectedCategory)) {
            int inputY = winY + TITLE_H + 26;
            if (inside(mx, my, winX + SIDEBAR_W + 10, inputY, winW - SIDEBAR_W - (int) settingsFieldX - 40, 16)) {
                ignoreInputFocused = !ignoreInputFocused;
                return true;
            }
            // Remove ignore on hover × click
            var ignores = dev.falaris.client.FalarisClient.getInstance().getIgnoresManager().getAll();
            int fy = winY + TITLE_H + 64;
            for (int i = 0; i < ignores.size(); i++) {
                String ignore = ignores.get(i);
                int nameEnd = winX + SIDEBAR_W + (int) settingsFieldX + winW - SIDEBAR_W - 60;
                if (inside(mx, my, nameEnd, fy, 14, 12)) {
                    dev.falaris.client.FalarisClient.getInstance().getIgnoresManager().remove(ignore);
                    dev.falaris.client.FalarisClient.getInstance().getConfigManager().saveIgnores(
                        dev.falaris.client.FalarisClient.getInstance().getIgnoresManager());
                    return true;
                }
                fy += 14;
            }
        }

        return true;
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.gui.Click click) {
        dragging = false;
        resizing = false;
        if (draggingSlider && draggingSetting != null) moduleManager.save();
        draggingSlider = false;
        draggingSetting = null;
        return true;
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.gui.Click click, double dx, double dy) {
        if (dragging) {
            int rawX = (int) click.x() - dragOffX;
            int rawY = (int) click.y() - dragOffY;
            winX = Math.round(rawX / (float) GRID_SNAP) * GRID_SNAP;
            winY = Math.round(rawY / (float) GRID_SNAP) * GRID_SNAP;
            return true;
        }
        if (resizing) {
            int rawW = (int) click.x() - winX + (winW - resizeOffX);
            int rawH = (int) click.y() - winY + (winH - resizeOffY);
            winW = Math.max(MIN_W, Math.round(rawW / (float) GRID_SNAP) * GRID_SNAP);
            winH = Math.max(MIN_H, Math.round(rawH / (float) GRID_SNAP) * GRID_SNAP);
            return true;
        }
        if (draggingSlider) {
            if (draggingSetting instanceof DoubleSetting ds) updateSlider(click.x(), ds);
            else if (draggingSetting instanceof IntegerSetting is) updateSlider(click.x(), is);
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horiz, double vert) {
        int modX = winX + SIDEBAR_W;
        int modW = winW - SIDEBAR_W - (int) settingsFieldX;
        if (mx > winX && mx < winX + SIDEBAR_W) {
            // Sidebar scroll
            int contentH = SIDEBAR_ITEMS.length * CAT_ITEM_H;
            int visH = winH - TITLE_H;
            int max = Math.max(0, contentH - visH);
            scrollOffset = MathHelper.clamp(scrollOffset - (float) vert * 30.0f, 0, max);
        } else if (mx > modX && mx < modX + modW) {
            if (isVirtual(selectedCategory)) {
                // No scroll for virtual pages
            } else {
                int listH = winH - TITLE_H - 22;
                int contentH = getModules().size() * (MOD_ITEM_H + MOD_GAP);
                int max = Math.max(0, contentH - listH);
                scrollOffset = MathHelper.clamp(scrollOffset - (float) vert * 24.0f, 0, max);
            }
        } else if (settingsFieldX > 5 && mx > winX + winW - (int) settingsFieldX) {
            int max = Math.max(0, settingsModule.getSettings().size() * 24 - (winH - TITLE_H - 50));
            settingsScrollOffset = MathHelper.clamp(settingsScrollOffset - (float) vert * 18.0f, 0, max);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput key) {
        if (friendInputFocused) {
            if (key.key() == 259 && !friendInput.isEmpty()) {
                friendInput = friendInput.substring(0, friendInput.length() - 1);
                return true;
            }
            if (key.key() == 257 && !friendInput.isEmpty()) {
                dev.falaris.client.FalarisClient.getInstance().getFriendsManager().add(friendInput);
                var fm = dev.falaris.client.FalarisClient.getInstance().getFriendsManager();
                dev.falaris.client.FalarisClient.getInstance().getConfigManager().saveFriends(fm);
                friendInput = "";
                friendInputFocused = false;
                return true;
            }
            return true;
        }

        if (ignoreInputFocused) {
            if (key.key() == 259 && !ignoreInput.isEmpty()) {
                ignoreInput = ignoreInput.substring(0, ignoreInput.length() - 1);
                return true;
            }
            if (key.key() == 257 && !ignoreInput.isEmpty()) {
                dev.falaris.client.FalarisClient.getInstance().getIgnoresManager().add(ignoreInput);
                var im = dev.falaris.client.FalarisClient.getInstance().getIgnoresManager();
                dev.falaris.client.FalarisClient.getInstance().getConfigManager().saveIgnores(im);
                ignoreInput = "";
                ignoreInputFocused = false;
                return true;
            }
            return true;
        }

        if (listeningForKey && settingsModule != null) {
            int kc = key.key();
            if (kc == 256) {
                listeningForKey = false;
                settingsModule.setKeyCode(-1);
                moduleManager.save();
                return true;
            }
            settingsModule.setKeyCode(kc);
            moduleManager.save();
            listeningForKey = false;
            return true;
        }

        if (key.key() == 256) {
            if (searchFocused && !searchQuery.isEmpty()) { searchQuery = ""; cacheDirty = true; return true; }
            if (searchFocused) { searchFocused = false; return true; }
            close();
            return true;
        }
        if (searchFocused) {
            if (key.key() == 259 && !searchQuery.isEmpty()) { searchQuery = searchQuery.substring(0, searchQuery.length() - 1); cacheDirty = true; return true; }
            return true;
        }
        return super.keyPressed(key);
    }

    @Override
    public boolean charTyped(CharInput ch) {
        if (friendInputFocused) {
            String c = new String(Character.toChars(ch.codepoint()));
            if (!c.isEmpty()) friendInput += c;
            return true;
        }
        if (ignoreInputFocused) {
            String c = new String(Character.toChars(ch.codepoint()));
            if (!c.isEmpty()) ignoreInput += c;
            return true;
        }
        if (searchFocused) {
            String c = new String(Character.toChars(ch.codepoint()));
            if (!c.isEmpty()) { searchQuery += c; cacheDirty = true; return true; }
        }
        return super.charTyped(ch);
    }

    @Override
    public void close() {
        dragging = false;
        settingsModule = null;
        super.close();
    }

    private void handleSetting(Setting<?> s, double mx, double my, int btn, int setX, int setW) {
        if (s instanceof BooleanSetting bs) {
            bs.set(!bs.enabled());
            moduleManager.save();
        } else if (s instanceof ModeSetting ms) {
            List<String> modes = ms.modes();
            int idx = modes.indexOf(ms.get());
            idx = btn == 1 ? idx - 1 : idx + 1;
            if (idx < 0) idx = modes.size() - 1;
            if (idx >= modes.size()) idx = 0;
            ms.set(modes.get(idx));
            moduleManager.save();
        } else if (s instanceof DoubleSetting ds) {
            draggingSlider = true;
            draggingSetting = s;
            updateSlider(mx, ds);
            moduleManager.save();
        } else if (s instanceof IntegerSetting is) {
            draggingSlider = true;
            draggingSetting = s;
            updateSlider(mx, is);
            moduleManager.save();
        }
    }

    private void updateSlider(double mx, DoubleSetting ds) {
        int[] info = findSettingInfo(ds);
        if (info == null) return;
        float p = (float) MathHelper.clamp((mx - info[0] - 2) / info[1], 0.0, 1.0);
        ds.set(ds.min() + (ds.max() - ds.min()) * p);
    }

    private void updateSlider(double mx, IntegerSetting is) {
        int[] info = findSettingInfo(is);
        if (info == null) return;
        float p = (float) MathHelper.clamp((mx - info[0] - 2) / info[1], 0.0, 1.0);
        is.set((int) Math.round(is.min() + (is.max() - is.min()) * p));
    }

    private int[] findSettingInfo(Setting<?> target) {
        if (settingsModule == null) return null;
        int px = winX + winW - (int) settingsFieldX;
        int pw = (int) settingsFieldX;
        for (Setting<?> s : settingsModule.getSettings()) {
            if (s == target) return new int[]{px + 8, pw - 16};
        }
        return null;
    }

    private void applyAnticheatPreset(String name) {
        for (var mod : moduleManager.getModules()) {
            for (var setting : mod.getSettings()) {
                if (setting.getName().equals("Bypass") && setting instanceof ModeSetting ms) {
                    ms.set(name);
                }
            }
        }
        moduleManager.save();
    }

    private List<Module> getModules() {
        if (!cacheDirty) return cachedModules;
        Category cat = null;
        try { cat = Category.valueOf(selectedCategory); } catch (Exception ignored) {}
        cachedModules = moduleManager.search(searchQuery, cat);
        cacheDirty = false;
        return cachedModules;
    }

    private static boolean inside(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static int argb(int a, int r, int g, int b) {
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

    private static String keyName(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_UNKNOWN -> "Unknown";
            case GLFW.GLFW_KEY_SPACE -> "Space";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LShift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RShift";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCtrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCtrl";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LAlt";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RAlt";
            case GLFW.GLFW_KEY_ESCAPE -> "Esc";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_ENTER -> "Enter";
            case GLFW.GLFW_KEY_UP -> "Up";
            case GLFW.GLFW_KEY_DOWN -> "Down";
            case GLFW.GLFW_KEY_LEFT -> "Left";
            case GLFW.GLFW_KEY_RIGHT -> "Right";
            case GLFW.GLFW_KEY_DELETE -> "Del";
            case GLFW.GLFW_KEY_BACKSPACE -> "Bksp";
            default -> {
                String name = GLFW.glfwGetKeyName(keyCode, 0);
                if (name != null && !name.isEmpty()) yield name.toUpperCase();
                char c = (char) keyCode;
                if (c >= 'A' && c <= 'Z') yield String.valueOf(c);
                if (c >= '0' && c <= '9') yield String.valueOf(c);
                yield "Key" + keyCode;
            }
        };
    }
}
