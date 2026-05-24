package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.Collection;

public final class PotionEffects extends RenderModule {
    private final ModeSetting position = setting(new ModeSetting("Position", "Screen position.", "Top Right", "Top Left", "Top Right", "Bottom Left", "Bottom Right"));
    private final IntegerSetting offsetX = setting(new IntegerSetting("Offset X", "Horizontal offset.", 4, 0, 800));
    private final IntegerSetting offsetY = setting(new IntegerSetting("Offset Y", "Vertical offset.", 40, 0, 600));
    private final BooleanSetting showAmplifier = setting(new BooleanSetting("Show Amplifier", "Show potion level.", true));
    private final BooleanSetting showTimer = setting(new BooleanSetting("Show Timer", "Show remaining duration.", true));
    private final BooleanSetting sortByTime = setting(new BooleanSetting("Sort By Time", "Sort by remaining duration.", true));
    private final BooleanSetting icon = setting(new BooleanSetting("Icon", "Show potion effect icon.", true));

    public PotionEffects() {
        super("PotionEffects", "Displays active potion effects with duration and amplifier.");
    }

    @Override
    protected void onHudRender(DrawContext ctx, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Collection<StatusEffectInstance> effects = client.player.getStatusEffects();
        if (effects.isEmpty()) return;

        var sorted = effects.stream()
            .filter(e -> e.getDuration() > 0)
            .sorted((a, b) -> sortByTime.enabled()
                ? Integer.compare(b.getDuration(), a.getDuration())
                : a.getEffectType().value().getName().getString().compareTo(b.getEffectType().value().getName().getString()))
            .toList();

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        int x = offsetX.get();
        int y = offsetY.get();

        for (StatusEffectInstance effect : sorted) {
            String name = effect.getEffectType().value().getName().getString();
            StringBuilder line = new StringBuilder(name);

            if (showAmplifier.enabled() && effect.getAmplifier() > 0) {
                line.append(" ").append(toRoman(effect.getAmplifier() + 1));
            }
            if (showTimer.enabled()) {
                int total = effect.getDuration() / 20;
                int min = total / 60;
                int sec = total % 60;
                line.append(" §7").append(String.format("%d:%02d", min, sec));
            }

            int color = effect.getEffectType().value().getColor();
            int textW = client.textRenderer.getWidth(line.toString());
            int drawX = switch (position.get()) {
                case "Top Right" -> sw - textW - x - 4;
                case "Bottom Right" -> sw - textW - x - 4;
                case "Bottom Left" -> x;
                default -> x;
            };
            int drawY = switch (position.get()) {
                case "Bottom Left" -> sh - y - (sorted.size() - sorted.indexOf(effect)) * 12;
                case "Bottom Right" -> sh - y - (sorted.size() - sorted.indexOf(effect)) * 12;
                default -> y + sorted.indexOf(effect) * 12;
            };

            ctx.fill(drawX - 2, drawY - 1, drawX + textW + 4, drawY + 10, 0x60000000);
            if (icon.enabled()) {
                ctx.fill(drawX - 2, drawY - 1, drawX, drawY + 10, (color & 0xFFFFFF) | 0xA0000000);
            }
            ctx.drawText(client.textRenderer, line.toString(), drawX + 2, drawY + 1, color, true);
        }
    }

    private String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            default -> String.valueOf(n);
        };
    }
}
