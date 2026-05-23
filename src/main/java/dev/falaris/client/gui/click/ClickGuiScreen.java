package dev.falaris.client.gui.click;

import dev.falaris.client.module.Category;
import dev.falaris.client.module.Module;
import dev.falaris.client.module.ModuleManager;
import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import dev.falaris.client.setting.Setting;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClickGuiScreen extends Screen {
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 360;
    private static final int CATEGORY_SELECTOR_HEIGHT = 28;
    private static final int MODULE_CARD_HEIGHT = 34;
    private static final int MODULE_CARD_SPACING = 10;
    private static final int MODULE_COLUMNS = 2;
    private static final int MODULE_LIST_ROWS = 4;

    private static final int PANEL_BG = UiColor.rgb(29, 32, 48);
    private static final int HEADER_BG = UiColor.rgb(37, 40, 57);
    private static final int CARD_BG = UiColor.rgb(47, 50, 70);
    private static final int CARD_HOVER = UiColor.rgb(61, 64, 90);
    private static final int ACCENT = UiColor.rgb(189, 147, 249);
    private static final int TEXT = UiColor.rgb(225, 228, 238);
    private static final int TEXT_SOFT = UiColor.rgb(180, 185, 210);
    private static final int TEXT_FAINT = UiColor.rgb(145, 150, 180);

    private final ModuleManager moduleManager;
    private final Animation openAnimation = new Animation(0.0f);
    private final Map<String, Animation> moduleAnimations = new HashMap<>();

    private Category selectedCategory = Category.COMBAT;
    private Module selectedModule;
    private List<Module> visibleModules = List.of();

    private int panelX;
    private int panelY;

    public ClickGuiScreen(ModuleManager moduleManager) {
        super(Text.literal("FALARIS"));
        this.moduleManager = moduleManager;
        openAnimation.setTarget(1.0f);
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;
        updateVisibleModules();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void tick() {
        openAnimation.tick(0.2f);
        for (Module module : moduleManager.getModules()) {
            Animation animation = moduleAnimations.computeIfAbsent(module.getId(), id -> new Animation(module.isEnabled() ? 1.0f : 0.0f));
            animation.setTarget(module.isEnabled() ? 1.0f : 0.0f);
            animation.tick(0.25f);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float open = easeOut(openAnimation.get());
        if (open < 0.01f) {
            return;
        }

        int alpha = (int) (open * 255);
        int x = panelX;
        int y = panelY;

        UiRender.fillRound(context, x + 4, y + 6, PANEL_WIDTH, PANEL_HEIGHT, 12, UiColor.argb((int) (alpha * 0.4f), 0, 0, 0));
        UiRender.fillRound(context, x, y, PANEL_WIDTH, PANEL_HEIGHT, 12, UiColor.argb(alpha, 29, 32, 48));

        UiRender.fillRound(context, x + 10, y + 10, PANEL_WIDTH - 20, 32, 8, UiColor.argb(alpha, 37, 40, 57));
        context.drawText(textRenderer, "FALARIS", x + 20, y + 18, TEXT, false);
        context.drawText(textRenderer, selectedCategory.getDisplayName(), x + PANEL_WIDTH - 115, y + 18, ACCENT, false);

        renderCategorySelector(context, x, y, alpha, mouseX, mouseY);
        renderModules(context, x, y, alpha, mouseX, mouseY);
        renderSelectedModule(context, x, y, alpha);

        context.drawText(textRenderer, "Left click to toggle · Right click to inspect", x + 18, y + PANEL_HEIGHT - 18, UiColor.rgb(140, 145, 170), false);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderCategorySelector(DrawContext context, int x, int y, int alpha, int mouseX, int mouseY) {
        int selectorX = x + 20;
        int selectorY = y + 48;
        int selectorW = PANEL_WIDTH - 40;
        int selectorH = CATEGORY_SELECTOR_HEIGHT;

        UiRender.fillRound(context, selectorX, selectorY, selectorW, selectorH, 8, UiColor.argb(alpha, 34, 38, 61));

        int arrowSize = 14;
        int leftX = selectorX + 8;
        int rightX = selectorX + selectorW - arrowSize - 8;
        boolean leftHover = inside(mouseX, mouseY, leftX, selectorY + 6, arrowSize, arrowSize);
        boolean rightHover = inside(mouseX, mouseY, rightX, selectorY + 6, arrowSize, arrowSize);
        int arrowColor = (leftHover || rightHover) ? TEXT : TEXT_FAINT;

        context.drawText(textRenderer, "<", leftX, selectorY + 6, arrowColor, false);
        context.drawText(textRenderer, ">", rightX, selectorY + 6, arrowColor, false);

        int nameX = selectorX + (selectorW - textRenderer.getWidth(selectedCategory.getDisplayName())) / 2;
        context.drawText(textRenderer, selectedCategory.getDisplayName(), nameX, selectorY + 6, TEXT, false);
    }

    private void renderModules(DrawContext context, int x, int y, int alpha, int mouseX, int mouseY) {
        int contentX = x + 20;
        int contentY = y + 86;
        int contentW = PANEL_WIDTH - 40;
        int columnWidth = (contentW - MODULE_CARD_SPACING) / MODULE_COLUMNS;
        int visibleCount = Math.min(MODULE_LIST_ROWS * MODULE_COLUMNS, visibleModules.size());

        if (visibleModules.isEmpty()) {
            context.drawText(textRenderer, "No modules in this category.", contentX + 6, contentY + 10, UiColor.rgb(160, 165, 185), false);
            return;
        }

        for (int index = 0; index < visibleCount; index++) {
            Module module = visibleModules.get(index);
            int col = index % MODULE_COLUMNS;
            int row = index / MODULE_COLUMNS;
            int cardX = contentX + col * (columnWidth + MODULE_CARD_SPACING);
            int cardY = contentY + row * (MODULE_CARD_HEIGHT + MODULE_CARD_SPACING);
            boolean hovered = inside(mouseX, mouseY, cardX, cardY, columnWidth, MODULE_CARD_HEIGHT);
            int cardColor = hovered ? CARD_HOVER : CARD_BG;
            UiRender.fillRound(context, cardX, cardY, columnWidth, MODULE_CARD_HEIGHT, 10, UiColor.argb(alpha, (cardColor >> 16) & 0xff, (cardColor >> 8) & 0xff, cardColor & 0xff));
            UiRender.fillRound(context, cardX + 8, cardY + 8, 8, 8, 4, UiColor.argb(alpha, 189, 147, 249));

            float enabledAnim = moduleAnimations.computeIfAbsent(module.getId(), id -> new Animation(module.isEnabled() ? 1.0f : 0.0f)).get();
            int labelColor = module.isEnabled() ? TEXT : TEXT_SOFT;
            context.drawText(textRenderer, module.getName(), cardX + 22, cardY + 8, labelColor, false);

            String state = module.isEnabled() ? "ON" : "OFF";
            int stateColor = module.isEnabled() ? UiColor.rgb(163, 190, 140) : UiColor.rgb(150, 155, 170);
            context.drawText(textRenderer, state, cardX + columnWidth - textRenderer.getWidth(state) - 10, cardY + 8, stateColor, false);
        }

        if (visibleModules.size() > MODULE_LIST_ROWS * MODULE_COLUMNS) {
            String more = "+" + (visibleModules.size() - MODULE_LIST_ROWS * MODULE_COLUMNS) + " more";
            context.drawText(textRenderer, more, contentX + contentW - textRenderer.getWidth(more), contentY + MODULE_LIST_ROWS * (MODULE_CARD_HEIGHT + MODULE_CARD_SPACING), TEXT_FAINT, false);
        }
    }

    private void renderSelectedModule(DrawContext context, int x, int y, int alpha) {
        int infoX = x + 20;
        int infoY = y + 86 + MODULE_LIST_ROWS * (MODULE_CARD_HEIGHT + MODULE_CARD_SPACING);
        int infoW = PANEL_WIDTH - 40;
        int infoH = PANEL_HEIGHT - (infoY - y) - 28;

        UiRender.fillRound(context, infoX, infoY, infoW, infoH, 10, UiColor.argb(alpha, 30, 33, 52));

        if (selectedModule == null) {
            context.drawText(textRenderer, "Right click a module to inspect it.", infoX + 10, infoY + 12, UiColor.rgb(160, 165, 185), false);
            return;
        }

        context.drawText(textRenderer, selectedModule.getName(), infoX + 10, infoY + 10, TEXT, false);
        context.drawText(textRenderer, selectedModule.getDescription(), infoX + 10, infoY + 26, TEXT_FAINT, false);

        List<Setting<?>> settings = selectedModule.getSettings();
        int lineY = infoY + 48;
        int shown = 0;
        for (Setting<?> setting : settings) {
            if (shown >= 3) {
                break;
            }
            String line = setting.getName() + ": " + formatSetting(setting);
            context.drawText(textRenderer, line, infoX + 10, lineY, TEXT_SOFT, false);
            lineY += 12;
            shown++;
        }

        if (settings.isEmpty()) {
            context.drawText(textRenderer, "No adjustable settings for this module.", infoX + 10, lineY, UiColor.rgb(160, 165, 185), false);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        int selectorX = panelX + 20;
        int selectorY = panelY + 48;
        int selectorW = PANEL_WIDTH - 40;
        int arrowSize = 14;
        int leftX = selectorX + 8;
        int rightX = selectorX + selectorW - arrowSize - 8;

        if (inside(mouseX, mouseY, leftX, selectorY + 6, arrowSize, arrowSize)) {
            previousCategory();
            return true;
        }
        if (inside(mouseX, mouseY, rightX, selectorY + 6, arrowSize, arrowSize)) {
            nextCategory();
            return true;
        }

        int contentX = panelX + 20;
        int contentY = panelY + 86;
        int contentW = PANEL_WIDTH - 40;
        int columnWidth = (contentW - MODULE_CARD_SPACING) / MODULE_COLUMNS;
        
        for (int index = 0; index < visibleModules.size(); index++) {
            int col = index % MODULE_COLUMNS;
            int row = index / MODULE_COLUMNS;
            if (row >= MODULE_LIST_ROWS) break;
            
            int cardX = contentX + col * (columnWidth + MODULE_CARD_SPACING);
            int cardY = contentY + row * (MODULE_CARD_HEIGHT + MODULE_CARD_SPACING);
            
            if (inside(mouseX, mouseY, cardX, cardY, columnWidth, MODULE_CARD_HEIGHT)) {
                Module module = visibleModules.get(index);
                if (button == 0) {
                    module.toggle();
                    moduleManager.save();
                } else if (button == 1) {
                    selectedModule = module;
                }
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    private void nextCategory() {
        int index = selectedCategory.ordinal() + 1;
        if (index >= Category.values().length) {
            index = 0;
        }
        selectedCategory = Category.values()[index];
        selectedModule = null;
        updateVisibleModules();
    }

    private void previousCategory() {
        int index = selectedCategory.ordinal() - 1;
        if (index < 0) {
            index = Category.values().length - 1;
        }
        selectedCategory = Category.values()[index];
        selectedModule = null;
        updateVisibleModules();
    }

    private String formatSetting(Setting<?> setting) {
        if (setting instanceof BooleanSetting bool) {
            return bool.enabled() ? "True" : "False";
        }
        if (setting instanceof ModeSetting mode) {
            return mode.get();
        }
        if (setting instanceof DoubleSetting ds) {
            return String.format("%.2f", ds.get());
        }
        if (setting instanceof IntegerSetting is) {
            return String.valueOf(is.get());
        }
        return "Unknown";
    }

    private void updateVisibleModules() {
        visibleModules = moduleManager.search("", selectedCategory);
    }

    private static float easeOut(float value) {
        return 1.0f - (float) Math.pow(1.0f - MathHelper.clamp(value, 0.0f, 1.0f), 3.0);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
