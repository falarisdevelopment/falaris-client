package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.item.Items;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;

import java.util.Set;

public final class ItemESP extends RenderModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Max render range.", 64.0, 8.0, 128.0));
    private final BooleanSetting valuables = setting(new BooleanSetting("Valuables", "Highlight valuable items.", true));
    private final BooleanSetting shulkers = setting(new BooleanSetting("Shulkers", "Highlight shulker boxes.", true));
    private final BooleanSetting weapons = setting(new BooleanSetting("Weapons", "Highlight weapons/tools.", false));
    private final BooleanSetting armor = setting(new BooleanSetting("Armor", "Highlight armor pieces.", false));
    private final BooleanSetting allItems = setting(new BooleanSetting("All Items", "Highlight every item.", false));
    private final ModeSetting style = setting(new ModeSetting("Style", "Render style.", "Box", "Box", "Glow", "Both"));

    private static final Set<String> VALUABLE_NAMES = Set.of(
        "enchanted_golden_apple", "totem_of_undying", "elytra", "trident",
        "netherite_ingot", "netherite_scrap", "diamond", "emerald",
        "ender_pearl", "shulker_box", "white_shulker_box", "orange_shulker_box",
        "magenta_shulker_box", "light_blue_shulker_box", "yellow_shulker_box",
        "lime_shulker_box", "pink_shulker_box", "gray_shulker_box",
        "light_gray_shulker_box", "cyan_shulker_box", "purple_shulker_box",
        "blue_shulker_box", "brown_shulker_box", "green_shulker_box",
        "red_shulker_box", "black_shulker_box"
    );

    public ItemESP() {
        super("ItemESP", "Highlights valuable item stacks on the ground.");
    }

    @Override
    protected void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        for (var entity : client.world.getEntities()) {
            if (!(entity instanceof ItemEntity ie) || !ie.isAlive()) continue;
            if (client.player.squaredDistanceTo(ie) > range.get() * range.get()) continue;

            ItemStack stack = ie.getStack();
            if (stack.isEmpty()) continue;
            if (!shouldHighlight(stack)) continue;

            RenderUtil.Color c = isValuable(stack)
                ? RenderUtil.Color.rgba(255, 220, 50, 200)
                : RenderUtil.Color.rgba(150, 200, 255, 180);

            RenderUtil.drawBox(context, ie.getBoundingBox().expand(0.15), c, true);
        }
    }

    private boolean shouldHighlight(ItemStack stack) {
        if (allItems.enabled()) return true;
        String name = stack.getItem().getTranslationKey().toLowerCase();
        if (valuables.enabled() && isValuable(stack)) return true;
        if (shulkers.enabled() && name.contains("shulker_box")) return true;
        if (weapons.enabled() && (name.contains("sword") || name.contains("axe") || name.contains("bow") || name.contains("crossbow"))) return true;
        if (armor.enabled() && (name.contains("helmet") || name.contains("chestplate") || name.contains("leggings") || name.contains("boots"))) return true;
        return false;
    }

    private boolean isValuable(ItemStack stack) {
        String name = stack.getItem().getTranslationKey().toLowerCase();
        for (String v : VALUABLE_NAMES) {
            if (name.contains(v)) return true;
        }
        if (!net.minecraft.enchantment.EnchantmentHelper.getEnchantments(stack).getEnchantments().isEmpty()) return true;
        return false;
    }
}
