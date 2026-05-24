package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.Random;

public final class MaceAssist extends MiscModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Mace assist mode.", "Attack", "Attack", "Notify"));
    private final DoubleSetting minFall = setting(new DoubleSetting("Min Fall", "Minimum fall distance before attacking.", 3.0, 0.5, 80.0));
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum target range.", 4.5, 1.0, 6.0));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks between mace actions.", 3, 1, 30));
    private final IntegerSetting jitter = setting(new IntegerSetting("Jitter", "Random extra ticks between mace actions.", 1, 0, 10));
    private final BooleanSetting requireMace = setting(new BooleanSetting("Require Mace", "Only act while holding a mace.", true));
    private final BooleanSetting velocityBoost = setting(new BooleanSetting("Velocity Boost", "Boost downward speed for extra fall damage.", true));
    private final ModeSetting bypass = setting(new ModeSetting("Bypass", "Anticheat bypass method.", "Vanilla", "Vanilla", "Legit", "Grim"));

    private final Random random = new Random();

    public MaceAssist() {
        super("MaceAssist", "Assists mace timing by attacking under crosshair with fall damage bonus.");
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null || client.crosshairTarget == null) return;
        if (requireMace.enabled() && !client.player.getMainHandStack().isOf(Items.MACE)) return;

        if (velocityBoost.enabled() && client.player.fallDistance > 1.0f && !client.player.isOnGround() && client.player.getVelocity().y > -2.0) {
            client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                client.player.getX(), client.player.getY() - 0.1, client.player.getZ(),
                false, client.player.horizontalCollision
            ));
        }

        if (client.player.fallDistance < minFall.get() || client.crosshairTarget.getType() != HitResult.Type.ENTITY) return;

        Entity target = ((EntityHitResult) client.crosshairTarget).getEntity();
        if (!(target instanceof LivingEntity) || client.player.squaredDistanceTo(target) > range.get() * range.get()) return;
        if (!ready(delay.get(), jitter.get())) return;

        boolean shouldMiss = (bypass.is("Grim") || bypass.is("Legit")) && random.nextFloat() < 0.10f;

        double estimate = estimateDamage((float) client.player.fallDistance);
        if (mode.is("Notify")) {
            client.player.sendMessage(net.minecraft.text.Text.literal("[Falaris] Mace damage: " + Math.round(estimate * 10.0) / 10.0), true);
            return;
        }

        if (shouldMiss) return;

        client.interactionManager.attackEntity(client.player, target);
        client.player.swingHand(Hand.MAIN_HAND);
    }

    private double estimateDamage(float fallDistance) {
        if (fallDistance <= 3.0f) return 6.0;
        if (fallDistance <= 8.0f) return 6.0 + (fallDistance - 3.0f) * 4.0;
        return 26.0 + (fallDistance - 8.0f) * 2.0;
    }
}
