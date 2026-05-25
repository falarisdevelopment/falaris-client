package dev.falaris.client.module.modules.combat;

import dev.falaris.client.FalarisClient;
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
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.Random;

public final class CrystalAura extends CombatModule {
    // Targeting
    private final DoubleSetting targetRange = setting(new DoubleSetting("Target Range", "Max target distance.", 8.0, 1.0, 12.0));
    private final DoubleSetting breakRange = setting(new DoubleSetting("Break Range", "Max crystal break distance.", 3.5, 1.0, 5.0));
    private final DoubleSetting placeRange = setting(new DoubleSetting("Place Range", "Max crystal place distance.", 3.5, 1.0, 5.0));
    private final DoubleSetting minDamage = setting(new DoubleSetting("Min Damage", "Minimum estimated target damage.", 6.0, 1.0, 20.0));
    private final DoubleSetting maxSelfDamage = setting(new DoubleSetting("Max Self Damage", "Maximum estimated self damage.", 8.0, 1.0, 20.0));
    private final ModeSetting priority = setting(new ModeSetting("Priority", "Target sorting mode.", "Distance", "Distance", "Health", "Angle"));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Allow targets without line of sight.", false));
    private final BooleanSetting antiBot = setting(new BooleanSetting("Anti Bot", "Skip bot-checked entities.", true));

    // Action toggles
    private final BooleanSetting place = setting(new BooleanSetting("Place", "Place end crystals.", true));
    private final BooleanSetting explode = setting(new BooleanSetting("Break", "Break end crystals.", true));
    private final BooleanSetting placeObsidian = setting(new BooleanSetting("Place Obsidian", "Auto-place obsidian before crystal.", true));
    private final BooleanSetting feetPlace = setting(new BooleanSetting("Feet Place", "Place obsidian under target feet.", true));
    private final BooleanSetting hitTarget = setting(new BooleanSetting("Hit Target", "Melee the target directly.", true));
    private final DoubleSetting hitRange = setting(new DoubleSetting("Melee Range", "Range for melee hits.", 3.0, 1.0, 3.5));

    // Bypass
    private final ModeSetting bypass = setting(new ModeSetting("Bypass", "Anticheat bypass method.", "Vanilla", "Vanilla", "Vanilla", "Ghost", "Grim", "Legit"));
    private final IntegerSetting faceDelay = setting(new IntegerSetting("Face Delay", "Ticks to face target before action (ghost).", 2, 0, 10));
    private final IntegerSetting targetSwitchDelay = setting(new IntegerSetting("Target Switch Delay", "Ticks before switching targets.", 10, 0, 40));
    private final DoubleSetting rotationSpeed = setting(new DoubleSetting("Rotation Speed", "Max rotation per tick.", 20.0, 1.0, 60.0));
    private final DoubleSetting minCps = setting(new DoubleSetting("Min CPS", "Minimum clicks/sec.", 3.0, 1.0, 12.0));
    private final DoubleSetting maxCps = setting(new DoubleSetting("Max CPS", "Maximum clicks/sec.", 8.0, 1.0, 20.0));
    private final DoubleSetting missChance = setting(new DoubleSetting("Miss Chance", "Chance to randomly miss a hit %.", 5.0, 0.0, 30.0));
    private final IntegerSetting actionJitter = setting(new IntegerSetting("Action Jitter", "Random extra ticks.", 2, 0, 8));

    // Auto switch
    private final BooleanSetting autoSwitchSword = setting(new BooleanSetting("Auto Sword", "Switch to sword for melee.", true));
    private final BooleanSetting autoSwitchPick = setting(new BooleanSetting("Auto Pick", "Switch to pick for crystal break.", false));

    private final Random random = new Random();
    private int faceTicks;
    private boolean hasFaced;
    private LivingEntity currentTarget;
    private int targetSwitchCooldown;
    private ActionPhase phase = ActionPhase.BREAK;
    private int ghostPreTicks;

    private enum ActionPhase { BREAK, PLACE, OBSIDIAN, MELEE }

    public CrystalAura() {
        super("CrystalAura", "Crystal PvP aura with ghost bypass, face delay, and smart action sequencing.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        faceTicks = 0;
        hasFaced = false;
        currentTarget = null;
        targetSwitchCooldown = 0;
        phase = ActionPhase.BREAK;
        ghostPreTicks = 0;
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        if (targetSwitchCooldown > 0) targetSwitchCooldown--;

        // Find/switch target
        LivingEntity target = CombatUtil.bestLivingTarget(client, targetRange.get(), true, true, false, throughWalls.enabled(), priority.get()).orElse(null);
        if (target == null) { currentTarget = null; return; }

        if (target != currentTarget && targetSwitchCooldown <= 0) {
            currentTarget = target;
            targetSwitchCooldown = targetSwitchDelay.get() + random.nextInt(5);
            hasFaced = false;
            faceTicks = 0;
        }
        if (currentTarget == null) return;
        if (antiBot.enabled() && FalarisClient.getInstance().getIgnoresManager().isIgnored(currentTarget.getName().getString())) return;

        boolean isGhost = bypass.is("Ghost") || bypass.is("Legit") || bypass.is("Grim");

        // Face delay for ghost modes
        if (isGhost && !hasFaced) {
            float[] rots = CombatUtil.rotationsTo(client.player, currentTarget.getEyePos());
            rotations().setMaxStep(rotationSpeed.get().floatValue());
            rotations().rotateToSilent(rots[0], rots[1], Math.max(1, faceDelay.get()));
            faceTicks++;
            if (faceTicks >= faceDelay.get()) {
                hasFaced = true;
                faceTicks = 0;
            }
            return;
        }

        // Ghost: tick-based action sequencing
        if (isGhost) {
            tickGhost(client, currentTarget);
        } else {
            tickVanilla(client, currentTarget);
        }
    }

    private void tickGhost(MinecraftClient client, LivingEntity target) {
        // Pre-action facing: face the current action's target before executing
        if (ghostPreTicks > 0) {
            ghostPreTicks--;
            faceCurrentAction(client, target);
            return;
        }

        // Execute the current action
        executePhase(client, target);

        // Advance sequence after a brief delay
        if (!actionReady(Math.max(1, faceDelay.get()), actionJitter.get())) return;
        advancePhase();
    }

    private void executePhase(MinecraftClient client, LivingEntity target) {
        switch (phase) {
            case BREAK -> {
                if (explode.enabled()) {
                    tryBreakBest(client);
                }
            }
            case PLACE -> {
                if (place.enabled()) {
                    tryPlaceBest(client, target);
                }
            }
            case OBSIDIAN -> {
                if (placeObsidian.enabled()) {
                    boolean placed = false;
                    if (feetPlace.enabled()) placed = tryPlaceObsidianFeet(client, target);
                    if (!placed) {
                        int slot = CombatUtil.findItem(client.player, Items.OBSIDIAN);
                        if (slot != -1) tryPlaceObsidian(client, target, slot);
                    }
                }
            }
            case MELEE -> {
                boolean willMiss = random.nextFloat() * 100 < missChance.get();
                if (hitTarget.enabled() && client.player.distanceTo(target) <= hitRange.get() && !willMiss) {
                    doMelee(client, target);
                }
            }
        }
    }

    private void advancePhase() {
        float r = random.nextFloat();
        if (r < 0.25f) {
            phase = ActionPhase.BREAK;
        } else if (r < 0.50f) {
            phase = ActionPhase.PLACE;
        } else if (r < 0.75f) {
            phase = ActionPhase.OBSIDIAN;
        } else {
            phase = ActionPhase.MELEE;
        }
        ghostPreTicks = Math.max(1, faceDelay.get());
    }

    private void faceCurrentAction(MinecraftClient client, LivingEntity target) {
        switch (phase) {
            case BREAK -> {
                EndCrystalEntity crystal = findCrystalToBreak(client);
                if (crystal != null) faceCrystal(client, crystal);
            }
            case PLACE -> faceTargetPos(client, target);
            case OBSIDIAN -> faceTargetPos(client, target);
            case MELEE -> faceTargetPos(client, target);
        }
    }

    private void faceTargetPos(MinecraftClient client, LivingEntity target) {
        faceSmooth(client, target.getEyePos());
    }

    private void tickVanilla(MinecraftClient client, LivingEntity target) {
        // Feet obsidian
        if (placeObsidian.enabled() && feetPlace.enabled()) {
            tryPlaceObsidianFeet(client, target);
        }
        if (placeObsidian.enabled() && !feetPlace.enabled()) {
            tryPlaceObsidian(client, target);
        }

        // Break
        if (explode.enabled() && tryBreakBest(client)) {
            EndCrystalEntity crystal = findCrystalToBreak(client);
            if (crystal != null && actionReady(1, actionJitter.get())) {
                Vec3d targetPos = new Vec3d(crystal.getX(), crystal.getY() + 0.5, crystal.getZ());
                faceSmooth(client, targetPos);
                CombatUtil.attack(client, crystal);
            }
        }

        // Place
        if (place.enabled() && actionReady(2, actionJitter.get())) {
            tryPlaceBest(client, target);
        }

        // Melee fallback
        if (hitTarget.enabled() && client.player.distanceTo(target) <= hitRange.get() && actionReady(1, actionJitter.get())) {
            doMelee(client, target);
        }
    }

    private EndCrystalEntity findCrystalToBreak(MinecraftClient client) {
        return CombatUtil.bestCrystal(client, breakRange.get(), throughWalls.enabled()).orElse(null);
    }

    private boolean tryBreakBest(MinecraftClient client) {
        EndCrystalEntity crystal = findCrystalToBreak(client);
        if (crystal == null) return false;
        Vec3d targetPos = new Vec3d(crystal.getX(), crystal.getY() + 0.5, crystal.getZ());
        faceSmooth(client, targetPos);
        CombatUtil.attack(client, crystal);
        return true;
    }

    private boolean tryPlaceObsidianFeet(MinecraftClient client, LivingEntity target) {
        int obsidianSlot = CombatUtil.findItem(client.player, Items.OBSIDIAN);
        if (obsidianSlot == -1) return false;

        BlockPos feetPos = target.getBlockPos();
        if (!client.world.getBlockState(feetPos).isAir()) return false;
        if (client.world.getBlockState(feetPos.down()).isAir()) return false;
        if (client.player.squaredDistanceTo(Vec3d.ofCenter(feetPos)) > placeRange.get() * placeRange.get()) return false;

        faceSmooth(client, Vec3d.ofCenter(feetPos));
        if (actionReady(2, actionJitter.get())) {
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
        faceSmooth(client, Vec3d.ofCenter(pos));
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
        faceSmooth(client, Vec3d.ofCenter(pos.up()));
        if (actionReady(1, actionJitter.get())) {
            CombatUtil.selectHotbarSlot(client.player, slot);
            CombatUtil.interactBlock(client, pos, Direction.UP);
        }
        return true;
    }

    private void faceSmooth(MinecraftClient client, Vec3d target) {
        boolean isGhost = bypass.is("Ghost") || bypass.is("Legit") || bypass.is("Grim");
        int ticks = isGhost ? Math.max(2, faceDelay.get()) : Math.max(1, 1);
        faceRotations(client, target, ticks);
    }

    private void faceCrystal(MinecraftClient client, EndCrystalEntity crystal) {
        faceSmooth(client, new Vec3d(crystal.getX(), crystal.getY() + 0.5, crystal.getZ()));
    }

    private void facePos(MinecraftClient client, Vec3d pos) {
        faceSmooth(client, pos);
    }

    private void faceRotations(MinecraftClient client, Vec3d target, int ticks) {
        float[] rots = CombatUtil.rotationsTo(client.player, target);
        rotations().setMaxStep(rotationSpeed.get().floatValue());

        if (bypass.is("Ghost") || bypass.is("Legit") || bypass.is("Grim")) {
            float noiseYaw = (random.nextFloat() - 0.5f) * 1.5f;
            float noisePitch = (random.nextFloat() - 0.5f) * 0.8f;
            rotations().rotateToSilent(rots[0] + noiseYaw, rots[1] + noisePitch, ticks);
            rotations().setServerRotation(rots[0], rots[1], ticks);
        } else {
            rotations().rotateToSilent(rots[0], rots[1], ticks);
        }
    }

    private boolean tryPlaceObsidian(MinecraftClient client, LivingEntity target, int slot) {
        BlockPos bestObsidian = findBestObsidianPlacementBlock(client, target);
        if (bestObsidian == null) return false;
        facePos(client, Vec3d.ofCenter(bestObsidian));
        CombatUtil.selectHotbarSlot(client.player, slot);
        CombatUtil.interactBlock(client, bestObsidian, Direction.UP);
        return true;
    }

    private void doMelee(MinecraftClient client, LivingEntity target) {
        if (autoSwitchSword.enabled()) {
            int swordSlot = findBestSword(client);
            if (swordSlot != -1 && swordSlot < 9) {
                CombatUtil.selectHotbarSlot(client.player, swordSlot);
            }
        }
        faceSmooth(client, target.getEyePos());
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

    private BlockPos findBestObsidianPlacementBlock(MinecraftClient client, LivingEntity target) {
        Optional<BlockPos> opt = findBestObsidianPlacement(client, target);
        return opt.orElse(null);
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
