package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;

public final class AimBot extends CombatModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum lock distance.", 6.0, 1.0, 12.0));
    private final DoubleSetting rotationSpeed = setting(new DoubleSetting("Rotation Speed", "Maximum rotation step per tick.", 14.0, 1.0, 60.0));
    private final ModeSetting priority = setting(new ModeSetting("Priority", "Target sorting mode.", "Angle", "Distance", "Health", "Angle"));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Target players.", true));
    private final BooleanSetting hostiles = setting(new BooleanSetting("Hostiles", "Target hostile mobs.", true));
    private final BooleanSetting passives = setting(new BooleanSetting("Passives", "Target passive mobs.", false));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Allow targets without line of sight.", false));
    private final BooleanSetting focusBlocks = setting(new BooleanSetting("Focus Blocks", "Aim at the block you are mining.", false));
    private final DoubleSetting smoothing = setting(new DoubleSetting("Smoothing", "Higher = smoother but slower aim.", 3.0, 1.0, 15.0));

    private float prevYaw, prevPitch;
    private boolean hasPrev;

    public AimBot() {
        super("AimBot", "Smoothly locks view onto the best target.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        hasPrev = false;
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) return;

        if (focusBlocks.enabled()) {
            handleBlockFocus(client);
            return;
        }

        LivingEntity target = CombatUtil.bestLivingTarget(
                client, range.get(),
                players.enabled(), hostiles.enabled(), passives.enabled(),
                throughWalls.enabled(), priority.get()
        ).orElse(null);

        if (target == null) {
            hasPrev = false;
            return;
        }

        float[] rots = CombatUtil.rotationsTo(client.player, target.getEyePos());

        float targetYaw = rots[0];
        float targetPitch = rots[1];

        if (!hasPrev) {
            prevYaw = client.player.getYaw();
            prevPitch = client.player.getPitch();
            hasPrev = true;
        }

        float step = rotationSpeed.get().floatValue();
        smoothRotate(client, targetYaw, targetPitch, step);
    }

    private void smoothRotate(MinecraftClient client, float targetYaw, float targetPitch, float step) {
        float yawDelta = MathHelper.wrapDegrees(targetYaw - prevYaw);
        float pitchDelta = targetPitch - prevPitch;

        float smoothFactor = smoothing.get().floatValue();
        float smoothYaw = prevYaw + MathHelper.clamp(yawDelta / smoothFactor, -step, step);
        float smoothPitch = prevPitch + MathHelper.clamp(pitchDelta / smoothFactor, -step, step);

        prevYaw = smoothYaw;
        prevPitch = smoothPitch;

        client.player.setYaw(smoothYaw);
        client.player.setPitch(MathHelper.clamp(smoothPitch, -90.0f, 90.0f));
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
                smoothRotate(client, rots[0], rots[1], rotationSpeed.get().floatValue());
            }
        } catch (Exception ignored) {}
    }
}
