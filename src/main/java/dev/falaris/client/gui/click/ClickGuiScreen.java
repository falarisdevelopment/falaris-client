package dev.falaris.client.gui.click;

import dev.falaris.client.module.Category;
import dev.falaris.client.module.Module;
import dev.falaris.client.module.ModuleManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClickGuiScreen extends Screen {
    private static final int MIN_WIDTH = 560;
    private static final int MIN_HEIGHT = 360;
    private static final int SIDEBAR_WIDTH = 146;
    private static final int HEADER_HEIGHT = 58;
    private static final int CARD_HEIGHT = 48;
    private static final int CARD_GAP = 8;

    private final ModuleManager moduleManager;
    private final Animation openAnimation = new Animation(0.0f);
    private final Map<Category, Animation> categoryAnimations = new EnumMap<>(Category.class);
    private final Map<String, Animation> toggleAnimations = new HashMap<>();

    private Category selectedCategory = Category.CLIENT;
    private String searchQuery = "";
    private int x = 42;
    private int y = 30;
    private int panelWidth = 640;
    private int panelHeight = 420;
    private int scrollOffset;
    private boolean dragging;
    private boolean resizing;
    private boolean searchFocused;
    private double dragOffsetX;
    private double dragOffsetY;

    public ClickGuiScreen(ModuleManager moduleManager) {
        super(Text.literal("Falaris Client"));
        this.moduleManager = moduleManager;
        for (Category category : Category.values()) {
            categoryAnimations.put(category, new Animation(category == selectedCategory ? 1.0f : 0.0f));
        }
        openAnimation.setTarget(1.0f);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void tick() {
        openAnimation.tick(0.2f);
        for (Category category : Category.values()) {
            Animation animation = categoryAnimations.get(category);
            animation.setTarget(category == selectedCategory ? 1.0f : 0.0f);
            animation.tick(0.22f);
        }
        for (Module module : moduleManager.getModules()) {
            Animation animation = toggleAnimations.computeIfAbsent(module.getId(), ignored -> new Animation(module.isEnabled() ? 1.0f : 0.0f));
            animation.setTarget(module.isEnabled() ? 1.0f : 0.0f);
            animation.tick(0.28f);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        float open = easeOut(openAnimation.get());
        int alpha = (int) (245 * open);
        int drawX = x + (int) ((1.0f - open) * 16.0f);
        int drawY = y + (int) ((1.0f - open) * 10.0f);

        UiRender.fillRound(context, drawX + 6, drawY + 8, panelWidth, panelHeight, 14, UiColor.argb((int) (80 * open), 0, 0, 0));
        UiRender.fillRound(context, drawX, drawY, panelWidth, panelHeight, 12, UiColor.argb(alpha, 11, 13, 21));
        UiRender.fillRound(context, drawX + 1, drawY + 1, panelWidth - 2, panelHeight - 2, 11, UiColor.argb(alpha, 17, 19, 30));
        UiRender.fillVerticalGradient(context, drawX + SIDEBAR_WIDTH, drawY, panelWidth - SIDEBAR_WIDTH, panelHeight, UiColor.argb(alpha, 18, 20, 32), UiColor.argb(alpha, 12, 14, 22));
        UiRender.fillVerticalGradient(context, drawX, drawY, SIDEBAR_WIDTH, panelHeight, UiColor.argb(alpha, 20, 22, 36), UiColor.argb(alpha, 12, 14, 23));

        renderBrand(context, drawX, drawY, alpha);
        renderSearch(context, drawX, drawY, mouseX, mouseY, alpha);
        renderSidebar(context, drawX, drawY, alpha);
        renderModules(context, drawX, drawY, mouseX, mouseY, alpha);

        UiRender.fillRound(context, drawX + panelWidth - 18, drawY + panelHeight - 18, 12, 12, 4, UiColor.argb(alpha, 58, 66, 94));
        UiRender.fillRound(context, drawX + panelWidth - 14, drawY + panelHeight - 14, 8, 8, 3, UiColor.argb(alpha, 129, 151, 255));
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderBrand(DrawContext context, int drawX, int drawY, int alpha) {
        UiRender.fillRound(context, drawX + 18, drawY + 17, 31, 31, 9, UiColor.argb(alpha, 113, 86, 255));
        UiRender.fillRound(context, drawX + 21, drawY + 20, 25, 25, 7, UiColor.argb(alpha, 42, 48, 88));
        context.drawTextWithShadow(textRenderer, "F", drawX + 31, drawY + 29, UiColor.rgb(238, 241, 255));
        context.drawText(textRenderer, "Falaris", drawX + 58, drawY + 19, UiColor.rgb(239, 242, 255), false);
        context.drawText(textRenderer, "Client", drawX + 58, drawY + 32, UiColor.rgb(132, 146, 178), false);
    }

    private void renderSearch(DrawContext context, int drawX, int drawY, int mouseX, int mouseY, int alpha) {
        int searchX = drawX + SIDEBAR_WIDTH + 18;
        int searchY = drawY + 16;
        int searchWidth = panelWidth - SIDEBAR_WIDTH - 36;
        boolean hovered = inside(mouseX, mouseY, searchX, searchY, searchWidth, 32);
        int border = searchFocused ? UiColor.argb(alpha, 122, 156, 255) : hovered ? UiColor.argb(alpha, 72, 82, 122) : UiColor.argb(alpha, 35, 40, 58);
        UiRender.fillRound(context, searchX, searchY, searchWidth, 32, 9, border);
        UiRender.fillRound(context, searchX + 1, searchY + 1, searchWidth - 2, 30, 8, UiColor.argb(alpha, 22, 25, 38));

        String value = searchQuery.isEmpty() && !searchFocused ? "Search modules" : searchQuery;
        int color = searchQuery.isEmpty() && !searchFocused ? UiColor.rgb(103, 115, 144) : UiColor.rgb(218, 225, 246);
        context.drawText(textRenderer, "?", searchX + 12, searchY + 12, UiColor.rgb(132, 146, 178), false);
        context.drawText(textRenderer, value, searchX + 28, searchY + 12, color, false);

        String count = moduleManager.search(searchQuery, selectedCategory).size() + " modules";
        context.drawText(textRenderer, count, searchX + searchWidth - textRenderer.getWidth(count) - 14, searchY + 12, UiColor.rgb(116, 128, 158), false);
    }

    private void renderSidebar(DrawContext context, int drawX, int drawY, int alpha) {
        int categoryY = drawY + 72;
        for (Category category : Category.values()) {
            float selected = easeOut(categoryAnimations.get(category).get());
            int rowX = drawX + 13;
            int rowY = categoryY - 8;
            int rowWidth = SIDEBAR_WIDTH - 26;
            int selectedAlpha = (int) (selected * alpha);
            if (selectedAlpha > 0) {
                UiRender.fillHorizontalGradient(context, rowX, rowY, rowWidth, 28, UiColor.argb(selectedAlpha, 85, 72, 176), UiColor.argb(selectedAlpha, 54, 92, 166));
                UiRender.fillRound(context, rowX, rowY, rowWidth, 28, 8, UiColor.argb((int) (selectedAlpha * 0.25f), 180, 190, 255));
            }

            int iconColor = blend(UiColor.rgb(129, 142, 174), UiColor.rgb(238, 241, 255), selected);
            int textColor = blend(UiColor.rgb(142, 154, 185), UiColor.rgb(238, 241, 255), selected);
            UiRender.fillRound(context, rowX + 8, rowY + 6, 16, 16, 5, UiColor.argb(alpha, 31, 35, 53));
            context.drawText(textRenderer, icon(category), rowX + 12, rowY + 11, iconColor, false);
            context.drawText(textRenderer, category.getDisplayName(), rowX + 32, rowY + 10, textColor, false);
            categoryY += 34;
        }
    }

    private void renderModules(DrawContext context, int drawX, int drawY, int mouseX, int mouseY, int alpha) {
        List<Module> modules = moduleManager.search(searchQuery, selectedCategory);
        int moduleX = drawX + SIDEBAR_WIDTH + 18;
        int moduleY = drawY + HEADER_HEIGHT + 20 - scrollOffset;
        int moduleWidth = panelWidth - SIDEBAR_WIDTH - 36;
        int bottom = drawY + panelHeight - 22;

        for (Module module : modules) {
            if (moduleY + CARD_HEIGHT >= drawY + HEADER_HEIGHT && moduleY <= bottom) {
                renderModuleCard(context, module, moduleX, moduleY, moduleWidth, mouseX, mouseY, alpha);
            }
            moduleY += CARD_HEIGHT + CARD_GAP;
        }

        if (modules.isEmpty()) {
            String empty = "No modules found";
            context.drawText(textRenderer, empty, moduleX + moduleWidth / 2 - textRenderer.getWidth(empty) / 2, drawY + panelHeight / 2, UiColor.rgb(125, 138, 168), false);
        }
    }

    private void renderModuleCard(DrawContext context, Module module, int moduleX, int moduleY, int moduleWidth, int mouseX, int mouseY, int alpha) {
        boolean hovered = inside(mouseX, mouseY, moduleX, moduleY, moduleWidth, CARD_HEIGHT);
        float enabled = easeOut(toggleAnimations.computeIfAbsent(module.getId(), ignored -> new Animation(module.isEnabled() ? 1.0f : 0.0f)).get());
        int base = hovered ? UiColor.argb(alpha, 29, 33, 49) : UiColor.argb(alpha, 23, 27, 41);
        int enabledOverlay = UiColor.argb((int) (enabled * 80), 88, 94, 174);

        UiRender.fillRound(context, moduleX, moduleY, moduleWidth, CARD_HEIGHT, 9, base);
        if (enabled > 0.01f) {
            UiRender.fillHorizontalGradient(context, moduleX, moduleY, moduleWidth, CARD_HEIGHT, enabledOverlay, UiColor.argb((int) (enabled * 42), 53, 97, 167));
        }
        UiRender.fillRound(context, moduleX, moduleY, 4, CARD_HEIGHT, 2, module.isEnabled() ? UiColor.rgb(143, 105, 255) : UiColor.rgb(47, 54, 76));

        int titleColor = module.isEnabled() ? UiColor.rgb(246, 248, 255) : UiColor.rgb(224, 230, 246);
        context.drawText(textRenderer, module.getName(), moduleX + 14, moduleY + 10, titleColor, false);
        String description = trimToWidth(module.getDescription(), moduleWidth - 120);
        context.drawText(textRenderer, description, moduleX + 14, moduleY + 27, UiColor.rgb(125, 139, 171), false);

        int settingsCount = module.getSettings().size();
        String settings = settingsCount + " set";
        context.drawText(textRenderer, settings, moduleX + moduleWidth - 96, moduleY + 10, UiColor.rgb(118, 131, 162), false);

        int toggleX = moduleX + moduleWidth - 48;
        int toggleY = moduleY + 15;
        UiRender.fillRound(context, toggleX, toggleY, 34, 18, 9, UiColor.argb(alpha, 45, 52, 73));
        if (enabled > 0.01f) {
            UiRender.fillHorizontalGradient(context, toggleX, toggleY, 34, 18, UiColor.argb((int) (enabled * alpha), 137, 98, 255), UiColor.argb((int) (enabled * alpha), 84, 161, 255));
        }
        int knobX = toggleX + 3 + Math.round(enabled * 16.0f);
        UiRender.fillRound(context, knobX, toggleY + 3, 12, 12, 6, UiColor.rgb(242, 245, 255));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }

        searchFocused = false;
        if (inside(mouseX, mouseY, x + panelWidth - 20, y + panelHeight - 20, 20, 20)) {
            resizing = true;
            return true;
        }

        if (inside(mouseX, mouseY, x + SIDEBAR_WIDTH + 18, y + 16, panelWidth - SIDEBAR_WIDTH - 36, 32)) {
            searchFocused = true;
            return true;
        }

        if (inside(mouseX, mouseY, x, y, panelWidth, HEADER_HEIGHT)) {
            dragging = true;
            dragOffsetX = mouseX - x;
            dragOffsetY = mouseY - y;
            return true;
        }

        int categoryY = y + 64;
        for (Category category : Category.values()) {
            if (inside(mouseX, mouseY, x + 13, categoryY, SIDEBAR_WIDTH - 26, 28)) {
                selectedCategory = category;
                scrollOffset = 0;
                return true;
            }
            categoryY += 34;
        }

        int moduleX = x + SIDEBAR_WIDTH + 18;
        int moduleY = y + HEADER_HEIGHT + 20 - scrollOffset;
        int moduleWidth = panelWidth - SIDEBAR_WIDTH - 36;
        for (Module module : moduleManager.search(searchQuery, selectedCategory)) {
            if (inside(mouseX, mouseY, moduleX, moduleY, moduleWidth, CARD_HEIGHT)) {
                module.toggle();
                moduleManager.save();
                return true;
            }
            moduleY += CARD_HEIGHT + CARD_GAP;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (dragging) {
            x = (int) (mouseX - dragOffsetX);
            y = (int) (mouseY - dragOffsetY);
            return true;
        }

        if (resizing) {
            panelWidth = Math.max(MIN_WIDTH, (int) mouseX - x);
            panelHeight = Math.max(MIN_HEIGHT, (int) mouseY - y);
            return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int contentHeight = moduleManager.search(searchQuery, selectedCategory).size() * (CARD_HEIGHT + CARD_GAP);
        int visibleHeight = panelHeight - HEADER_HEIGHT - 40;
        int maxScroll = Math.max(0, contentHeight - visibleHeight);
        scrollOffset = MathHelper.clamp(scrollOffset - (int) (verticalAmount * 24.0), 0, maxScroll);
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = false;
        resizing = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (!searchFocused) {
            return super.charTyped(input);
        }
        String typed = input.asString();
        if (typed.isEmpty()) {
            return false;
        }
        char chr = typed.charAt(0);
        if (Character.isLetterOrDigit(chr) || Character.isWhitespace(chr) || chr == '_' || chr == '-') {
            searchQuery += chr;
            scrollOffset = 0;
            return true;
        }

        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (searchFocused && keyCode == 259 && !searchQuery.isEmpty()) {
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            scrollOffset = 0;
            return true;
        }
        if (searchFocused && keyCode == 256) {
            searchFocused = false;
            return true;
        }

        return super.keyPressed(input);
    }

    private String trimToWidth(String value, int maxWidth) {
        if (textRenderer.getWidth(value) <= maxWidth) {
            return value;
        }
        String trimmed = value;
        while (!trimmed.isEmpty() && textRenderer.getWidth(trimmed + "...") > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + "...";
    }

    private static String icon(Category category) {
        return switch (category) {
            case COMBAT -> "C";
            case MOVEMENT -> "M";
            case RENDER -> "R";
            case PLAYER -> "P";
            case WORLD -> "W";
            case MISC -> "*";
            case CLIENT -> "F";
        };
    }

    private static float easeOut(float value) {
        return 1.0f - (float) Math.pow(1.0f - MathHelper.clamp(value, 0.0f, 1.0f), 3.0);
    }

    private static int blend(int from, int to, float amount) {
        amount = MathHelper.clamp(amount, 0.0f, 1.0f);
        int r = (int) (red(from) + (red(to) - red(from)) * amount);
        int g = (int) (green(from) + (green(to) - green(from)) * amount);
        int b = (int) (blue(from) + (blue(to) - blue(from)) * amount);
        return UiColor.rgb(r, g, b);
    }

    private static int red(int color) {
        return color >> 16 & 255;
    }

    private static int green(int color) {
        return color >> 8 & 255;
    }

    private static int blue(int color) {
        return color & 255;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
