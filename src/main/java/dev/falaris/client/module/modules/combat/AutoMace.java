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
    private final DoubleSetting minFallDist = setting(new DoubleSetting("Min Fall Dist", "Minimum fall distance for bonus.", 2.0, 0.0, 10.0));
    private final DoubleSetting cps = setting(new DoubleSetting("CPS", "Attacks per second.", 8.0, 1.0, 20.0));
    private final IntegerSetting jitter = setting(new IntegerSetting("Jitter", "Random extra ticks.", 2, 0, 8));
    private final ModeSetting priority = setting(new ModeSetting("Priority", "Target sort.", "Distance", "Distance", "Health", "Angle"));
    private final BooleanSetting autoSwitch = setting(new BooleanSetting("Auto Switch", "Swap to mace.", true));
    private final BooleanSetting autoJump = setting(new BooleanSetting("Auto Jump", "Jump to build fall distance.", true));
    private final BooleanSetting silentRotate = setting(new BooleanSetting("Silent Rotate", "Face target.", true));
    private final DoubleSetting rotationSpeed = setting(new DoubleSetting("Rotation Speed", "Rotation speed.", 25.0, 1.0, 60.0));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Target players.", true));
    private final BooleanSetting hostiles = setting(new BooleanSetting("Hostiles", "Target hostile mobs.", true));
    private final BooleanSetting passives = setting(new BooleanSetting("Passives", "Target passive mobs.", false));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Ignore line of sight.", false));
    private final ModeSetting bypass = setting(new ModeSetting("Bypass", "Anti-cheat.", "Vanilla", "Vanilla", "Legit", "Grim"));

    private final BooleanSetting autoWind = setting(new BooleanSetting("Auto Wind Charge", "Use wind charge to launch upward.", true));
    private final DoubleSetting windRange = setting(new DoubleSetting("Wind Range", "Target range to wind charge up.", 6.0, 2.0, 20.0));
    private final BooleanSetting airStrafe = setting(new BooleanSetting("Air Strafe", "Control movement in air toward target.", true));
    private final DoubleSetting airStrafeSpeed = setting(new DoubleSetting("Air Strafe Speed", "Horizontal air velocity.", 0.5, 0.1, 2.0));
    private final BooleanSetting onlySlam = setting(new BooleanSetting("Only Slam", "Only attack when falling with fall distance.", false));
    private final DoubleSetting fallAttackRange = setting(new DoubleSetting("Fall Attack Range", "Range during fall slam.", 4.0, 1.0, 7.0));

    // Prestige-style additions
    private final BooleanSetting breachCheck = setting(new BooleanSetting("Breach Check", "Only slam if target armor is not breached.", true));
    private final BooleanSetting triggerbotTiming = setting(new BooleanSetting("Triggerbot Timing", "Hit exactly when crosshair on target.", false));
    private final DoubleSetting breachArmorThreshold = setting(new DoubleSetting("Armor Threshold", "Max armor % to consider breached.", 0.3, 0.1, 1.0));

    // Wind charge integration
    private final BooleanSetting shortWindCharge = setting(new BooleanSetting("Short Wind Charge", "Fire wind at feet to launch up.", false));
    private final IntegerSetting shortWindCooldown = setting(new IntegerSetting("Short Wind Cooldown", "Ticks between short wind charges.", 30, 10, 80));

    private final Random random = new Random();
    private int windCooldown;
    private int shortWindCooldownCounter;

    public AutoMace() {
        super("AutoMace", "Prestige-style mace automation with breach check and wind charge integration.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        windCooldown = 0;
        shortWindCooldownCounter = 0;
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (windCooldown > 0) windCooldown--;
        if (shortWindCooldownCounter > 0) shortWindCooldownCounter--;

        LivingEntity target = CombatUtil.bestLivingTarget(
                client, range.get() + 2.0,
                players.enabled(), hostiles.enabled(), passives.enabled(),
                throughWalls.enabled(), priority.get()
        ).orElse(null);
        if (target == null) return;

        boolean isFalling = !client.player.isOnGround() && client.player.getVelocity().y < -0.1;
        boolean hasFallDist = client.player.fallDistance >= minFallDist.get();

        if (autoSwitch.enabled()) {
            int maceSlot = findMace(client.player);
            if (maceSlot < 0) return;
            if (maceSlot < 9) client.player.getInventory().setSelectedSlot(maceSlot);
        }

        double dist = client.player.distanceTo(target);

        // Short Wind Charge at feet to launch up (Prestige-style)
        if (shortWindCharge.enabled() && shortWindCooldownCounter <= 0 && client.player.isOnGround() && dist <= range.get() + 2.0) {
            int windSlot = findWindCharge(client.player);
            if (windSlot >= 0 && windSlot < 9) {
                client.player.getInventory().setSelectedSlot(windSlot);
                client.player.jump();
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                shortWindCooldownCounter = shortWindCooldown.get() + random.nextInt(10);
                windCooldown = 5;
            }
        }

        // Wind charge launch toward target
        if (autoWind.enabled() && windCooldown <= 0 && client.player.isOnGround() && dist <= windRange.get() && shortWindCooldownCounter <= 0) {
            int windSlot = findWindCharge(client.player);
            if (windSlot >= 0 && windSlot < 9) {
                if (silentRotate.enabled()) {
                    float[] rots = CombatUtil.rotationsTo(client.player, target.getEyePos());
                    rotations().setMaxStep(rotationSpeed.get().floatValue());
                    rotations().rotateToSilent(rots[0], rots[1], 2);
                }
                client.player.getInventory().setSelectedSlot(windSlot);
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                windCooldown = 10 + random.nextInt(5);
            }
        }

        // Air strafe toward target when falling
        if (airStrafe.enabled() && isFalling && dist > range.get()) {
            Vec3d toTarget = new Vec3d(target.getX() - client.player.getX(), 0, target.getZ() - client.player.getZ()).normalize();
            double vx = toTarget.x * airStrafeSpeed.get();
            double vz = toTarget.z * airStrafeSpeed.get();
            client.player.setVelocity(vx, client.player.getVelocity().y, vz);
        }

        if (silentRotate.enabled()) {
            float[] rots = CombatUtil.rotationsTo(client.player, target.getEyePos());
            float yawDelta = Math.abs(MathHelper.wrapDegrees(rots[0] - client.player.getYaw()));
            int ticks = Math.max(1, (int) Math.ceil(yawDelta / rotationSpeed.get().floatValue()));
            rotations().setMaxStep(rotationSpeed.get().floatValue());

            if (bypass.is("Grim") || bypass.is("Legit")) {
                float noiseYaw = (random.nextFloat() - 0.5f) * 2.0f;
                float noisePitch = (random.nextFloat() - 0.5f) * 1.0f;
                rotations().rotateToSilent(rots[0] + noiseYaw, rots[1] + noisePitch, Math.max(2, ticks));
            } else {
                rotations().rotateToSilent(rots[0], rots[1], Math.max(1, ticks));
            }
        }

        // Jump to build fall distance when close
        if (autoJump.enabled() && client.player.isOnGround() && dist <= range.get() + 1.5 && shortWindCooldownCounter <= 0) {
            client.player.jump();
        }

        int minTicks = Math.max(1, (int) Math.round(20.0 / cps.get()));
        boolean shouldMiss = (bypass.is("Grim") || bypass.is("Legit")) && random.nextFloat() < 0.08f;
        boolean canSlam = isFalling && hasFallDist && dist <= fallAttackRange.get();
        boolean canHit = !onlySlam.enabled() && dist <= range.get();
        boolean groundHit = !onlySlam.enabled() && client.player.isOnGround() && dist <= range.get() && client.player.fallDistance < 0.5f;

        // Breach check: skip slam if target's armor is already breached
        if (breachCheck.enabled() && canSlam) {
            double targetArmor = target.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ARMOR);
            double maxArmor = 20.0;
            double armorRatio = targetArmor / maxArmor;
            if (armorRatio < breachArmorThreshold.get()) {
                canSlam = false;
            }
        }

        // Triggerbot timing: only hit when crosshair is on target
        if (triggerbotTiming.enabled()) {
            var crosshair = client.crosshairTarget;
            if (crosshair == null || crosshair.getType() != net.minecraft.util.hit.HitResult.Type.ENTITY) {
                canSlam = false;
                canHit = false;
                groundHit = false;
            }
        }

        if ((canSlam || canHit || groundHit) && !shouldMiss) {
            if (actionReady(minTicks, jitter.get())) {
                CombatUtil.attack(client, target);
            }
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
