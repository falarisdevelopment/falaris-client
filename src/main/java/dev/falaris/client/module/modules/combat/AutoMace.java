package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public final class AutoMace extends CombatModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Attack range.", 3.5, 1.0, 6.0));
    private final DoubleSetting minFallDist = setting(new DoubleSetting("Min Fall Dist", "Minimum fall distance for bonus.", 2.5, 0.0, 10.0));
    private final DoubleSetting cps = setting(new DoubleSetting("CPS", "Attacks per second.", 7.0, 1.0, 20.0));
    private final IntegerSetting jitter = setting(new IntegerSetting("Jitter", "Random extra ticks.", 2, 0, 8));
    private final ModeSetting priority = setting(new ModeSetting("Priority", "Target sort.", "Distance", "Distance", "Health", "Angle"));
    private final BooleanSetting autoSwitch = setting(new BooleanSetting("Auto Switch", "Swap to mace.", true));
    private final BooleanSetting silentRotate = setting(new BooleanSetting("Silent Rotate", "Face target.", true));
    private final DoubleSetting rotationSpeed = setting(new DoubleSetting("Rotation Speed", "Rotation speed.", 25.0, 1.0, 60.0));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Target players.", true));
    private final BooleanSetting hostiles = setting(new BooleanSetting("Hostiles", "Target hostile mobs.", true));
    private final BooleanSetting passives = setting(new BooleanSetting("Passives", "Target passive mobs.", false));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Ignore line of sight.", false));
    private final ModeSetting bypass = setting(new ModeSetting("Bypass", "Anti-cheat.", "Vanilla", "Vanilla", "Vanilla", "Ghost", "Grim", "Legit"));

    // Ghost settings
    private final IntegerSetting faceDelay = setting(new IntegerSetting("Face Delay", "Ticks to face before hitting.", 2, 0, 10));
    private final DoubleSetting missChance = setting(new DoubleSetting("Miss Chance", "% chance to miss.", 5.0, 0.0, 30.0));

    // Boom settings
    private final BooleanSetting autoJump = setting(new BooleanSetting("Auto Jump", "Jump to build fall.", true));
    private final BooleanSetting onlySlam = setting(new BooleanSetting("Only Slam", "Only attack when falling.", false));
    private final DoubleSetting fallAttackRange = setting(new DoubleSetting("Fall Attack Range", "Range during fall slam.", 4.0, 1.0, 7.0));
    private final BooleanSetting airStrafe = setting(new BooleanSetting("Air Strafe", "Control movement in air.", true));
    private final DoubleSetting airStrafeSpeed = setting(new DoubleSetting("Air Strafe Speed", "Horizontal air velocity.", 0.4, 0.1, 2.0));
    private final BooleanSetting autoWind = setting(new BooleanSetting("Auto Wind Charge", "Wind charge to launch.", false));
    private final IntegerSetting windCooldown = setting(new IntegerSetting("Wind Cooldown", "Ticks between wind uses.", 20, 5, 60));

    private final Random random = new Random();
    private int windTick;
    private int faceTicks;
    private boolean hasFaced;

    public AutoMace() {
        super("AutoMace", "Mace slam automation with ghost bypass and fall prediction.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        windTick = 0;
        faceTicks = 0;
        hasFaced = false;
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (windTick > 0) windTick--;

        LivingEntity target = CombatUtil.bestLivingTarget(
            client, range.get() + 2.0,
            players.enabled(), hostiles.enabled(), passives.enabled(),
            throughWalls.enabled(), priority.get()
        ).orElse(null);
        if (target == null) return;

        if (autoSwitch.enabled()) {
            int maceSlot = findMace(client.player);
            if (maceSlot < 0) return;
            client.player.getInventory().setSelectedSlot(maceSlot);
        }

        double dist = client.player.distanceTo(target);
        boolean isFalling = !client.player.isOnGround() && client.player.getVelocity().y < -0.1;
        boolean hasFallDist = client.player.fallDistance >= minFallDist.get();
        boolean isGhost = bypass.is("Ghost") || bypass.is("Legit") || bypass.is("Grim");

        // Wind charge launch
        if (autoWind.enabled() && windTick <= 0 && client.player.isOnGround() && dist <= range.get() + 2.0) {
            int windSlot = findWindCharge(client.player);
            if (windSlot >= 0) {
                client.player.getInventory().setSelectedSlot(windSlot);
                client.player.jump();
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                windTick = windCooldown.get() + random.nextInt(5);
                if (autoSwitch.enabled()) {
                    int mSlot = findMace(client.player);
                    if (mSlot >= 0) client.player.getInventory().setSelectedSlot(mSlot);
                }
            }
        }

        // Air strafe toward target
        if (airStrafe.enabled() && isFalling && dist > range.get()) {
            Vec3d toTarget = new Vec3d(target.getX() - client.player.getX(), 0, target.getZ() - client.player.getZ()).normalize();
            client.player.setVelocity(toTarget.x * airStrafeSpeed.get(), client.player.getVelocity().y, toTarget.z * airStrafeSpeed.get());
        }

        // Jump to build fall
        if (autoJump.enabled() && client.player.isOnGround() && dist <= range.get() + 1.5 && !isGhost) {
            client.player.jump();
        }

        // Face delay for ghost modes
        if (isGhost && !hasFaced && (isFalling || !onlySlam.enabled())) {
            float[] rots = CombatUtil.rotationsTo(client.player, target.getEyePos());
            rotations().setMaxStep(rotationSpeed.get().floatValue());
            float noiseYaw = (random.nextFloat() - 0.5f) * 2.0f;
            float noisePitch = (random.nextFloat() - 0.5f) * 1.0f;
            rotations().rotateToSilent(rots[0] + noiseYaw, rots[1] + noisePitch, Math.max(2, faceDelay.get()));
            faceTicks++;
            if (faceTicks >= faceDelay.get()) { hasFaced = true; faceTicks = 0; }
            return;
        }

        // Attack logic
        boolean canSlam = isFalling && hasFallDist && dist <= fallAttackRange.get();
        boolean canHit = !onlySlam.enabled() && dist <= range.get();

        if (isGhost && !hasFaced && !canSlam && !canHit) return;

        // Rotations
        if (silentRotate.enabled()) {
            float[] rots = CombatUtil.rotationsTo(client.player, target.getEyePos());
            rotations().setMaxStep(rotationSpeed.get().floatValue());

            int rotTicks = isGhost ? Math.max(2, faceDelay.get()) : 1;
            if (isGhost) {
                float ny = (random.nextFloat() - 0.5f) * 2.0f;
                float np = (random.nextFloat() - 0.5f) * 1.0f;
                rotations().rotateToSilent(rots[0] + ny, rots[1] + np, rotTicks);
            } else {
                rotations().rotateToSilent(rots[0], rots[1], rotTicks);
            }
        }

        // Timing
        int minTicks = Math.max(1, (int) Math.round(20.0 / cps.get()));
        int adjustedJitter = isGhost ? jitter.get() + random.nextInt(3) : jitter.get();
        boolean shouldMiss = isGhost && random.nextFloat() * 100 < missChance.get();

        if ((canSlam || canHit) && !shouldMiss && actionReady(minTicks, adjustedJitter)) {
            CombatUtil.attack(client, target);
            if (isGhost) { hasFaced = false; faceTicks = 0; }
        }
    }

    private int findMace(ClientPlayerEntity player) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getStack(slot).isOf(Items.MACE)) return slot;
        }
        return -1;
    }

    private int findWindCharge(ClientPlayerEntity player) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getStack(slot).isOf(Items.WIND_CHARGE)) return slot;
        }
        return -1;
    }
}
