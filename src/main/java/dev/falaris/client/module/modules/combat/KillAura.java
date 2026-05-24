package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public final class KillAura extends CombatModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum attack distance.", 3.0, 1.0, 6.0));
    private final BooleanSetting useCooldown = setting(new BooleanSetting("Use Cooldown", "Wait for the weapon to fully charge before attacking.", true));
    private final DoubleSetting cps = setting(new DoubleSetting("CPS", "Average attacks per second.", 9.0, 1.0, 20.0));
    private final IntegerSetting jitter = setting(new IntegerSetting("Delay Jitter", "Random extra ticks between attacks.", 2, 0, 8));
    private final DoubleSetting rotationSpeed = setting(new DoubleSetting("Rotation Speed", "Maximum rotation step per tick.", 18.0, 1.0, 60.0));
    private final ModeSetting priority = setting(new ModeSetting("Priority", "Target sorting mode.", "Distance", "Distance", "Health", "Angle"));
    private final BooleanSetting silentRotate = setting(new BooleanSetting("Silent Rotate", "Rotate without moving your head visibly.", true));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Target players.", true));
    private final BooleanSetting hostiles = setting(new BooleanSetting("Hostiles", "Target hostile mobs.", true));
    private final BooleanSetting passives = setting(new BooleanSetting("Passives", "Target passive mobs.", false));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Allow targets without line of sight.", false));
    private final ModeSetting bypass = setting(new ModeSetting("Bypass", "Anticheat bypass method.", "Vanilla", "Vanilla", "Legit", "Grim", "Vulcan", "Watchdog"));

    private final Random random = new Random();

    public KillAura() {
        super("KillAura", "Automatically attacks nearby valid entities.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        LivingEntity target = CombatUtil.bestLivingTarget(
                client, range.get(),
                players.enabled(), hostiles.enabled(), passives.enabled(),
                throughWalls.enabled(), priority.get()
        ).orElse(null);
        if (target == null) return;

        if (silentRotate.enabled()) {
            float[] rots = CombatUtil.rotationsTo(client.player, target.getEyePos());
            float yawDelta = Math.abs(MathHelper.wrapDegrees(rots[0] - client.player.getYaw()));
            int ticks = Math.max(1, (int) Math.ceil(yawDelta / rotationSpeed.get().floatValue()));
            rotations().setMaxStep(rotationSpeed.get().floatValue());

            if (bypass.is("Grim")) {
                float noiseYaw = (random.nextFloat() - 0.5f) * 2.0f;
                float noisePitch = (random.nextFloat() - 0.5f) * 1.5f;
                rotations().rotateToSilent(rots[0] + noiseYaw, rots[1] + noisePitch, Math.max(2, ticks));
            } else if (bypass.is("Legit")) {
                float noiseYaw = (random.nextFloat() - 0.5f) * 4.0f;
                float noisePitch = (random.nextFloat() - 0.5f) * 2.0f;
                rotations().rotateToSilent(rots[0] + noiseYaw, rots[1] + noisePitch, Math.max(3, ticks + 1));
            } else {
                rotations().rotateToSilent(rots[0], rots[1], Math.max(1, ticks));
            }
        }

        boolean canBypassCooldown = client.player.getAbilities().creativeMode;
        if (useCooldown.enabled() && !canBypassCooldown) {
            float progress = client.player.getAttackCooldownProgress(0.5f);
            if (progress >= 1.0f) {
                if (bypass.is("Grim") || bypass.is("Legit")) {
                    if (random.nextFloat() < 0.08f) return;
                }
                CombatUtil.attack(client, target);
            }
        } else {
            int baseTicks = Math.max(1, (int) Math.round(20.0 / cps.get()));
            int extraJitter = bypass.is("Grim") || bypass.is("Legit") ? jitter.get() + random.nextInt(4) : jitter.get();
            if (actionReady(baseTicks, extraJitter)) {
                if ((bypass.is("Grim") || bypass.is("Legit")) && random.nextFloat() < 0.05f) return;
                CombatUtil.attack(client, target);
            }
        }
    }
}
