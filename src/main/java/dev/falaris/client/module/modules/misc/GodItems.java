package dev.falaris.client.module.modules.misc;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

public final class GodItems extends MiscModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Item to generate.", "Kill Potion",
            "Kill Potion", "Hyperion Sword", "God Apple", "Stacked Totem", "God Armor", "Give All"));
    private final IntegerSetting amplifier = setting(new IntegerSetting("Amplifier", "Potion amplifier / enchant level.", 255, 1, 255));
    private final IntegerSetting count = setting(new IntegerSetting("Count", "Generated stack count.", 1, 1, 64));
    private final BooleanSetting requireCreative = setting(new BooleanSetting("Require Creative", "Only generate while in creative.", true));

    private boolean generated;

    public GodItems() {
        super("GodItems", "Generates creative-only items including kill potions and stacked totems.");
    }

    @Override
    protected void onEnable() {
        generated = false;
        super.onEnable();
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (generated || client.player == null || client.player.networkHandler == null) return;
        if (requireCreative.enabled() && !client.player.getAbilities().creativeMode) {
            client.player.sendMessage(Text.literal("[Falaris] GodItems requires creative mode."), false);
            generated = true;
            setEnabled(false);
            return;
        }

        if (mode.is("Give All")) {
            giveAll(client);
        } else {
            ItemStack stack = createItem(mode.get());
            giveStack(client, stack, mode.get());
        }
        generated = true;
        setEnabled(false);
    }

    private void giveAll(MinecraftClient client) {
        String[] allModes = {"Kill Potion", "Hyperion Sword", "God Apple", "Stacked Totem", "God Armor"};
        int slot = client.player.getInventory().getSelectedSlot();
        for (String m : allModes) {
            ItemStack stack = createItem(m);
            int s = 36 + slot;
            client.player.getInventory().setStack(slot, stack);
            client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(s, stack));
            slot = (slot + 1) % 9;
        }
        client.player.sendMessage(Text.literal("[Falaris] Generated all items in your hotbar."), false);
    }

    private void giveStack(MinecraftClient client, ItemStack stack, String name) {
        int selectedSlot = client.player.getInventory().getSelectedSlot();
        int slot = 36 + selectedSlot;
        client.player.getInventory().setStack(selectedSlot, stack);
        client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot, stack));
        client.player.sendMessage(Text.literal("[Falaris] Generated " + name + " in your selected hotbar slot."), false);
    }

    private ItemStack createItem(String modeName) {
        return switch (modeName) {
            case "Kill Potion" -> createKillPotion();
            case "Hyperion Sword" -> createHyperionSword();
            case "God Apple" -> createGodApple();
            case "Stacked Totem" -> createStackedTotem();
            case "God Armor" -> createGodArmor();
            default -> ItemStack.EMPTY;
        };
    }

    private ItemStack createKillPotion() {
        ItemStack stack = new ItemStack(Items.SPLASH_POTION, count.get());
        PotionContentsComponent contents = new PotionContentsComponent(
                Optional.empty(),
                Optional.of(0x4B0082),
                List.of(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, amplifier.get())),
                Optional.of("falaris_kill_potion")
        );
        stack.set(DataComponentTypes.POTION_CONTENTS, contents);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Falaris Kill Potion"));
        return stack;
    }

    private ItemStack createHyperionSword() {
        ItemStack stack = new ItemStack(Items.IRON_SWORD, 1);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("\u00A7b\u00A7lHyperion X"));
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    private ItemStack createGodApple() {
        ItemStack stack = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, count.get());
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("\u00A76\u00A7lGod Apple"));
        return stack;
    }

    private ItemStack createStackedTotem() {
        ItemStack stack = new ItemStack(Items.TOTEM_OF_UNDYING, Math.min(count.get(), 64));
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("\u00A7d\u00A7lStacked Totem"));
        return stack;
    }

    private ItemStack createGodArmor() {
        ItemStack stack = new ItemStack(Items.NETHERITE_CHESTPLATE, 1);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("\u00A76\u00A7lGod Armor"));
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }
}
