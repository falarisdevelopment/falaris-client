package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.Optional;
import java.util.Random;

public final class AutoElytraMace extends CombatModule {
    private final ModeSetting targetMode = setting(new ModeSetting("Target Mode", "Who to target.", "Players", "Players", "Hostiles", "All"));
    private final DoubleSetting targetRange = setting(new DoubleSetting("Target Range", "Max target distance.", 40.0, 8.0, 80.0));
    private final DoubleSetting attackRange = setting(new DoubleSetting("Attack Range", "Range to perform mace hit.", 4.5, 2.0, 6.0));
    private final DoubleSetting minFallDistance = setting(new DoubleSetting("Min Fall Distance", "Min fall for mace damage.", 4.0, 2.0, 30.0));
    private final DoubleSetting idealFallDistance = setting(new DoubleSetting("Ideal Fall", "Ideal fall for max damage.", 12.0, 4.0, 50.0));
    private final DoubleSetting diveHeight = setting(new DoubleSetting("Dive Height", "Height above target to start dive.", 15.0, 5.0, 50.0));

    private final BooleanSetting autoEquip = setting(new BooleanSetting("Auto Equip", "Equip elytra and mace.", true));
    private final BooleanSetting autoStartFlying = setting(new BooleanSetting("Auto Start Flying", "Auto-start elytra.", true));
    private final ModeSetting boostMode = setting(new ModeSetting("Boost Mode", "Dive propellant.", "Firework", "Firework", "Wind Charge", "Pearl", "Both"));
    private final IntegerSetting boostDelay = setting(new IntegerSetting("Boost Delay", "Ticks between boosts.", 10, 2, 30));
    private final BooleanSetting boostInDive = setting(new BooleanSetting("Boost in Dive", "Use boosts during dive.", true));

    private final BooleanSetting silentRotate = setting(new BooleanSetting("Silent Rotate", "Face target silently.", true));
    private final DoubleSetting rotationSpeed = setting(new DoubleSetting("Rotation Speed", "Rotation speed.", 25.0, 5.0, 60.0));
    private final ModeSetting bypass = setting(new ModeSetting("Bypass", "Anti-cheat.", "Vanilla", "Vanilla", "Vanilla", "Ghost", "Grim"));

    private final BooleanSetting autoSwitchMace = setting(new BooleanSetting("Auto Switch Mace", "Switch to mace before hitting.", true));
    private final BooleanSetting autoSwitchSword = setting(new BooleanSetting("Auto Switch Sword", "Switch back to sword after hit.", true));
    private final IntegerSetting postHitDelay = setting(new IntegerSetting("Post-Hit Delay", "Ticks after hit before next action.", 10, 1, 30));

    private final Random random = new Random();
    private LivingEntity target;
    private int boostTick;
    private int postHitWait;
    private int prevSlot;
    private boolean hitThisDive;
    private DivePhase divePhase;

    private enum DivePhase { CLIMB, DIVE, STRIKE, RECOVER }

    public AutoElytraMace() {
        super("AutoElytraMace", "Advanced elytra+mace dive bomb with boost management and smart timing.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        target = null;
        boostTick = 0;
        postHitWait = 0;
        prevSlot = -1;
        hitThisDive = false;
        divePhase = DivePhase.CLIMB;
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (postHitWait > 0) { postHitWait--; return; }

        // Find/refresh target
        if (target == null || !target.isAlive() || client.player.squaredDistanceTo(target) > targetRange.get() * targetRange.get()) {
            target = findTarget(client);
        }
        if (target == null) return;

        boolean isGhost = bypass.is("Ghost") || bypass.is("Grim");
        if (isGhost && random.nextFloat() < 0.06f) return;

        boostTick++;

        // Auto equip elytra
        if (autoEquip.enabled()) {
            ensureElytraEquipped(client);
        }

        // Auto start flying
        if (autoStartFlying.enabled() && shouldStartGliding(client)) {
            startGliding(client);
        }

        // Determine dive phase based on state
        updateDivePhase(client);

        // Execute phase actions
        switch (divePhase) {
            case CLIMB -> doClimb(client);
            case DIVE -> doDive(client);
            case STRIKE -> doStrike(client);
            case RECOVER -> doRecover(client);
        }
    }

    private void updateDivePhase(MinecraftClient client) {
        if (!client.player.isGliding()) {
            divePhase = DivePhase.CLIMB;
            hitThisDive = false;
            return;
        }

        double heightAbove = client.player.getY() - target.getY();
        double dist = client.player.distanceTo(target);

        if (divePhase == DivePhase.STRIKE) return; // Don't override while striking

        if (divePhase == DivePhase.RECOVER && client.player.isOnGround()) {
            divePhase = DivePhase.CLIMB;
            hitThisDive = false;
            return;
        }

        if (heightAbove >= diveHeight.get() && dist > attackRange.get() * 2) {
            divePhase = DivePhase.DIVE;
        } else if (heightAbove < diveHeight.get() && dist > attackRange.get() && client.player.getVelocity().y < -0.5) {
            divePhase = DivePhase.DIVE;
        } else if (heightAbove > 2.0 && dist <= attackRange.get() && client.player.fallDistance >= minFallDistance.get()) {
            divePhase = DivePhase.STRIKE;
        } else if (heightAbove <= 2.0 && dist > attackRange.get()) {
            divePhase = DivePhase.CLIMB;
        }
    }

    private void doClimb(MinecraftClient client) {
        // Face toward target while climbing
        faceTarget(client, target, 1.0f);

        // Boost upward
        if (boostTick >= boostDelay.get()) {
            useBoostItem(client);
            boostTick = 0;
        }

        // Start gliding if falling
        if (client.player.getVelocity().y < -0.3 && !client.player.isGliding()) {
            startGliding(client);
        }
    }

    private void doDive(MinecraftClient client) {
        double dist = client.player.distanceTo(target);
        double heightAbove = client.player.getY() - target.getY();

        // Face target while diving
        faceTarget(client, target, 1.0f);

        // Boost toward target
        if (boostInDive.enabled() && boostTick >= boostDelay.get() && heightAbove > 2.0) {
            useBoostItem(client);
            boostTick = 0;
        }

        // Check if we're close enough to strike
        if (dist <= attackRange.get() && client.player.fallDistance >= minFallDistance.get()) {
            divePhase = DivePhase.STRIKE;
        }

        // If we overshot, climb again
        if (heightAbove < -5.0) {
            divePhase = DivePhase.CLIMB;
        }
    }

    private void doStrike(MinecraftClient client) {
        double dist = client.player.distanceTo(target);

        // Switch to mace
        if (autoSwitchMace.enabled()) {
            int maceSlot = findItemInInventory(client.player, Items.MACE);
            if (maceSlot >= 0 && maceSlot < 9) {
                prevSlot = client.player.getInventory().getSelectedSlot();
                client.player.getInventory().setSelectedSlot(maceSlot);
            }
        }

        // Face target for attack
        faceTarget(client, target, 0.5f);

        // Attack when close enough with sufficient fall distance
        if (dist <= attackRange.get() && client.player.fallDistance >= minFallDistance.get()) {
            if (actionReady(isGhost(client) ? 1 + random.nextInt(2) : 1, 0)) {
                CombatUtil.attack(client, target);
                hitThisDive = true;

                // Switch back to sword
                if (autoSwitchSword.enabled() && prevSlot >= 0 && prevSlot < 9) {
                    int swordSlot = findBestSwordSlot(client);
                    if (swordSlot >= 0 && swordSlot < 9) {
                        client.player.getInventory().setSelectedSlot(swordSlot);
                    } else {
                        client.player.getInventory().setSelectedSlot(prevSlot);
                    }
                }

                postHitWait = postHitDelay.get();
                divePhase = DivePhase.RECOVER;
            }
        }

        // If we missed the window, climb again
        if (dist > attackRange.get() * 1.5) {
            divePhase = DivePhase.CLIMB;
        }
    }

    private void doRecover(MinecraftClient client) {
        // Let the player fall safely or boost up again
        if (client.player.isOnGround() || client.player.fallDistance < 1.0f) {
            divePhase = DivePhase.CLIMB;
            hitThisDive = false;
        }
    }

    private void faceTarget(MinecraftClient client, Entity target, float speedMul) {
        if (!silentRotate.enabled()) return;
        Vec3d targetPos = target.getBoundingBox().getCenter();
        float[] rots = CombatUtil.rotationsTo(client.player, targetPos);

        rotations().setMaxStep(rotationSpeed.get().floatValue() * speedMul);

        if (isGhost(client)) {
            float ny = (random.nextFloat() - 0.5f) * 2.0f;
            float np = (random.nextFloat() - 0.5f) * 1.0f;
            rotations().rotateToSilent(rots[0] + ny, rots[1] + np, Math.max(2, (int)(distTo(client.player, targetPos) * 0.5)));
            rotations().setServerRotation(rots[0], rots[1], Math.max(2, (int)(distTo(client.player, targetPos) * 0.3)));
        } else {
            rotations().rotateToSilent(rots[0], rots[1], Math.max(1, (int)(distTo(client.player, targetPos) * 0.3)));
        }
    }

    private void useBoostItem(MinecraftClient client) {
        boolean used = false;

        if (boostMode.is("Firework") || boostMode.is("Both")) {
            int slot = findItemInInventory(client.player, Items.FIREWORK_ROCKET);
            if (slot >= 0 && slot < 9) {
                client.player.getInventory().setSelectedSlot(slot);
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                used = true;
            }
        }
        if (!used && (boostMode.is("Wind Charge") || boostMode.is("Both"))) {
            int slot = findItemInInventory(client.player, Items.WIND_CHARGE);
            if (slot >= 0 && slot < 9) {
                client.player.getInventory().setSelectedSlot(slot);
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                used = true;
            }
        }
        if (!used && (boostMode.is("Pearl") || boostMode.is("Both"))) {
            int slot = findItemInInventory(client.player, Items.ENDER_PEARL);
            if (slot >= 0 && slot < 9) {
                client.player.getInventory().setSelectedSlot(slot);
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            }
        }
    }

    private void ensureElytraEquipped(MinecraftClient client) {
        ItemStack chest = client.player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.isOf(Items.ELYTRA)) return;

        int elytraSlot = findItemInInventory(client.player, Items.ELYTRA);
        if (elytraSlot >= 0 && elytraSlot < 9) {
            client.player.getInventory().setSelectedSlot(elytraSlot);
        }
    }

    private boolean shouldStartGliding(MinecraftClient client) {
        if (client.player.isGliding()) return false;
        if (client.player.isOnGround()) return false;
        if (client.player.getVelocity().y >= -0.2) return false;
        // Only start gliding when above target
        if (target != null && client.player.getY() < target.getY() + 5.0) return false;
        return true;
    }

    private void startGliding(MinecraftClient client) {
        client.player.networkHandler.sendPacket(new ClientCommandC2SPacket(
            client.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING
        ));
    }

    private LivingEntity findTarget(MinecraftClient client) {
        double range = targetRange.get();
        return client.world.getEntitiesByClass(LivingEntity.class, client.player.getBoundingBox().expand(range), entity -> {
            if (entity == client.player || !entity.isAlive()) return false;
            if (client.player.squaredDistanceTo(entity) > range * range) return false;
            // Only target entities below us (for dive bomb)
            if (entity.getY() > client.player.getY() + 2.0) return false;
            return switch (targetMode.get()) {
                case "Players" -> entity instanceof PlayerEntity;
                case "Hostiles" -> entity instanceof net.minecraft.entity.mob.HostileEntity;
                case "All" -> true;
                default -> false;
            };
        }).stream().min(Comparator.comparingDouble(client.player::distanceTo)).orElse(null);
    }

    private int findItemInInventory(ClientPlayerEntity player, net.minecraft.item.Item item) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getStack(slot).isOf(item)) return slot;
        }
        // Search rest of inventory
        for (int slot = 9; slot < 36; slot++) {
            if (player.getInventory().getStack(slot).isOf(item)) return slot;
        }
        return -1;
    }

    private int findBestSwordSlot(MinecraftClient client) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = client.player.getInventory().getStack(slot);
            if (stack.isOf(Items.WOODEN_SWORD) || stack.isOf(Items.STONE_SWORD)
                || stack.isOf(Items.IRON_SWORD) || stack.isOf(Items.GOLDEN_SWORD)
                || stack.isOf(Items.DIAMOND_SWORD) || stack.isOf(Items.NETHERITE_SWORD)) return slot;
        }
        return -1;
    }

    private boolean isGhost(MinecraftClient client) {
        return bypass.is("Ghost") || bypass.is("Grim");
    }

    private double distTo(ClientPlayerEntity player, Vec3d pos) {
        return player.squaredDistanceTo(pos);
    }

    @Override
    protected void onDisable() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && prevSlot >= 0 && prevSlot < 9) {
            client.player.getInventory().setSelectedSlot(prevSlot);
        }
        target = null;
        boostTick = 0;
        postHitWait = 0;
        prevSlot = -1;
        hitThisDive = false;
        divePhase = DivePhase.CLIMB;
    }
}
