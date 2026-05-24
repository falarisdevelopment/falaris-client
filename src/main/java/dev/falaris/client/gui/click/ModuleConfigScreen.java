package dev.falaris.client.gui.click;

import dev.falaris.client.module.Module;
import dev.falaris.client.module.ModuleManager;
import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import dev.falaris.client.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class ModuleConfigScreen extends Screen {
    private static final int W = 320;
    private static final int H = 300;

    private final ModuleManager moduleManager;
    private final Module module;
    private final Screen parent;

    private int gx, gy;
    private boolean dragging;
    private int dragOffX, dragOffY;
    private float scrollOffset;
    private boolean listeningForKey;
    private Setting<?> draggingSetting;
    private boolean draggingSlider;

    private static final int BG = 0xFF18181C;
    private static final int BG2 = 0xFF24262E;
    private static final int SURFACE = 0xFF282A34;
    private static final int SURFACE2 = 0xFF32323E;
    private static final int ACCENT = 0xFF54B8B3;
    private static final int TEXT = 0xFFDCE1EB;
    private static final int SOFT = 0xFFA5AABE;
    private static final int MUTED = 0xFF6E738C;

    public ModuleConfigScreen(ModuleManager moduleManager, Module module, Screen parent) {
        super(Text.literal(module.getName()));
        this.moduleManager = moduleManager;
        this.module = module;
        this.parent = parent;
        center();
    }

    private void center() {
        gx = (width - W) / 2;
        gy = (height - H) / 2;
    }

    @Override
    protected void init() { center(); }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);

        ctx.fill(gx, gy, gx + W, gy + H, BG);
        ctx.fill(gx, gy, gx + W, gy + 28, BG2);

        ctx.drawText(textRenderer, Text.literal(module.getName()), gx + 10, gy + 8, ACCENT, false);

        boolean closeHover = inside(mx, my, gx + W - 20, gy + 4, 16, 16);
        ctx.drawText(textRenderer, "\u2715", gx + W - 16, gy + 8, closeHover ? ACCENT : SOFT, false);

        int keyY = gy + 32;
        String keyText;
        if (listeningForKey) {
            keyText = "> Press a key...";
        } else {
            int kc = module.getKeyCode();
            keyText = kc >= 0 ? "Key: " + keyName(kc) : "Key: None";
        }
        boolean keyHover = inside(mx, my, gx + 8, keyY, W - 16, 14);
        ctx.fill(gx + 8, keyY, gx + 8 + W - 16, keyY + 14, keyHover ? SURFACE2 : SURFACE);
        ctx.drawText(textRenderer, keyText, gx + 12, keyY + 3, listeningForKey ? ACCENT : SOFT, false);

        List<Setting<?>> settings = module.getSettings();
        int sy = gy + 52;
        int contentH = settings.size() * 24;
        int maxScroll = Math.max(0, contentH - (H - 60));
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);

        for (int i = 0; i < settings.size(); i++) {
            Setting<?> s = settings.get(i);
            int curY = sy + i * 24 - (int) scrollOffset;
            if (curY + 22 < gy + 52 || curY > gy + H - 10) continue;

            ctx.drawText(textRenderer, s.getName(), gx + 8, curY + 1, SOFT, false);

            if (s instanceof BooleanSetting bs) {
                int tw = 18, th = 9;
                int tx = gx + W - tw - 10, ty = curY + 1;
                ctx.fill(tx, ty, tx + tw, ty + th, bs.enabled() ? ACCENT : SURFACE);
                int ks = th - 2;
                int kx = bs.enabled() ? tx + tw - ks - 1 : tx + 1;
                ctx.fill(kx, ty + 1, kx + ks, ty + ks + 1, TEXT);
            } else if (s instanceof ModeSetting ms) {
                String val = ms.get();
                int fw = textRenderer.getWidth(val);
                ctx.fill(gx + W - fw - 18, curY + 1, gx + W - 10, curY + 11, SURFACE);
                ctx.drawText(textRenderer, val, gx + W - fw - 14, curY + 2, ACCENT, false);
            } else if (s instanceof DoubleSetting ds) {
                float p = (float) ((ds.get() - ds.min()) / (ds.max() - ds.min()));
                int sx = gx + 8, sY = curY + 12, sw = W - 16, sh = 2;
                ctx.fill(sx, sY, sx + sw, sY + sh, SURFACE);
                if (p > 0.005f) {
                    ctx.fill(sx, sY, (int) (sx + sw * p), sY + sh, ACCENT);
                }
                String val = String.format("%.1f", ds.get());
                ctx.drawText(textRenderer, val, gx + W - textRenderer.getWidth(val) - 10, curY + 1, ACCENT, false);
            } else if (s instanceof IntegerSetting is) {
                float p = (float) (is.get() - is.min()) / (is.max() - is.min());
                int sx = gx + 8, sY = curY + 12, sw = W - 16, sh = 2;
                ctx.fill(sx, sY, sx + sw, sY + sh, SURFACE);
                if (p > 0.005f) {
                    ctx.fill(sx, sY, (int) (sx + sw * p), sY + sh, ACCENT);
                }
                String val = String.valueOf(is.get());
                ctx.drawText(textRenderer, val, gx + W - textRenderer.getWidth(val) - 10, curY + 1, ACCENT, false);
            }
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x50000000);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int btn = click.button();

        if (btn == 0 && inside(mx, my, gx, gy, W, 28)) {
            dragging = true;
            dragOffX = (int) mx - gx;
            dragOffY = (int) my - gy;
            return true;
        }

        if (inside(mx, my, gx + W - 20, gy + 4, 16, 16)) {
            close();
            return true;
        }

        if (inside(mx, my, gx + 8, gy + 32, W - 16, 14)) {
            listeningForKey = !listeningForKey;
            return true;
        }

        List<Setting<?>> settings = module.getSettings();
        int sy = gy + 52;
        for (int i = 0; i < settings.size(); i++) {
            Setting<?> s = settings.get(i);
            int curY = sy + i * 24 - (int) scrollOffset;
            if (inside(mx, my, gx + 8, curY, W - 16, 22)) {
                handleSetting(s, mx, my, btn);
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.gui.Click click) {
        dragging = false;
        draggingSlider = false;
        draggingSetting = null;
        return true;
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.gui.Click click, double dx, double dy) {
        if (dragging) {
            gx = (int) click.x() - dragOffX;
            gy = (int) click.y() - dragOffY;
            return true;
        }
        if (draggingSlider) {
            if (draggingSetting instanceof DoubleSetting ds) {
                updateSlider(click.x(), ds);
            } else if (draggingSetting instanceof IntegerSetting is) {
                updateSlider(click.x(), is);
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horiz, double vert) {
        int max = Math.max(0, module.getSettings().size() * 24 - (H - 60));
        scrollOffset = MathHelper.clamp(scrollOffset - (float) vert * 18.0f, 0, max);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput key) {
        if (listeningForKey) {
            int kc = key.key();
            if (kc == 256) {
                listeningForKey = false;
                module.setKeyCode(-1);
                moduleManager.save();
                return true;
            }
            module.setKeyCode(kc);
            moduleManager.save();
            listeningForKey = false;
            return true;
        }
        if (key.key() == 256) {
            close();
            return true;
        }
        return super.keyPressed(key);
    }

    @Override
    public void close() {
        moduleManager.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    private void handleSetting(Setting<?> s, double mx, double my, int btn) {
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
        } else if (s instanceof IntegerSetting is) {
            draggingSlider = true;
            draggingSetting = s;
            updateSlider(mx, is);
        }
    }

    private void updateSlider(double mx, DoubleSetting ds) {
        float p = (float) MathHelper.clamp((mx - (gx + 10)) / (W - 20), 0.0, 1.0);
        ds.set(ds.min() + (ds.max() - ds.min()) * p);
    }

    private void updateSlider(double mx, IntegerSetting is) {
        float p = (float) MathHelper.clamp((mx - (gx + 10)) / (W - 20), 0.0, 1.0);
        is.set((int) Math.round(is.min() + (is.max() - is.min()) * p));
    }

    private static boolean inside(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
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
