package dev.falaris.client.module.modules.render;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.gui.click.UiColor;
import dev.falaris.client.module.Module;
import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Comparator;
import java.util.List;

public final class ArrayListMod extends RenderModule {
    private final ModeSetting sortMode = setting(new ModeSetting("Sort", "ABC or by length.", "ABC", "ABC", "Length"));
    private final ModeSetting colorMode = setting(new ModeSetting("Color", "Text color mode.", "Purple", "Purple", "Rainbow", "White", "Category"));
    private final IntegerSetting offsetX = setting(new IntegerSetting("Offset X", "Horizontal position.", 2, 0, 800));
    private final IntegerSetting offsetY = setting(new IntegerSetting("Offset Y", "Vertical position.", 2, 0, 600));
    private final BooleanSetting shadow = setting(new BooleanSetting("Shadow", "Draw text shadow.", true));
    private final BooleanSetting background = setting(new BooleanSetting("Background", "Draw dark background behind text.", true));

    public ArrayListMod() {
        super("ArrayList", "Shows active modules on screen.");
    }

    @Override
    protected void onHudRender(DrawContext ctx, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        List<Module> active = FalarisClient.getInstance().getModuleManager().getModules().stream()
                .filter(Module::isEnabled)
                .filter(m -> !m.getName().equals("ArrayList"))
                .sorted(sortMode.is("ABC")
                        ? Comparator.comparing(Module::getName)
                        : Comparator.comparingInt(m -> -m.getName().length()))
                .toList();

        int y = offsetY.get();
        int screenW = client.getWindow().getScaledWidth();

        for (Module mod : active) {
            String name = mod.getName();
            String suffix = settingSuffix(mod);
            String display = suffix.isEmpty() ? name : name + " §7" + suffix;
            int color = getColor(mod, y);
            int textW = client.textRenderer.getWidth(display);
            int x = screenW - textW - 4 - offsetX.get();

            if (background.enabled()) {
                ctx.fill(x - 2, y - 1, x + textW + 4, y + 10, 0x60000000);
            }

            if (shadow.enabled()) {
                ctx.drawText(client.textRenderer, display, x + 1, y + 1, color, true);
            } else {
                ctx.drawText(client.textRenderer, display, x + 1, y + 1, color, false);
            }

            y += 11;
        }
    }

    private int getColor(Module mod, int yOffset) {
        if (colorMode.is("White")) return UiColor.rgb(230, 232, 240);
        if (colorMode.is("Rainbow")) return rainbowColor(yOffset);
        if (colorMode.is("Category")) return switch (mod.getCategory()) {
            case COMBAT -> UiColor.rgb(235, 100, 110);
            case MOVEMENT -> UiColor.rgb(100, 200, 150);
            case RENDER -> UiColor.rgb(130, 170, 245);
            case PLAYER -> UiColor.rgb(245, 200, 100);
            case WORLD -> UiColor.rgb(200, 150, 100);
            case MISC -> UiColor.rgb(180, 150, 220);
            case CLIENT -> UiColor.rgb(170, 170, 190);
        };
        return UiColor.rgb(155, 130, 225); // Purple default
    }

    private int rainbowColor(int offset) {
        long time = System.currentTimeMillis() / 30;
        float hue = ((time + offset * 15L) % 360L) / 360f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.75f, 0.95f);
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    private String settingSuffix(Module mod) {
        for (dev.falaris.client.setting.Setting<?> s : mod.getSettings()) {
            String n = s.getName().toLowerCase();
            if (n.contains("range") || n.contains("distance")) {
                String val = s.get().toString();
                return "(" + val + " " + (n.contains("distance") ? "m" : "blocks") + ")";
            }
            if (n.contains("cps")) {
                String val = s.get().toString();
                return "(" + val + " cps)";
            }
            if (n.contains("mode")) {
                return "(" + ((dev.falaris.client.setting.ModeSetting) s).get() + ")";
            }
        }
        return "";
    }
}
