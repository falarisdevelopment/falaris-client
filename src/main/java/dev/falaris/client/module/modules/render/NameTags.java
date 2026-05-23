package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class NameTags extends RenderModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum nametag render range.", 96.0, 8.0, 256.0));
    private final DoubleSetting scale = setting(new DoubleSetting("Scale", "Base nametag scale.", 0.025, 0.01, 0.08));
    private final BooleanSetting playersOnly = setting(new BooleanSetting("Players Only", "Only show player nametags.", true));
    private final BooleanSetting health = setting(new BooleanSetting("Health", "Show health and absorption.", true));
    private final BooleanSetting armor = setting(new BooleanSetting("Armor", "Show armor slots.", true));
    private final BooleanSetting inventory = setting(new BooleanSetting("Inventory Preview", "Show hotbar inventory preview.", true));
    private final BooleanSetting distance = setting(new BooleanSetting("Distance", "Show distance to entity.", true));

    public NameTags() {
        super("NameTags", "Advanced nametags with health, armor, and inventory preview.");
    }

    @Override
    protected void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        for (Entity entity : client.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || entity == client.player || !entity.isAlive()) {
                continue;
            }
            if (playersOnly.enabled() && !(entity instanceof PlayerEntity)) {
                continue;
            }
            if (client.player.squaredDistanceTo(entity) > range.get() * range.get()) {
                continue;
            }

            RenderUtil.drawNametag(
                    context,
                    entity,
                    lines(client, living),
                    RenderUtil.Color.rgba(8, 10, 18, 130),
                    0xFFFFFFFF,
                    0.6,
                    scale.get()
            );
        }
    }

    private List<String> lines(MinecraftClient client, LivingEntity entity) {
        List<String> lines = new ArrayList<>();
        StringBuilder title = new StringBuilder(entity.getName().getString());
        if (distance.enabled() && client.player != null) {
            title.append(" [").append(Math.round(client.player.distanceTo(entity))).append("m]");
        }
        lines.add(title.toString());

        if (health.enabled()) {
            float hp = entity.getHealth() + entity.getAbsorptionAmount();
            lines.add("HP " + Math.round(hp * 10.0f) / 10.0f + " / " + Math.round(entity.getMaxHealth() * 10.0f) / 10.0f);
        }
        if (armor.enabled()) {
            lines.add("Armor "
                    + slot(entity, EquipmentSlot.HEAD) + " | "
                    + slot(entity, EquipmentSlot.CHEST) + " | "
                    + slot(entity, EquipmentSlot.LEGS) + " | "
                    + slot(entity, EquipmentSlot.FEET));
        }
        if (inventory.enabled() && entity instanceof PlayerEntity player) {
            StringBuilder preview = new StringBuilder("Inv ");
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (!stack.isEmpty()) {
                    preview.append("[").append(RenderUtil.compactItem(stack)).append("] ");
                }
            }
            lines.add(preview.toString().trim());
        }
        return lines;
    }

    private String slot(LivingEntity entity, EquipmentSlot slot) {
        return RenderUtil.compactItem(entity.getEquippedStack(slot));
    }
}
