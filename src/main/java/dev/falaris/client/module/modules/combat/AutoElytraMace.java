package dev.falaris.client.module.modules.combat;

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

public final class AutoElytraMace extends CombatModule {
    private final DoubleSetting targetRange = setting(new DoubleSetting("Target Range", "Max target distance.", 30.0, 4.0, 60.0));
    private final DoubleSetting minFallDistance = setting(new DoubleSetting("Min Fall Distance", "Min fall distance to attack.", 5.0, 2.0, 20.0));
    private final DoubleSetting attackRange = setting(new DoubleSetting("Attack Range", "Range to attack.", 4.0, 1.0, 6.0));
    private final BooleanSetting autoEquip = setting(new BooleanSetting("Auto Equip", "Equip elytra from hotbar.", true));
    private final BooleanSetting autoStartFlying = setting(new BooleanSetting("Auto Start Flying", "Auto-start elytra when falling.", true));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Target players.", true));
    private final BooleanSetting hostiles = setting(new BooleanSetting("Hostiles", "Target hostiles.", false));
    private final BooleanSetting diveBomb = setting(new BooleanSetting("Dive Bomb", "Rocket boost to dive.", true));
    private final ModeSetting diveMode = setting(new ModeSetting("Dive Mode", "Dive propellant.", "Firework", "Firework", "Wind Charge", "Both"));
    private final DoubleSetting diveRange = setting(new DoubleSetting("Dive Range", "Range to start dive.", 12.0, 4.0, 35.0));
    private final IntegerSetting fireworkDelay = setting(new IntegerSetting("Firework Delay", "Ticks between boosts.", 15, 5, 50));
    private final ModeSetting bypass = setting(new ModeSetting("Bypass", "Anti-cheat.", "Vanilla", "Vanilla", "Legit", "Grim"));

    private final Random random = new Random();
    private LivingEntity target;
    private int fireworkTick;

    public AutoElytraMace() {
        super("AutoElytraMace", "Auto elytra combat with mace slam and dive bomb.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        target = null;
        fireworkTick = 0;
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        if (target == null || !target.isAlive() || client.player.squaredDistanceTo(target) > targetRange.get() * targetRange.get()) {
            target = findTarget(client);
        }
        if (target == null) return;

        boolean isLegit = bypass.is("Legit") || bypass.is("Grim");
        if (isLegit && random.nextFloat() < 0.10f) return;

        fireworkTick++;

        if (autoEquip.enabled()) {
            int elytraSlot = CombatUtil.findItem(client.player, Items.ELYTRA);
            if (elytraSlot != -1 && !client.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
                CombatUtil.selectHotbarSlot(client.player, elytraSlot);
            }
        }

        if (autoStartFlying.enabled() && !client.player.isOnGround() && !client.player.isGliding() && client.player.getVelocity().y < -0.3) {
            client.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(
                client.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING
            ));
        }

        double dist = client.player.distanceTo(target);

        if (client.player.isGliding() && diveBomb.enabled() && dist <= diveRange.get() && fireworkTick >= fireworkDelay.get()) {
            boolean above = client.player.getY() > target.getY() + 2.0;
            if (above || dist > attackRange.get()) {
                boolean used = false;
                if (diveMode.is("Firework") || diveMode.is("Both")) {
                    int fw = CombatUtil.findItem(client.player, Items.FIREWORK_ROCKET);
                    if (fw >= 0 && fw < 9) {
                        CombatUtil.selectHotbarSlot(client.player, fw);
                        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                        used = true;
                    }
                }
                if (!used && (diveMode.is("Wind Charge") || diveMode.is("Both"))) {
                    int wc = CombatUtil.findItem(client.player, Items.WIND_CHARGE);
                    if (wc >= 0 && wc < 9) {
                        CombatUtil.selectHotbarSlot(client.player, wc);
                        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                    }
                }
                fireworkTick = 0;
            }
        }

        if (client.player.fallDistance >= minFallDistance.get() && dist <= attackRange.get()) {
            int maceSlot = CombatUtil.findItem(client.player, Items.MACE);
            if (maceSlot != -1 && maceSlot < 9) {
                CombatUtil.selectHotbarSlot(client.player, maceSlot);
            }
            if (actionReady(1, 0)) {
                CombatUtil.attack(client, target);
            }
        }
    }

    private LivingEntity findTarget(MinecraftClient client) {
        return client.world.getEntitiesByClass(LivingEntity.class, client.player.getBoundingBox().expand(targetRange.get()), entity -> {
            if (entity == client.player || !entity.isAlive()) return false;
            if (entity instanceof PlayerEntity) return players.enabled();
            return hostiles.enabled() && entity instanceof net.minecraft.entity.mob.HostileEntity;
        }).stream().min(Comparator.comparingDouble(client.player::distanceTo)).orElse(null);
    }
}
