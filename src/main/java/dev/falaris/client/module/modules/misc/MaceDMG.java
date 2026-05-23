package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public final class MaceDMG extends MiscModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Mace damage helper mode.", "Attack", "Attack", "Notify"));
    private final DoubleSetting minFall = setting(new DoubleSetting("Min Fall", "Minimum fall distance before attacking.", 3.0, 0.5, 80.0));
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum target range.", 4.5, 1.0, 6.0));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks between mace actions.", 3, 1, 30));
    private final IntegerSetting jitter = setting(new IntegerSetting("Jitter", "Random extra ticks between mace actions.", 1, 0, 10));
    private final BooleanSetting requireMace = setting(new BooleanSetting("Require Mace", "Only act while holding a mace.", true));

    public MaceDMG() {
        super("MaceDMG", "Assists timing high-fall mace attacks and reports estimated damage.");
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null || client.crosshairTarget == null) {
            return;
        }
        if (requireMace.enabled() && !client.player.getMainHandStack().isOf(Items.MACE)) {
            return;
        }
        if (client.player.fallDistance < minFall.get() || client.crosshairTarget.getType() != HitResult.Type.ENTITY) {
            return;
        }

        Entity target = ((EntityHitResult) client.crosshairTarget).getEntity();
        if (!(target instanceof LivingEntity) || client.player.squaredDistanceTo(target) > range.get() * range.get()) {
            return;
        }
        if (!ready(delay.get(), jitter.get())) {
            return;
        }

        double estimate = estimateDamage((float) client.player.fallDistance);
        if (mode.is("Notify")) {
            client.player.sendMessage(net.minecraft.text.Text.literal("[Falaris] Estimated mace damage: " + Math.round(estimate * 10.0) / 10.0), true);
            return;
        }

        client.interactionManager.attackEntity(client.player, target);
        client.player.swingHand(Hand.MAIN_HAND);
    }

    private double estimateDamage(float fallDistance) {
        if (fallDistance <= 3.0f) {
            return 6.0;
        }
        if (fallDistance <= 8.0f) {
            return 6.0 + (fallDistance - 3.0f) * 4.0;
        }
        return 26.0 + (fallDistance - 8.0f) * 2.0;
    }
}
