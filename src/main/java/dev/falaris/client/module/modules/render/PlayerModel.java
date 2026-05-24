package dev.falaris.client.module.modules.render;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public final class PlayerModel extends RenderModule {
    private final IntegerSetting offsetX = setting(new IntegerSetting("Offset X", "Horizontal position.", 50, 0, 800));
    private final IntegerSetting offsetY = setting(new IntegerSetting("Offset Y", "Vertical position.", 150, 0, 600));
    private final IntegerSetting scale = setting(new IntegerSetting("Scale", "Size multiplier.", 100, 40, 300));
    private final BooleanSetting showPose = setting(new BooleanSetting("Show Pose", "Show current action text.", true));
    private final BooleanSetting showEquipment = setting(new BooleanSetting("Show Equipment", "Show armor and held items.", true));
    private final ModeSetting style = setting(new ModeSetting("Style", "Display style.", "Compact", "Compact", "Full"));

    public PlayerModel() {
        super("PlayerModel", "Shows your character model with equipment on screen.");
    }

    @Override
    protected void onHudRender(DrawContext ctx, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        AbstractClientPlayerEntity p = client.player;
        int x = offsetX.get();
        int y = offsetY.get();
        int s = scale.get();

        int headY = y;

        int bodyX = x - s / 6;
        int bodyW = s / 3;
        int bodyH = (int) (s * 0.4);
        int headSize = (int) (s * 0.25);

        String pose = "";
        if (showPose.enabled()) {
            pose = getPoseText(p);
            ctx.drawText(client.textRenderer, "§7" + pose, x - 25, headY - 12, 0xFFFFFFFF, true);
        }

        int nameColor = p.isSneaking() ? 0xFFFFFF55 : 0xFFFFFFFF;
        ctx.drawText(client.textRenderer, p.getName().getString(), x - client.textRenderer.getWidth(p.getName().getString()) / 2, headY - headSize / 2 - 10, nameColor, true);

        int headX = x - headSize / 2;
        int skinColor = 0xFFFFD8BE;
        ctx.fill(headX, headY, headX + headSize, headY + headSize, skinColor);

        int eyeSize = Math.max(1, headSize / 6);
        int eyeY = headY + headSize / 3;
        int eyeSpacing = headSize / 4;
        ctx.fill(headX + eyeSpacing, eyeY, headX + eyeSpacing + eyeSize, eyeY + eyeSize, 0xFF000000);
        ctx.fill(headX + headSize - eyeSpacing - eyeSize, eyeY, headX + headSize - eyeSpacing, eyeY + eyeSize, 0xFF000000);

        int mouthY = headY + (int) (headSize * 0.65);
        int mouthW = headSize / 3;
        ctx.fill(headX + (headSize - mouthW) / 2, mouthY, headX + (headSize + mouthW) / 2, mouthY + 1, 0xFF000000);

        int bodyY = headY + headSize + 2;
        int armorColor = getArmorColor(p);

        ctx.fill(bodyX, bodyY, bodyX + bodyW, bodyY + bodyH, armorColor);

        if (!p.getEquippedStack(EquipmentSlot.CHEST).isEmpty()) {
            ctx.fill(bodyX - 1, bodyY - 1, bodyX + bodyW + 1, bodyY, armorColor);
        }
        if (!p.getEquippedStack(EquipmentSlot.LEGS).isEmpty()) {
            ctx.fill(bodyX, bodyY + bodyH - 2, bodyX + bodyW, bodyY + bodyH, getArmorColor(EquipmentSlot.LEGS, p));
        }

        int armW = Math.max(2, bodyW / 4);
        int armH = (int) (bodyH * 0.7);
        ctx.fill(bodyX - armW, bodyY + 2, bodyX, bodyY + 2 + armH, skinColor);
        ctx.fill(bodyX + bodyW, bodyY + 2, bodyX + bodyW + armW, bodyY + 2 + armH, skinColor);

        ItemStack mainHand = p.getMainHandStack();
        if (showEquipment.enabled() && !mainHand.isEmpty()) {
            String handName = mainHand.getName().getString();
            if (handName.length() > 10) handName = handName.substring(0, 10) + "..";
            ctx.drawText(client.textRenderer, "§f" + handName, bodyX + bodyW + armW + 2, bodyY + 4, 0xFFFFFFFF, true);
        }

        int legW = bodyW / 3;
        int legH = (int) (bodyH * 0.8);
        int legY = bodyY + bodyH;
        ctx.fill(bodyX + legW, legY, bodyX + legW * 2, legY + legH, getLeggingsColor(p));
        ctx.fill(bodyX, legY, bodyX + legW, legY + legH, getLeggingsColor(p));

        if (showEquipment.enabled()) {
            ItemStack head = p.getEquippedStack(EquipmentSlot.HEAD);
            ItemStack chest = p.getEquippedStack(EquipmentSlot.CHEST);
            ItemStack legs = p.getEquippedStack(EquipmentSlot.LEGS);
            ItemStack feet = p.getEquippedStack(EquipmentSlot.FEET);

            String info = "";
            if (!head.isEmpty()) info += "§f" + abbreviate(head.getName().getString(), 6) + " ";
            if (!chest.isEmpty()) info += "§f" + abbreviate(chest.getName().getString(), 6) + " ";
            if (!legs.isEmpty()) info += "§f" + abbreviate(legs.getName().getString(), 6) + " ";
            if (!feet.isEmpty()) info += "§f" + abbreviate(feet.getName().getString(), 6) + " ";
            if (!info.isEmpty()) {
                ctx.drawText(client.textRenderer, info.trim(), x - 35, legY + legH + 2, 0xFFFFFFFF, true);
            }
        }
    }

    private int getArmorColor(AbstractClientPlayerEntity player) {
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.isEmpty()) return 0xFF336699;
        return 0xFF4488AA;
    }

    private int getArmorColor(EquipmentSlot slot, AbstractClientPlayerEntity player) {
        ItemStack stack = player.getEquippedStack(slot);
        if (stack.isEmpty()) return 0xFF446688;
        return 0xFF5588AA;
    }

    private int getLeggingsColor(AbstractClientPlayerEntity player) {
        ItemStack leggings = player.getEquippedStack(EquipmentSlot.LEGS);
        if (leggings.isEmpty()) return 0xFF2255AA;
        return 0xFF3366AA;
    }

    private String getPoseText(AbstractClientPlayerEntity player) {
        if (player.isUsingItem()) {
            String item = player.getMainHandStack().getName().getString();
            return "§6" + item;
        }
        if (player.isSneaking()) return "§7Sneaking";
        if (player.isSprinting()) return "§eSprinting";
        if (player.isSwimming()) return "§bSwimming";
        if (!player.isOnGround()) return "§aAir";
        if (player.hurtTime > 0) return "§cHurt";
        return "§7Idle";
    }

    private String abbreviate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + ".." : s;
    }
}
