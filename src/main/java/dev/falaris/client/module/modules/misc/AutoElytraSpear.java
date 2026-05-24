package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.Random;

public final class AutoElytraSpear extends MiscModule {
    private final DoubleSetting targetRange = setting(new DoubleSetting("Target Range", "Max target distance.", 40.0, 5.0, 80.0));
    private final DoubleSetting attackRange = setting(new DoubleSetting("Attack Range", "Range to strike.", 4.0, 2.0, 8.0));
    private final DoubleSetting diveRange = setting(new DoubleSetting("Dive Range", "Range to start dive.", 15.0, 5.0, 40.0));
    private final IntegerSetting fireworkDelay = setting(new IntegerSetting("Firework Delay", "Ticks between boosts.", 20, 5, 60));
    private final BooleanSetting autoEquip = setting(new BooleanSetting("Auto Equip", "Equip elytra from hotbar.", true));
    private final BooleanSetting autoStart = setting(new BooleanSetting("Auto Start", "Auto-start gliding when falling.", true));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Target players.", true));
    private final ModeSetting weapon = setting(new ModeSetting("Weapon", "Spear weapon.", "Trident", "Trident", "Mace", "Sword"));
    private final BooleanSetting fireworkBoost = setting(new BooleanSetting("Firework Boost", "Use rockets to dive.", true));
    private final IntegerSetting strafeInterval = setting(new IntegerSetting("Strafe Interval", "Ticks between strafe adjustments.", 10, 5, 30));

    private final Random random = new Random();
    private LivingEntity target;
    private int fireworkTick;
    private int strafeTick;

    public AutoElytraSpear() {
        super("AutoElytraSpear", "Elytra combat with trident/spear dive attacks. Vape-style.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        target = null;
        fireworkTick = 0;
        strafeTick = 0;
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        fireworkTick++;
        strafeTick++;

        if (target == null || !target.isAlive() || client.player.squaredDistanceTo(target) > targetRange.get() * targetRange.get()) {
            target = findTarget(client);
        }
        if (target == null) return;

        if (autoEquip.enabled()) {
            int elytraSlot = findItem(client, Items.ELYTRA);
            if (elytraSlot != -1 && !client.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
                selectSlot(client, elytraSlot);
            }
        }

        if (autoStart.enabled() && !client.player.isOnGround() && !client.player.isGliding() && client.player.getVelocity().y < -0.3) {
            client.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(
                client.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING
            ));
        }

        if (!client.player.isGliding()) return;

        double dist = client.player.distanceTo(target);

        if (fireworkBoost.enabled() && dist <= diveRange.get() && fireworkTick >= fireworkDelay.get()) {
            int fw = findItem(client, Items.FIREWORK_ROCKET);
            if (fw >= 0 && fw < 9) {
                selectSlot(client, fw);
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                fireworkTick = 0;
            }
        }

        if (dist <= attackRange.get()) {
            int spearSlot = findBestSpear(client);
            if (spearSlot >= 0 && spearSlot < 9) {
                client.player.getInventory().setSelectedSlot(spearSlot);
            }
            if (ready(2, 1)) {
                Vec3d look = target.getEyePos();
                float[] rots = dev.falaris.client.module.modules.combat.CombatUtil.rotationsTo(client.player, look);
                client.player.setYaw(rots[0]);
                client.player.setPitch(rots[1]);
                client.interactionManager.attackEntity(client.player, target);
                client.player.swingHand(Hand.MAIN_HAND);
            }
        }

        if (strafeTick >= strafeInterval.get()) {
            strafeTick = 0;
            double dx = target.getX() - client.player.getX();
            double dz = target.getZ() - client.player.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 0.1) {
                Vec3d vel = client.player.getVelocity();
                Vec3d toward = new Vec3d(dx / len * 0.3, -0.3, dz / len * 0.3);
                client.player.setVelocity(vel.add(toward));
            }
        }
    }

    private LivingEntity findTarget(MinecraftClient client) {
        return client.world.getEntitiesByClass(LivingEntity.class, client.player.getBoundingBox().expand(targetRange.get()), entity -> {
            return entity != client.player && entity.isAlive()
                && (!(entity instanceof PlayerEntity) || players.enabled());
        }).stream().min(Comparator.comparingDouble(client.player::distanceTo)).orElse(null);
    }

    private int findBestSpear(MinecraftClient client) {
        net.minecraft.item.Item targetItem = switch (weapon.get()) {
            case "Trident" -> Items.TRIDENT;
            case "Mace" -> Items.MACE;
            default -> Items.DIAMOND_SWORD;
        };
        return findItem(client, targetItem);
    }

    private int findItem(MinecraftClient client, net.minecraft.item.Item item) {
        for (int slot = 0; slot < 9; slot++) {
            if (client.player.getInventory().getStack(slot).isOf(item)) return slot;
        }
        for (int slot = 9; slot < 36; slot++) {
            if (client.player.getInventory().getStack(slot).isOf(item)) return slot;
        }
        return -1;
    }

    private void selectSlot(MinecraftClient client, int slot) {
        if (slot >= 0 && slot < 9) {
            client.player.getInventory().setSelectedSlot(slot);
        }
    }
}
