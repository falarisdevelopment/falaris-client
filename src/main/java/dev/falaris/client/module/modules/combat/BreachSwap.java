package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;

public final class BreachSwap extends CombatModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Max sword hit range for trigger.", 3.5, 1.0, 6.0));
    private final IntegerSetting cooldown = setting(new IntegerSetting("Cooldown", "Ticks between swaps.", 20, 5, 60));
    private final BooleanSetting preferBreach = setting(new BooleanSetting("Prefer Breach", "Prefer mace with Breach enchant.", true));
    private final BooleanSetting preferDensity = setting(new BooleanSetting("Prefer Density", "Prefer mace with Density enchant.", true));
    private final BooleanSetting autoSlam = setting(new BooleanSetting("Auto Slam", "Attack with mace after swap.", true));
    private final BooleanSetting onlyPlayers = setting(new BooleanSetting("Only Players", "Only target players.", true));
    private final DoubleSetting chance = setting(new DoubleSetting("Chance", "Chance to swap per hit (0-1).", 0.75, 0.1, 1.0));

    private int tickCounter;

    public BreachSwap() {
        super("BreachSwap", "Auto-swaps to breach mace after landing a sword hit. Prestige-style.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        tickCounter = 0;
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        tickCounter++;
        if (tickCounter < cooldown.get()) return;

        if (!client.options.attackKey.isPressed()) return;

        var target = client.crosshairTarget;
        if (target == null) return;

        if (target.getType() != net.minecraft.util.hit.HitResult.Type.ENTITY) return;
        var entity = ((net.minecraft.util.hit.EntityHitResult) target).getEntity();
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) return;
        if (onlyPlayers.enabled() && !(entity instanceof net.minecraft.entity.player.PlayerEntity)) return;
        if (client.player.distanceTo(living) > range.get()) return;

        if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() > chance.get()) return;

        boolean holdingSword = isSword(client.player.getMainHandStack().getItem());
        if (!holdingSword) return;

        int maceSlot = findBestMace(client);
        if (maceSlot == -1 || maceSlot >= 9) return;

        client.player.getInventory().setSelectedSlot(maceSlot);
        tickCounter = 0;

        if (autoSlam.enabled() && client.player.fallDistance > 2.0f) {
            CombatUtil.attack(client, living);
        }
    }

    private int findBestMace(MinecraftClient client) {
        int bestSlot = -1;
        int bestScore = -1;
        for (int slot = 0; slot < 36; slot++) {
            var stack = client.player.getInventory().getStack(slot);
            if (!stack.isOf(Items.MACE)) continue;
            int score = slot < 9 ? 3 : 0;
            var ench = stack.get(net.minecraft.component.DataComponentTypes.ENCHANTMENTS);
            if (ench != null) {
                String s = ench.toString().toLowerCase();
                if (s.contains("breach") && preferBreach.enabled()) score += 2;
                if (s.contains("density") && preferDensity.enabled()) score += 2;
            }
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private boolean isSword(net.minecraft.item.Item item) {
        return item == Items.WOODEN_SWORD || item == Items.STONE_SWORD
            || item == Items.IRON_SWORD || item == Items.GOLDEN_SWORD
            || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD;
    }
}
