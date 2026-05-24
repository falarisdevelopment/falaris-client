package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.Random;

public final class CrystalAura extends CombatModule {
    private final DoubleSetting targetRange = setting(new DoubleSetting("Target Range", "Max target distance.", 8.0, 1.0, 12.0));
    private final DoubleSetting breakRange = setting(new DoubleSetting("Break Range", "Max crystal break distance.", 3.0, 1.0, 4.0));
    private final DoubleSetting placeRange = setting(new DoubleSetting("Place Range", "Max crystal place distance.", 3.0, 1.0, 4.0));
    private final DoubleSetting minDamage = setting(new DoubleSetting("Min Damage", "Minimum estimated target damage.", 6.0, 1.0, 20.0));
    private final DoubleSetting maxSelfDamage = setting(new DoubleSetting("Max Self Damage", "Maximum estimated self damage.", 8.0, 1.0, 20.0));
    private final DoubleSetting rotationSpeed = setting(new DoubleSetting("Rotation Speed", "Max rotation per tick.", 20.0, 1.0, 60.0));
    private final IntegerSetting actionJitter = setting(new IntegerSetting("Action Jitter", "Random extra ticks between actions.", 2, 0, 8));
    private final BooleanSetting place = setting(new BooleanSetting("Place", "Place end crystals.", true));
    private final BooleanSetting explode = setting(new BooleanSetting("Break", "Break end crystals.", true));
    private final BooleanSetting placeObsidian = setting(new BooleanSetting("Place Obsidian", "Auto-place obsidian before crystal.", true));
    private final BooleanSetting rotate = setting(new BooleanSetting("Rotate", "Face crystal actions.", true));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Allow crystals without line of sight.", false));
    private final ModeSetting priority = setting(new ModeSetting("Priority", "Target sorting mode.", "Distance", "Distance", "Health", "Angle"));
    private final BooleanSetting hitTarget = setting(new BooleanSetting("Hit Target", "Attack the target directly.", true));
    private final DoubleSetting hitTargetRange = setting(new DoubleSetting("Hit Range", "Range to hit the target.", 3.0, 1.0, 3.0));
    private final BooleanSetting autoSwitch = setting(new BooleanSetting("Auto Switch", "Auto switch to sword for melee hits.", true));
    private final ModeSetting bypass = setting(new ModeSetting("Bypass", "Anticheat bypass method.", "Vanilla", "Vanilla", "Legit", "Grim"));

    // Prestige-style additions
    private final ModeSetting breakMode = setting(new ModeSetting("Break Mode", "Crystal break priority.", "Normal", "Normal", "Double Tap", "Chain", "Sequential"));
    private final BooleanSetting silentHeadBob = setting(new BooleanSetting("Silent Head Bob", "Simulate head bobbing for legit look.", true));
    private final BooleanSetting idPrediction = setting(new BooleanSetting("ID Prediction", "Predict entity ID for faster hits.", false));
    private final BooleanSetting feetPlace = setting(new BooleanSetting("Feet Place", "Place obsidian under target feet before crystal.", true));

    private final Random random = new Random();
    private int actionCounter;
    private int headBobPhase;
    private int predictedId;

    public CrystalAura() {
        super("CrystalAura", "Prestige-style crystal PvP with double-tap, ID prediction, silent head bob.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        LivingEntity target = CombatUtil.bestLivingTarget(client, targetRange.get(), true, true, false, throughWalls.enabled(), priority.get()).orElse(null);
        if (target == null) return;

        actionCounter++;

        if (silentHeadBob.enabled()) {
            headBobPhase += 2;
            if (headBobPhase > 360) headBobPhase = 0;
            float bob = MathHelper.sin(headBobPhase * MathHelper.RADIANS_PER_DEGREE) * 0.5f;
            client.player.setPitch(client.player.getPitch() + bob * 0.02f);
        }

        if (bypass.is("Grim")) {
            tickGrim(client, target);
            return;
        }
        if (bypass.is("Legit")) {
            tickLegit(client, target);
            return;
        }

        // Double Tap: break then place at same spot
        if (breakMode.is("Double Tap") && explode.enabled()) {
            Optional<EndCrystalEntity> crystal = CombatUtil.bestCrystal(client, breakRange.get(), throughWalls.enabled());
            if (crystal.isPresent()) {
                tryBreakBest(client);
                if (actionReady(0, 0)) {
                    EndCrystalEntity ec = crystal.get();
                    BlockPos basePos = BlockPos.ofFloored(ec.getX(), ec.getY() - 1, ec.getZ());
                    if (place.enabled() && canPlaceCrystal(client, basePos)) {
                        int slot = CombatUtil.findItem(client.player, Items.END_CRYSTAL);
                        if (slot >= 0 && slot < 9) {
                            CombatUtil.selectHotbarSlot(client.player, slot);
                            CombatUtil.interactBlock(client, basePos, Direction.UP);
                        }
                    }
                }
            }
        }

        // FeetPlace: place obsidian under target
        if (feetPlace.enabled() && placeObsidian.enabled()) {
            tryPlaceObsidianFeet(client, target);
        }

        if (placeObsidian.enabled() && !feetPlace.enabled()) {
            tryPlaceObsidian(client, target);
        }

        if (breakMode.is("Chain")) {
            chainBreak(client);
        } else if (breakMode.is("Sequential")) {
            sequentialBreak(client, target);
        } else if (!breakMode.is("Double Tap")) {
            if (explode.enabled()) tryBreakBest(client);
            if (place.enabled()) tryPlaceBest(client, target);
        }

        hitFallback(client, target);
    }

    private void tickGrim(MinecraftClient client, LivingEntity target) {
        if (explode.enabled() && actionReady(3 + random.nextInt(3), actionJitter.get())) {
            tryBreakBest(client);
        }
        if (placeObsidian.enabled() && actionReady(5 + random.nextInt(3), actionJitter.get())) {
            tryPlaceObsidian(client, target);
        }
        if (place.enabled() && actionReady(2 + random.nextInt(2), actionJitter.get())) {
            tryPlaceBest(client, target);
        }
        if (hitTarget.enabled() && client.player.distanceTo(target) <= hitTargetRange.get()) {
            if (actionReady(4, actionJitter.get())) {
                doMelee(client, target);
            }
        }
    }

    private void tickLegit(MinecraftClient client, LivingEntity target) {
        boolean willBreak = explode.enabled() && random.nextFloat() < 0.6f;
        boolean willObsidian = placeObsidian.enabled() && random.nextFloat() < 0.4f;
        boolean willPlace = place.enabled() && random.nextFloat() < 0.5f;
        boolean willMelee = hitTarget.enabled() && client.player.distanceTo(target) <= hitTargetRange.get() && random.nextFloat() < 0.3f;

        if (willBreak && actionReady(4 + random.nextInt(4), actionJitter.get())) {
            tryBreakBest(client);
        }
        if (willObsidian && actionReady(6 + random.nextInt(2), actionJitter.get())) {
            tryPlaceObsidian(client, target);
        }
        if (willPlace && actionReady(3 + random.nextInt(3), actionJitter.get())) {
            tryPlaceBest(client, target);
        }
        if (willMelee && actionReady(5, actionJitter.get())) {
            doMelee(client, target);
        }
    }

    private void chainBreak(MinecraftClient client) {
        while (explode.enabled()) {
            Optional<EndCrystalEntity> crystal = CombatUtil.bestCrystal(client, breakRange.get(), throughWalls.enabled());
            if (crystal.isEmpty()) break;
            CombatUtil.attack(client, crystal.get());
        }
    }

    private void sequentialBreak(MinecraftClient client, LivingEntity target) {
        if (!explode.enabled()) return;
        Optional<EndCrystalEntity> crystal = CombatUtil.bestCrystal(client, breakRange.get(), throughWalls.enabled());
        if (crystal.isEmpty()) return;

        if (idPrediction.enabled()) {
            try {
                int id = crystal.get().getId();
                if (predictedId != id && predictedId != 0) {
                    int slot = CombatUtil.findItem(client.player, Items.DIAMOND_SWORD);
                    if (slot >= 0) CombatUtil.selectHotbarSlot(client.player, slot);
                }
                predictedId = id;
            } catch (Exception ignored) {}
        }

        tryBreakBest(client);
        if (place.enabled() && actionReady(0, 0)) {
            tryPlaceBest(client, target);
        }
    }

    private boolean tryBreakBest(MinecraftClient client) {
        Optional<EndCrystalEntity> crystal = CombatUtil.bestCrystal(client, breakRange.get(), throughWalls.enabled());
        if (crystal.isEmpty()) return false;

        EndCrystalEntity entity = crystal.get();
        Vec3d targetPos = new Vec3d(entity.getX(), entity.getY() + 0.5, entity.getZ());
        if (rotate.enabled()) {
            faceSmooth(client, targetPos);
        }
        if (actionReady(1, actionJitter.get())) {
            CombatUtil.attack(client, entity);
        }
        return true;
    }

    private boolean tryPlaceObsidianFeet(MinecraftClient client, LivingEntity target) {
        int obsidianSlot = CombatUtil.findItem(client.player, Items.OBSIDIAN);
        if (obsidianSlot == -1) return false;

        BlockPos feetPos = target.getBlockPos();
        if (!client.world.getBlockState(feetPos).isAir()) return false;
        if (client.world.getBlockState(feetPos.down()).isAir()) return false;
        double dist = client.player.squaredDistanceTo(Vec3d.ofCenter(feetPos));
        if (dist > placeRange.get() * placeRange.get()) return false;

        if (rotate.enabled()) {
            faceSmooth(client, Vec3d.ofCenter(feetPos));
        }
        if (actionReady(1, actionJitter.get())) {
            CombatUtil.selectHotbarSlot(client.player, obsidianSlot);
            CombatUtil.interactBlock(client, feetPos, Direction.UP);
        }
        return true;
    }

    private boolean tryPlaceObsidian(MinecraftClient client, LivingEntity target) {
        int obsidianSlot = CombatUtil.findItem(client.player, Items.OBSIDIAN);
        if (obsidianSlot == -1) return false;

        Optional<BlockPos> placePos = findBestObsidianPlacement(client, target);
        if (placePos.isEmpty()) return false;

        BlockPos pos = placePos.get();
        Vec3d center = Vec3d.ofCenter(pos);
        if (rotate.enabled()) {
            faceSmooth(client, center);
        }
        if (actionReady(2, actionJitter.get())) {
            CombatUtil.selectHotbarSlot(client.player, obsidianSlot);
            CombatUtil.interactBlock(client, pos, Direction.UP);
        }
        return true;
    }

    private boolean tryPlaceBest(MinecraftClient client, LivingEntity target) {
        int slot = CombatUtil.findItem(client.player, Items.END_CRYSTAL);
        if (slot == -1) return false;

        Optional<BlockPos> best = findBestCrystalBase(client, target);
        if (best.isEmpty()) return false;

        BlockPos pos = best.get();
        Vec3d placePos = Vec3d.ofCenter(pos.up());
        if (rotate.enabled()) {
            faceSmooth(client, placePos);
        }
        if (actionReady(1, actionJitter.get())) {
            CombatUtil.selectHotbarSlot(client.player, slot);
            CombatUtil.interactBlock(client, pos, Direction.UP);
        }
        return true;
    }

    private void faceSmooth(MinecraftClient client, Vec3d target) {
        float[] rots = CombatUtil.rotationsTo(client.player, target);
        float yawDelta = Math.abs(MathHelper.wrapDegrees(rots[0] - client.player.getYaw()));
        float pitchDelta = Math.abs(rots[1] - client.player.getPitch());
        int ticks = Math.max(1, (int) Math.ceil(Math.max(yawDelta, pitchDelta) / rotationSpeed.get().floatValue()));
        rotations().setMaxStep(rotationSpeed.get().floatValue());
        rotations().rotateTo(rots[0], rots[1], ticks);
    }

    private void doMelee(MinecraftClient client, LivingEntity target) {
        if (autoSwitch.enabled()) {
            int swordSlot = findBestSword(client);
            if (swordSlot != -1 && swordSlot < 9) {
                CombatUtil.selectHotbarSlot(client.player, swordSlot);
            }
        }
        if (actionReady(1, actionJitter.get())) {
            CombatUtil.attack(client, target);
        }
    }

    private int findBestSword(MinecraftClient client) {
        net.minecraft.item.Item[] swords = {Items.NETHERITE_SWORD, Items.DIAMOND_SWORD, Items.IRON_SWORD, Items.STONE_SWORD};
        for (net.minecraft.item.Item sword : swords) {
            int slot = CombatUtil.findItem(client.player, sword);
            if (slot != -1) return slot;
        }
        return -1;
    }

    private void hitFallback(MinecraftClient client, LivingEntity target) {
        if (!hitTarget.enabled()) return;
        if (client.player.distanceTo(target) > hitTargetRange.get()) return;
        if (!actionReady(1, actionJitter.get())) return;
        doMelee(client, target);
    }

    private Optional<BlockPos> findBestObsidianPlacement(MinecraftClient client, LivingEntity target) {
        int radius = (int) Math.ceil(placeRange.get());
        BlockPos origin = target.getBlockPos();
        BlockPos best = null;
        double bestDamage = 0;

        for (BlockPos pos : BlockPos.iterateOutwards(origin, radius, 2, radius)) {
            if (!client.world.getBlockState(pos).isAir()) continue;
            if (client.world.getBlockState(pos.down()).isAir()) continue;
            Vec3d crystalPos = Vec3d.ofCenter(pos.up());
            if (client.player.squaredDistanceTo(crystalPos) > placeRange.get() * placeRange.get()) continue;
            Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
            double dmg = explosionDamage(targetPos, crystalPos, 6.0f, 12.0f);
            if (dmg < minDamage.get()) continue;
            Vec3d selfPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
            double selfDmg = explosionDamage(selfPos, crystalPos, 6.0f, 12.0f);
            if (selfDmg > maxSelfDamage.get()) continue;
            if (dmg > bestDamage) {
                best = pos.toImmutable();
                bestDamage = dmg;
            }
        }
        return Optional.ofNullable(best);
    }

    private Optional<BlockPos> findBestCrystalBase(MinecraftClient client, LivingEntity target) {
        int radius = (int) Math.ceil(placeRange.get());
        BlockPos origin = target.getBlockPos();
        BlockPos best = null;
        double bestDamage = 0;

        for (BlockPos pos : BlockPos.iterateOutwards(origin, radius, 2, radius)) {
            Vec3d crystalPos = Vec3d.ofCenter(pos.up());
            if (!canPlaceCrystal(client, pos)) continue;
            if (client.player.squaredDistanceTo(crystalPos) > placeRange.get() * placeRange.get()) continue;
            Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
            double dmg = explosionDamage(targetPos, crystalPos, 6.0f, 12.0f);
            if (dmg < minDamage.get()) continue;
            Vec3d selfPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
            double selfDmg = explosionDamage(selfPos, crystalPos, 6.0f, 12.0f);
            if (selfDmg > maxSelfDamage.get()) continue;
            if (dmg > bestDamage) {
                best = pos.toImmutable();
                bestDamage = dmg;
            }
        }
        return Optional.ofNullable(best);
    }

    private boolean canPlaceCrystal(MinecraftClient client, BlockPos pos) {
        return (client.world.getBlockState(pos).isOf(Blocks.OBSIDIAN) || client.world.getBlockState(pos).isOf(Blocks.BEDROCK))
            && client.world.getBlockState(pos.up()).isAir();
    }

    private double explosionDamage(Vec3d target, Vec3d source, float power, float maxDamage) {
        double distance = target.distanceTo(source);
        double exposure = 1.0;
        double impact = (1.0 - distance / (2.0 * power)) * exposure;
        impact = MathHelper.clamp(impact, 0.0, 1.0);
        return (impact * impact + impact) / 2.0 * maxDamage * 1.5 + 1.0;
    }
}
