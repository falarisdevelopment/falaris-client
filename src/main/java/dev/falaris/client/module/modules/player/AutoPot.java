package dev.falaris.client.module.modules.player;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;

import java.util.Random;

public final class AutoPot extends PlayerModule {
    private final DoubleSetting healthThreshold = setting(new DoubleSetting("Health Threshold", "Auto-heal when health below.", 10.0, 1.0, 20.0));
    private final BooleanSetting autoHealth = setting(new BooleanSetting("Auto Health", "Auto-splash health pots.", true));
    private final BooleanSetting autoSpeed = setting(new BooleanSetting("Auto Speed", "Auto-splash speed.", false));
    private final BooleanSetting autoStrength = setting(new BooleanSetting("Auto Strength", "Auto-splash strength.", false));
    private final BooleanSetting autoFireRes = setting(new BooleanSetting("Auto Fire Res", "Auto-splash fire resistance.", false));
    private final ModeSetting potMode = setting(new ModeSetting("Pot Mode", "Throw method.", "Normal", "Normal", "Legit", "Instant"));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks between pots.", 10, 2, 40));
    private final IntegerSetting jitter = setting(new IntegerSetting("Jitter", "Random extra ticks.", 3, 0, 10));
    private final BooleanSetting rotate = setting(new BooleanSetting("Rotate", "Look down when throwing.", true));

    private final Random random = new Random();
    private int potCooldown;

    public AutoPot() {
        super("AutoPot", "Auto-splash potions for NoDebuff/Pot PvP.");
    }

    @Override
    protected void onEnable() { super.onEnable(); potCooldown = 0; }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (potCooldown > 0) { potCooldown--; return; }

        if (autoHealth.enabled() && client.player.getHealth() + client.player.getAbsorptionAmount() <= healthThreshold.get()) {
            if (throwPotion(client, findPotionWithEffect(client, StatusEffects.INSTANT_HEALTH))) return;
        }
        if (autoSpeed.enabled() && !client.player.hasStatusEffect(StatusEffects.SPEED)) {
            if (throwPotion(client, findPotionWithEffect(client, StatusEffects.SPEED))) return;
        }
        if (autoStrength.enabled() && !client.player.hasStatusEffect(StatusEffects.STRENGTH)) {
            if (throwPotion(client, findPotionWithEffect(client, StatusEffects.STRENGTH))) return;
        }
        if (autoFireRes.enabled() && client.player.isOnFire() && !client.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
            if (throwPotion(client, findPotionWithEffect(client, StatusEffects.FIRE_RESISTANCE))) return;
        }
    }

    private boolean throwPotion(MinecraftClient client, int slot) {
        if (slot < 0) return false;
        int prev = client.player.getInventory().getSelectedSlot();
        client.player.getInventory().setSelectedSlot(slot);

        if (rotate.enabled()) {
            var rm = FalarisClient.getInstance().getRotationManager();
            rm.setMaxStep(90.0f);
            rm.rotateToSilent(client.player.getYaw(), 90.0f, 1);
        }

        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        client.player.swingHand(Hand.MAIN_HAND);

        client.player.getInventory().setSelectedSlot(prev);
        potCooldown = delay.get() + random.nextInt(jitter.get() + 1);
        return true;
    }

    private int findPotionWithEffect(MinecraftClient client, RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty() || !stack.isOf(Items.SPLASH_POTION)) continue;
            PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents == null) continue;
            if (contents.potion().isPresent()) {
                var potion = contents.potion().get().value();
                for (StatusEffectInstance inst : potion.getEffects()) {
                    if (inst.getEffectType() == effect) return i;
                }
            }
            for (StatusEffectInstance inst : contents.customEffects()) {
                if (inst.getEffectType() == effect) return i;
            }
        }
        return -1;
    }
}
