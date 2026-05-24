package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

public final class AimAssist extends CombatModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum assist distance.", 5.5, 1.0, 10.0));
    private final DoubleSetting fov = setting(new DoubleSetting("FOV", "Maximum angle from crosshair.", 55.0, 5.0, 180.0));
    private final DoubleSetting strength = setting(new DoubleSetting("Strength", "Assist rotation speed.", 6.0, 0.5, 30.0));
    private final ModeSetting priority = setting(new ModeSetting("Priority", "Target sorting mode.", "Angle", "Distance", "Health", "Angle"));
    private final BooleanSetting requireAttack = setting(new BooleanSetting("Require Attack", "Only assist while the attack key is held.", true));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Target players.", true));
    private final BooleanSetting hostiles = setting(new BooleanSetting("Hostiles", "Target hostile mobs.", true));
    private final BooleanSetting passives = setting(new BooleanSetting("Passives", "Target passive mobs.", false));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Allow targets without line of sight.", false));
    private final BooleanSetting focusBlocks = setting(new BooleanSetting("Focus Blocks", "Aim at the block you are mining.", false));
    private final ModeSetting bypass = setting(new ModeSetting("Bypass", "Anticheat bypass.", "Vanilla", "Vanilla", "Legit", "Grim"));

    private final Random random = new Random();

    public AimAssist() {
        super("AimAssist", "Smoothly adjusts your aim toward nearby targets.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) return;
        if (requireAttack.enabled() && !client.options.attackKey.isPressed()) return;

        if (focusBlocks.enabled()) {
            handleBlockFocus(client);
            return;
        }

        LivingEntity target = CombatUtil.bestLivingTarget(
                client, range.get(),
                players.enabled(), hostiles.enabled(), passives.enabled(),
                throughWalls.enabled(), priority.get()
        ).filter(entity -> CombatUtil.angleTo(client.player, entity.getEyePos()) <= fov.get()).orElse(null);

        if (target == null) return;

        float[] rots = CombatUtil.rotationsTo(client.player, target.getEyePos());

        if (bypass.is("Legit") || bypass.is("Grim")) {
            if (random.nextFloat() < 0.20f) return;
            rots[0] += (random.nextFloat() - 0.5f) * 3.0f;
            rots[1] += (random.nextFloat() - 0.5f) * 1.5f;
        }

        float strengthVal = strength.get().floatValue();

        float yawDelta = MathHelper.wrapDegrees(rots[0] - client.player.getYaw());
        float pitchDelta = rots[1] - client.player.getPitch();

        float yawStep = MathHelper.clamp(yawDelta / 3.0f, -strengthVal, strengthVal);
        float pitchStep = MathHelper.clamp(pitchDelta / 3.0f, -strengthVal * 0.6f, strengthVal * 0.6f);

        if (Math.abs(yawDelta) > 0.5f || Math.abs(pitchDelta) > 0.5f) {
            client.player.setYaw(client.player.getYaw() + yawStep);
            client.player.setPitch(MathHelper.clamp(client.player.getPitch() + pitchStep, -90.0f, 90.0f));
        }
    }

    private void handleBlockFocus(MinecraftClient client) {
        try {
            java.lang.reflect.Field field = client.interactionManager.getClass().getDeclaredField("currentBreakingPos");
            field.setAccessible(true);
            net.minecraft.util.math.BlockPos breakingPos = (net.minecraft.util.math.BlockPos) field.get(client.interactionManager);
            if (breakingPos == null && client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                breakingPos = ((net.minecraft.util.hit.BlockHitResult) client.crosshairTarget).getBlockPos();
            }
            if (breakingPos != null && !client.world.getBlockState(breakingPos).isAir()) {
                float[] rots = CombatUtil.rotationsTo(client.player, breakingPos.toCenterPos());
                float yawDelta = MathHelper.wrapDegrees(rots[0] - client.player.getYaw());
                float pitchDelta = rots[1] - client.player.getPitch();
                float strengthVal = strength.get().floatValue();
                client.player.setYaw(client.player.getYaw() + MathHelper.clamp(yawDelta / 3.0f, -strengthVal, strengthVal));
                client.player.setPitch(MathHelper.clamp(client.player.getPitch() + MathHelper.clamp(pitchDelta / 3.0f, -strengthVal, strengthVal), -90.0f, 90.0f));
            }
        } catch (Exception ignored) {}
    }
}
