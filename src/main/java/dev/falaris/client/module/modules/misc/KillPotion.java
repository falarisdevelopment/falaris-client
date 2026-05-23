package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

public final class KillPotion extends MiscModule {
    private final ModeSetting potionType = setting(new ModeSetting("Potion Type", "Generated potion item type.", "Splash", "Splash", "Lingering", "Drinkable"));
    private final IntegerSetting amplifier = setting(new IntegerSetting("Amplifier", "Instant damage amplifier.", 125, 1, 255));
    private final IntegerSetting count = setting(new IntegerSetting("Count", "Generated stack count.", 1, 1, 64));
    private final BooleanSetting requireCreative = setting(new BooleanSetting("Require Creative", "Only generate while in creative.", true));
    private boolean generated;

    public KillPotion() {
        super("KillPotion", "Generates an OP-only creative test potion with high instant damage.");
    }

    @Override
    protected void onEnable() {
        generated = false;
        super.onEnable();
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (generated || client.player == null || client.player.networkHandler == null) {
            return;
        }
        if (requireCreative.enabled() && !client.player.getAbilities().creativeMode) {
            client.player.sendMessage(Text.literal("[Falaris] KillPotion requires creative mode or OP creative permissions."), false);
            generated = true;
            setEnabled(false);
            return;
        }

        ItemStack stack = createPotion();
        int selectedSlot = client.player.getInventory().getSelectedSlot();
        int slot = 36 + selectedSlot;
        client.player.getInventory().setStack(selectedSlot, stack);
        client.player.networkHandler.sendPacket(new CreativeInventoryActionC2SPacket(slot, stack));
        client.player.sendMessage(Text.literal("[Falaris] Generated creative test kill potion in your selected hotbar slot."), false);
        generated = true;
        setEnabled(false);
    }

    private ItemStack createPotion() {
        Item item = switch (potionType.get()) {
            case "Lingering" -> Items.LINGERING_POTION;
            case "Drinkable" -> Items.POTION;
            default -> Items.SPLASH_POTION;
        };

        ItemStack stack = new ItemStack(item, count.get());
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
}
