package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public final class WTap extends CombatModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Sprint reset method.", "WTap", "WTap", "STap", "BlockHit"));
    private final IntegerSetting tapTicks = setting(new IntegerSetting("Tap Ticks", "Ticks to hold sprint off.", 3, 1, 10));
    private final BooleanSetting requireTarget = setting(new BooleanSetting("Require Target", "Only tap when aiming at entity.", true));

    private int tapTimer;
    private boolean wasTapping;

    public WTap() {
        super("WTap", "Resets sprint on hit to reduce knockback received.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null) return;

        if (tapTimer > 0) {
            tapTimer--;
            if (!client.player.isSprinting()) {
                wasTapping = true;
            }
            if (tapTimer <= 0 && wasTapping) {
                client.player.setSprinting(true);
                wasTapping = false;
            }
            return;
        }

        if (!client.options.forwardKey.isPressed()) return;
        if (!client.player.isSprinting()) return;

        boolean attacking = isAttacking(client);
        if (!attacking) return;

        if (mode.is("BlockHit")) {
            if (client.player.getAttackCooldownProgress(0.5f) >= 0.9f) {
                client.options.attackKey.setPressed(false);
                tapTimer = tapTicks.get();
            }
            return;
        }

        client.player.setSprinting(false);
        tapTimer = tapTicks.get();
    }

    private boolean isAttacking(MinecraftClient client) {
        if (client.crosshairTarget instanceof EntityHitResult hit && hit.getType() == HitResult.Type.ENTITY) {
            Entity target = hit.getEntity();
            if (!requireTarget.enabled() || (target instanceof LivingEntity living && living.isAlive() && living != client.player)) {
                return client.options.attackKey.isPressed();
            }
        }
        return false;
    }
}
