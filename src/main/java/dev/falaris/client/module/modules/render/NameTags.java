package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;

import java.util.ArrayList;
import java.util.List;

public final class NameTags extends RenderModule {
    private final ModeSetting displayMode = setting(new ModeSetting("Display Mode", "3D world nametags, 2D HUD overlay, or both.", "Both", "3D World", "2D HUD", "Both"));
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Max render range.", 128.0, 8.0, 512.0));
    private final DoubleSetting scale = setting(new DoubleSetting("Scale", "Nametag scale.", 0.04, 0.01, 0.12));
    private final DoubleSetting yOffset = setting(new DoubleSetting("Y Offset", "Height above head.", 1.2, 0.2, 4.0));
    private final BooleanSetting playersOnly = setting(new BooleanSetting("Players Only", "Only players.", false));
    private final BooleanSetting showName = setting(new BooleanSetting("Show Name", "Show player name.", true));
    private final BooleanSetting showHealth = setting(new BooleanSetting("Show Health", "Show health bar + hearts.", true));
    private final BooleanSetting showArmor = setting(new BooleanSetting("Show Armor", "Show armor with durability.", true));
    private final BooleanSetting showHand = setting(new BooleanSetting("Show Hand", "Show main/offhand.", true));
    private final BooleanSetting showDistance = setting(new BooleanSetting("Show Distance", "Distance tag.", true));
    private final BooleanSetting showGroundItems = setting(new BooleanSetting("Ground Items", "Item names on ground.", true));
    private final ModeSetting armorStyle = setting(new ModeSetting("Armor Style", "Display mode.", "Per Piece", "Per Piece", "Total Armor", "Compact"));
    private final ModeSetting durabilityStyle = setting(new ModeSetting("Durability Style", "Bar visual.", "Blocks", "Blocks", "Brackets", "Numbers Only"));
    private final DoubleSetting bgOpacity = setting(new DoubleSetting("BG Opacity", "Background alpha.", 0.5, 0.0, 1.0));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Render behind blocks.", false));
    private final BooleanSetting showArmorBars = setting(new BooleanSetting("Armor Bars", "Durability bars per piece.", true));
    private final BooleanSetting colorByMaterial = setting(new BooleanSetting("Color by Material", "Tier-based color.", true));
    private final BooleanSetting showEnchants = setting(new BooleanSetting("Show Enchants", "Enchant sparkle indicator.", true));
    private final BooleanSetting highlightOp = setting(new BooleanSetting("Highlight OP Items", "Yellow for valuables.", true));
    // HUD overlay settings (used in 2D HUD / Both mode)
    private final ModeSetting hudPosition = setting(new ModeSetting("HUD Position", "Screen position.", "Top Left", "Top Left", "Top Right", "Bottom Left", "Bottom Right"));
    private final IntegerSetting hudOffsetX = setting(new IntegerSetting("HUD Offset X", "Horizontal offset.", 10, 0, 800));
    private final IntegerSetting hudOffsetY = setting(new IntegerSetting("HUD Offset Y", "Vertical offset.", 60, 0, 600));
    private final BooleanSetting showHurtTime = setting(new BooleanSetting("Show Hurt Time", "Flash red when target is hit.", true));

    public NameTags() {
        super("NameTags", "3D nametags + 2D target HUD overlay with health, armor, distance, and hurt time.");
    }

    @Override
    protected void onHudRender(DrawContext ctx, float tickDelta) {
        if (displayMode.is("3D World")) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        LivingEntity target = findHudTarget(client);
        if (target == null || target == client.player) return;

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();

        String name = target.getName().getString();
        float hp = target.getHealth();
        float maxHp = target.getMaxHealth();
        double dist = client.player.distanceTo(target);
        int hurtTime = target.hurtTime;
        double armorVal = target.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ARMOR);
        int armorTotal = (int) Math.round(armorVal);

        String hpStr = String.format("%.0f / %.0f", hp, maxHp);
        String distStr = String.format("%.1f m", dist);
        String line1 = name;
        String line2 = "";
        if (showHealth.enabled()) line2 += "HP: " + hpStr;
        if (showDistance.enabled()) line2 += (line2.isEmpty() ? "" : " | ") + distStr;

        int textW = Math.max(client.textRenderer.getWidth(line1), client.textRenderer.getWidth(line2));
        int ox = hudOffsetX.get();
        int oy = hudOffsetY.get();
        int baseX = switch (hudPosition.get()) {
            case "Top Right" -> sw - textW - ox - 16;
            case "Bottom Right" -> sw - textW - ox - 16;
            case "Bottom Left" -> ox;
            default -> ox;
        };
        int baseY = switch (hudPosition.get()) {
            case "Bottom Left" -> sh - oy - 50;
            case "Bottom Right" -> sh - oy - 50;
            default -> oy;
        };

        int w = textW + 24;
        int h = (showHealth.enabled() || showDistance.enabled()) ? 34 : 16;

        ctx.fill(baseX, baseY, baseX + w, baseY + h, 0xB0101014);
        if (showHurtTime.enabled() && hurtTime > 0) {
            float alpha = Math.min(1.0f, hurtTime / 10.0f);
            ctx.fill(baseX, baseY, baseX + w, baseY + h, ((int)(alpha * 0x30) << 24) | 0xFF0000);
        }
        if (showHealth.enabled() && maxHp > 0) {
            float ratio = Math.max(0, Math.min(1, hp / maxHp));
            int barY = baseY + 13;
            int barH = 4;
            ctx.fill(baseX + 2, barY, baseX + w - 2, barY + barH, 0x40000000);
            int barColor = ratio > 0.6f ? 0xFF44CC44 : ratio > 0.3f ? 0xFFDDAA00 : 0xFFDD3333;
            ctx.fill(baseX + 2, barY, (int)(baseX + 2 + (w - 4) * ratio), barY + barH, barColor);
        }
        ctx.drawText(client.textRenderer, line1, baseX + 2, baseY + 2, 0xFFCCCCCC, true);
        if (!line2.isEmpty()) {
            ctx.drawText(client.textRenderer, line2, baseX + 2, baseY + (showHealth.enabled() ? 19 : 13), 0xFF88AAFF, true);
        }
        if (showArmor.enabled() && armorTotal > 0) {
            String armorStr = "A: " + armorTotal;
            int ax = baseX + w - client.textRenderer.getWidth(armorStr) - 2;
            ctx.drawText(client.textRenderer, armorStr, ax, baseY + (showHealth.enabled() ? 19 : 13), 0xFFAAAAAA, true);
        }
    }

    private LivingEntity findHudTarget(MinecraftClient client) {
        if (client.crosshairTarget instanceof EntityHitResult ehr) {
            if (ehr.getEntity() instanceof LivingEntity le && le.isAlive() && le != client.player) return le;
        }
        return null;
    }

    @Override
    protected void onWorldRender(WorldRenderContext context) {
        if (displayMode.is("2D HUD")) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        double rangeSq = range.get() * range.get();

        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player || !entity.isAlive()) continue;
            if (client.player.squaredDistanceTo(entity) > rangeSq) continue;

            if (entity instanceof LivingEntity living) {
                if (playersOnly.enabled() && !(entity instanceof PlayerEntity)) continue;
                List<String> lines = buildLines(client, living);
                if (lines.isEmpty()) continue;
                double dist = client.player.distanceTo(entity);
                double dynamicScale = scale.get() * Math.min(1.0, 20.0 / Math.max(1.0, dist));
                double dynamicY = yOffset.get() + entity.getHeight() * 0.3;
                RenderUtil.drawNametag(context, entity, lines,
                    RenderUtil.Color.rgba(8, 10, 18, (int)(130 * bgOpacity.get())),
                    0xFFFFFFFF, dynamicY, Math.max(0.01, dynamicScale));
            } else if (showGroundItems.enabled() && entity instanceof ItemEntity ie) {
                ItemStack stack = ie.getStack();
                if (stack.isEmpty()) continue;
                String label = stack.getName().getString() + " x" + stack.getCount();
                int color = highlightOp.enabled() && isOpItem(stack) ? 0xFFFFFF00 : 0xFFFFFFFF;
                double dist = client.player.distanceTo(entity);
                double dynamicScale = scale.get() * 0.8 * Math.min(1.0, 20.0 / Math.max(1.0, dist));
                RenderUtil.drawNametag(context, entity, List.of(label),
                    RenderUtil.Color.rgba(8, 10, 18, (int)(130 * bgOpacity.get())),
                    color, yOffset.get() + 0.3, Math.max(0.01, dynamicScale));
            }
        }
    }

    private List<String> buildLines(MinecraftClient client, LivingEntity entity) {
        List<String> lines = new ArrayList<>();

        // Line 1: Name + distance
        StringBuilder top = new StringBuilder();
        if (showName.enabled()) top.append("§f§l").append(entity.getName().getString());
        if (showDistance.enabled() && client.player != null) {
            int dist = (int) Math.round(client.player.distanceTo(entity));
            String color = dist < 10 ? "§c" : dist < 25 ? "§e" : "§7";
            top.append(" ").append(color).append("[").append(dist).append("m]");
        }
        if (!top.isEmpty()) lines.add(top.toString());

        // Line 2: Health bar (text-based) + heart count + numbers
        if (showHealth.enabled()) {
            float hp = entity.getHealth();
            float max = entity.getMaxHealth();
            float abs = entity.getAbsorptionAmount();
            float pct = Math.min(1, hp / max);

            int barW = 20;
            int filled = (int)(barW * pct);
            String hpColor = pct > 0.6 ? "§a" : pct > 0.25 ? "§e" : "§c";
            StringBuilder bar = new StringBuilder("§8[");
            for (int i = 0; i < barW; i++) {
                if (i < filled) {
                    if (abs > 0 && i >= filled - (int)(barW * Math.min(1, abs / max))) {
                        bar.append("§e█");
                    } else {
                        bar.append(hpColor).append("█");
                    }
                } else {
                    bar.append("§8░");
                }
            }
            bar.append("§8] ");

            int hearts = (int) Math.ceil((hp + abs) / 2.0);
            int maxHearts = (int) Math.ceil(max / 2.0);
            for (int i = 0; i < Math.min(maxHearts, 10); i++) {
                bar.append(i < hearts ? "§c❤" : "§7❤");
            }
            if (maxHearts > 10) bar.append("§7...");

            bar.append(" §f").append(String.format("%.1f", hp)).append("§7/").append(String.format("%.1f", max));
            if (abs > 0) bar.append(" §e+").append(String.format("%.1f", abs));
            lines.add(bar.toString());
        }

        // Lines 3+: Armor
        if (showArmor.enabled()) {
            if (armorStyle.is("Per Piece")) {
                EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
                for (EquipmentSlot slot : slots) {
                    ItemStack stack = entity.getEquippedStack(slot);
                    String piece = armorPieceLine(stack);
                    if (piece != null) lines.add(piece);
                }
            } else if (armorStyle.is("Total Armor")) {
                lines.add(totalArmorLine(entity));
            } else if (armorStyle.is("Compact")) {
                StringBuilder compact = new StringBuilder();
                EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
                boolean first = true;
                for (EquipmentSlot slot : slots) {
                    ItemStack stack = entity.getEquippedStack(slot);
                    if (!first) compact.append(" §7║ ");
                    compact.append(compactPiece(stack));
                    first = false;
                }
                String c = compact.toString().trim();
                if (!c.isEmpty()) lines.add(c);
            }
        }

        // Hand items
        if (showHand.enabled()) {
            ItemStack mainHand = entity.getMainHandStack();
            ItemStack offHand = entity.getOffHandStack();
            StringBuilder hand = new StringBuilder();
            boolean hasMain = !mainHand.isEmpty();
            boolean hasOff = !offHand.isEmpty();
            if (hasMain) {
                hand.append("§f[").append(compactPiece(mainHand)).append("§f]");
            }
            if (hasMain && hasOff) hand.append(" §7| ");
            if (hasOff) {
                hand.append("§f[").append(compactPiece(offHand)).append("§f] §7(off)");
            }
            if (hasMain || hasOff) lines.add(hand.toString());
        }

        return lines;
    }

    private String armorPieceLine(ItemStack stack) {
        if (stack.isEmpty()) return "§8-";
        String name = shortName(stack);
        String color = materialColor(stack);
        String prefix = color;
        if (showEnchants.enabled() && stack.hasGlint()) {
            prefix = "§d";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append(name).append("§7");

        if (stack.isDamaged() && stack.getMaxDamage() > 0) {
            int maxD = stack.getMaxDamage();
            int dmg = stack.getDamage();
            int remain = maxD - dmg;
            int pct = remain * 100 / maxD;
            String durColor = pct > 60 ? "§a" : pct > 25 ? "§e" : "§c";

            sb.append(" ");

            if (showArmorBars.enabled()) {
                sb.append("§7[");
                int filled = Math.max(0, Math.min(10, pct / 10));
                for (int b = 0; b < 10; b++) {
                    sb.append(b < filled ? durColor + "█" : "§8█");
                }
                sb.append("§7] ");
            }

            sb.append(durColor).append(remain).append("§7/").append(maxD);
        } else if (!stack.isDamaged() && stack.getMaxDamage() > 0) {
            sb.append(" §a[").append(stack.getMaxDamage()).append("]");
        }
        return sb.toString();
    }

    private String compactPiece(ItemStack stack) {
        if (stack.isEmpty()) return "§8-";
        return materialColor(stack) + shortName(stack) + "§f";
    }

    private String totalArmorLine(LivingEntity entity) {
        double total = entity.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ARMOR);
        int armorInt = (int) Math.round(total);
        int filled = Math.min(20, armorInt);
        StringBuilder bar = new StringBuilder("§7Armor §8[");
        for (int i = 0; i < 20; i++) {
            bar.append(i < filled ? "§b█" : "§8░");
        }
        bar.append("§8] §f").append(armorInt);
        return bar.toString();
    }

    private String materialColor(ItemStack stack) {
        if (!colorByMaterial.enabled()) return "§f";
        String name = stack.getItem().getTranslationKey().toLowerCase();
        if (name.contains("netherite")) return "§8";
        if (name.contains("diamond")) return "§b";
        if (name.contains("iron")) return "§7";
        if (name.contains("gold")) return "§6";
        if (name.contains("leather")) return "§6";
        if (name.contains("chain")) return "§8";
        if (name.contains("turtle")) return "§2";
        return "§f";
    }

    private String shortName(ItemStack stack) {
        String full = stack.getItem().getTranslationKey();
        if (full.contains(".")) {
            String[] parts = full.split("\\.");
            String last = parts[parts.length - 1].replace("_", " ");
            if (last.startsWith("netherite ")) return last.substring(10);
            if (last.startsWith("diamond ")) return last.substring(8);
            if (last.startsWith("iron ")) return last.substring(5);
            if (last.startsWith("golden ")) return last.substring(7);
            if (last.startsWith("leather ")) return last.substring(8);
            if (last.startsWith("chainmail ")) return last.substring(10);
            return last;
        }
        return stack.getName().getString();
    }

    private boolean isOpItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) return true;
        if (stack.isOf(Items.TOTEM_OF_UNDYING)) return true;
        if (stack.isOf(Items.ELYTRA)) return true;
        if (stack.isOf(Items.TRIDENT)) return true;
        if (stack.isOf(Items.NETHERITE_INGOT)) return true;
        if (stack.isOf(Items.NETHERITE_SCRAP)) return true;
        if (stack.isOf(Items.DIAMOND)) return true;
        if (stack.isOf(Items.ENDER_PEARL)) return true;
        if (stack.isOf(Items.ENDER_CHEST)) return true;
        if (stack.isOf(Items.GOLDEN_APPLE)) return true;
        if (stack.getItem().getTranslationKey().toLowerCase().contains("netherite")) return true;
        if (!net.minecraft.enchantment.EnchantmentHelper.getEnchantments(stack).getEnchantments().isEmpty()) return true;
        return false;
    }
}
