package dev.falaris.client.module.modules.combat;

import dev.falaris.client.FalarisClient;
import dev.falaris.client.backtrack.BacktrackManager;
import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import dev.falaris.client.rotation.RotationManager;
import dev.falaris.client.tick.TickScheduler;
import dev.falaris.client.util.PositionExtrapolation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public final class KillAura extends CombatModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum attack distance.", 3.0, 1.0, 6.0));
    private final BooleanSetting useCooldown = setting(new BooleanSetting("Use Cooldown", "Wait for weapon to fully charge.", true));
    private final DoubleSetting minCps = setting(new DoubleSetting("Min CPS", "Minimum attacks/sec.", 5.0, 1.0, 15.0));
    private final DoubleSetting maxCps = setting(new DoubleSetting("Max CPS", "Maximum attacks/sec.", 9.0, 1.0, 20.0));
    private final IntegerSetting jitter = setting(new IntegerSetting("Delay Jitter", "Random extra ticks.", 2, 0, 8));
    private final DoubleSetting rotationSpeed = setting(new DoubleSetting("Rotation Speed", "Max rotation per tick.", 18.0, 1.0, 60.0));
    private final ModeSetting priority = setting(new ModeSetting("Priority", "Target sort mode.", "Distance", "Distance", "Health", "Angle"));
    private final BooleanSetting silentRotate = setting(new BooleanSetting("Silent Rotate", "Rotate without moving head.", true));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Target players.", true));
    private final BooleanSetting hostiles = setting(new BooleanSetting("Hostiles", "Target hostile mobs.", true));
    private final BooleanSetting passives = setting(new BooleanSetting("Passives", "Target passive mobs.", false));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Allow targets without line of sight.", false));
    private final BooleanSetting antiBot = setting(new BooleanSetting("Anti Bot", "Skip ignored entities.", true));
    private final ModeSetting bypass = setting(new ModeSetting("Bypass", "Anticheat bypass method.", "Vanilla", "Vanilla", "Ghost", "Grim", "Vulcan", "Watchdog"));

    private final IntegerSetting faceDelay = setting(new IntegerSetting("Face Delay", "Ticks to face before hitting.", 2, 0, 10));
    private final DoubleSetting missChance = setting(new DoubleSetting("Miss Chance", "% chance to miss.", 3.0, 0.0, 30.0));
    private final IntegerSetting targetSwitchDelay = setting(new IntegerSetting("Target Switch Delay", "Ticks before switching.", 10, 0, 40));
    private final IntegerSetting postRotateDelay = setting(new IntegerSetting("Post-Rotate Delay", "Ticks after facing before attack.", 1, 0, 5));
    private final BooleanSetting extrapolate = setting(new BooleanSetting("Extrapolate", "Skip attack if target will leave range before next click.", true));

    private final Random random = new Random();
    private int faceTicks;
    private int postRotateTicks;
    private boolean hasFaced;
    private boolean postRotateDone;
    private LivingEntity currentTarget;
    private int switchCooldown;
    private int attackCooldownTicks;
    private float currentCps;

    // OnTick rotation state
    private float[] lastOnTickRots;

    // Backtrack support
    private Vec3d backtrackTargetPos;
    private boolean onTickAttackPending;

    public KillAura() {
        super("KillAura", "Automatically attacks nearby valid entities with ghost bypass.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        faceTicks = 0;
        postRotateTicks = 0;
        hasFaced = false;
        postRotateDone = false;
        currentTarget = null;
        switchCooldown = 0;
        attackCooldownTicks = 0;
        currentCps = (minCps.get().floatValue() + maxCps.get().floatValue()) / 2.0f;
        lastOnTickRots = null;
        onTickAttackPending = false;
        PositionExtrapolation.clear();
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.getNetworkHandler() == null) return;
        if (switchCooldown > 0) switchCooldown--;
        if (attackCooldownTicks > 0) attackCooldownTicks--;

        LivingEntity target = CombatUtil.bestLivingTarget(
            client, range.get(),
            players.enabled(), hostiles.enabled(), passives.enabled(),
            throughWalls.enabled(), priority.get()
        ).orElse(null);
        if (target == null) { currentTarget = null; return; }

        backtrackTargetPos = getBacktrackedPos(target);
        Vec3d targetPos = backtrackTargetPos != null ? backtrackTargetPos : target.getEyePos();
        double effectiveRange = range.get();
        if (backtrackTargetPos != null) {
            effectiveRange += 0.5;
        }
        if (client.player.squaredDistanceTo(targetPos) > effectiveRange * effectiveRange) return;

        PositionExtrapolation.record(target);
        if (extrapolate.enabled()) {
            int predTicks = computeBaseTicks(bypass.is("Grim"));
            if (PositionExtrapolation.willLeaveRange(client.player, target, effectiveRange, predTicks)) return;
        }

        if (target != currentTarget) {
            if (switchCooldown > 0) return;
            currentTarget = target;
            switchCooldown = targetSwitchDelay.get() + random.nextInt(5);
            hasFaced = false;
            postRotateDone = false;
            faceTicks = 0;
            postRotateTicks = 0;
        }
        if (antiBot.enabled() && FalarisClient.getInstance().getIgnoresManager().isIgnored(currentTarget.getName().getString())) return;

        boolean isGrim = bypass.is("Grim");
        boolean useBypassTiming = bypass.is("Ghost") || isGrim || bypass.is("Vulcan") || bypass.is("Watchdog");
        boolean isGhost = bypass.is("Ghost");

        if (isGrim) {
            rotations().setEasing(RotationManager.Easing.EASE_IN_OUT);
        }

        if (useBypassTiming && !hasFaced) {
            float[] rots = CombatUtil.rotationsTo(client.player, targetPos);
            rotations().setMaxStep(rotationSpeed.get().floatValue());
            int ticks = Math.max(2, faceDelay.get());
            float ny = (random.nextFloat() - 0.5f) * 2.0f;
            float np = (random.nextFloat() - 0.5f) * 1.0f;
            rotations().rotateToSilent(rots[0] + ny, rots[1] + np, ticks);
            rotations().setServerRotation(rots[0], rots[1], ticks);
            faceTicks++;
            if (faceTicks >= faceDelay.get()) {
                hasFaced = true;
                faceTicks = 0;
                postRotateTicks = 0;
                postRotateDone = false;
            }
            return;
        }

        if (useBypassTiming && hasFaced && !postRotateDone) {
            postRotateTicks++;
            if (postRotateTicks >= Math.max(1, isGrim ? postRotateDelay.get() + random.nextInt(2) : postRotateDelay.get())) {
                postRotateDone = true;
            } else {
                return;
            }
        }

        if (silentRotate.enabled()) {
            float[] rots = CombatUtil.rotationsTo(client.player, targetPos);
            float yawDelta = Math.abs(MathHelper.wrapDegrees(rots[0] - client.player.getYaw()));
            int ticks = Math.max(1, (int) Math.ceil(yawDelta / rotationSpeed.get().floatValue()));
            rotations().setMaxStep(rotationSpeed.get().floatValue());

            if (useBypassTiming) {
                float ny = (random.nextFloat() - 0.5f) * 2.0f;
                float np = (random.nextFloat() - 0.5f) * 1.5f;
                rotations().rotateToSilent(rots[0] + ny, rots[1] + np, Math.max(2, ticks));
                rotations().setServerRotation(rots[0], rots[1], ticks);
                lastOnTickRots = rots;
            } else {
                rotations().rotateToSilent(rots[0], rots[1], Math.max(1, ticks));
            }
        }

        if (attackCooldownTicks > 0) return;

        if (useCooldown.enabled() && !client.player.getAbilities().creativeMode) {
            float progress = client.player.getAttackCooldownProgress(1.0f);
            if (progress < 1.0f) return;
        }

        int baseTicks = computeBaseTicks(isGrim);
        int extraJitter = useBypassTiming ? jitter.get() + random.nextInt(3) : jitter.get();
        boolean shouldMiss = useBypassTiming && random.nextFloat() * 100 < missChance.get();

        if (!shouldMiss) {
            // OnTick rotation: send rotation packet before attack, revert after
            if (isGrim && lastOnTickRots != null) {
                client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    lastOnTickRots[0], lastOnTickRots[1],
                    client.player.isOnGround(), client.player.horizontalCollision
                ));
            }

            CombatUtil.attack(client, target);

            // Revert rotation
            if (isGrim && lastOnTickRots != null) {
                client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                    client.player.getYaw(), client.player.getPitch(),
                    client.player.isOnGround(), client.player.horizontalCollision
                ));
            }

            int cd = useCooldown.enabled() ? Math.max(1, baseTicks / 2) : baseTicks + random.nextInt(extraJitter + 1);
            if (useBypassTiming && !useCooldown.enabled()) {
                TickScheduler.getInstance().scheduleOnce("killaura-reset", () -> {
                    hasFaced = false;
                    postRotateDone = false;
                    faceTicks = 0;
                    postRotateTicks = 0;
                }, cd);
            }
            attackCooldownTicks = cd;
        }

        if (isGrim) {
            rotations().setEasing(RotationManager.Easing.LINEAR);
        }
    }

    private int computeBaseTicks(boolean isGrim) {
        if (isGrim) {
            currentCps += (random.nextGaussian() * 0.5);
            currentCps = MathHelper.clamp(currentCps, minCps.get().floatValue(), maxCps.get().floatValue());
            return Math.max(1, (int) Math.round(20.0 / currentCps));
        }
        return Math.max(1, (int) Math.round(20.0 / (minCps.get() + random.nextDouble() * (maxCps.get() - minCps.get()))));
    }

    private Vec3d getBacktrackedPos(LivingEntity target) {
        var bt = FalarisClient.getInstance().getModuleManager().find("backtrack");
        if (bt.isEmpty() || !bt.get().isEnabled()) return null;
        Backtrack backtrack = (Backtrack) bt.get();
        var tp = BacktrackManager.getInstance().getBacktracked(target.getId(), backtrack.getDelayMs());
        return tp != null ? tp.pos.add(0, target.getEyeHeight(target.getPose()), 0) : null;
    }
}
