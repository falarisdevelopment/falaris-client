package dev.falaris.client.module.modules.misc;

import dev.falaris.client.module.modules.combat.CombatUtil;
import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;

public final class Hunter extends MiscModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Target search range.", 32.0, 4.0, 64.0));
    private final DoubleSetting attackRange = setting(new DoubleSetting("Attack Range", "Attack range.", 3.5, 1.0, 6.0));
    private final DoubleSetting speed = setting(new DoubleSetting("Speed", "Flight/movement speed.", 1.0, 0.1, 5.0));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks between attacks.", 10, 1, 40));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Target players.", true));
    private final BooleanSetting hostiles = setting(new BooleanSetting("Hostiles", "Target hostile mobs.", true));
    private final BooleanSetting passives = setting(new BooleanSetting("Passives", "Target passive mobs.", false));
    private final BooleanSetting fly = setting(new BooleanSetting("Fly", "Enable flight when hunting.", true));
    private final BooleanSetting elytraAware = setting(new BooleanSetting("Elytra Aware", "Don't force flight if using elytra.", true));
    private final BooleanSetting autoWeapon = setting(new BooleanSetting("Auto Weapon", "Switch to best weapon when attacking.", true));
    private final BooleanSetting respectCooldown = setting(new BooleanSetting("Respect Cooldown", "Only attack when weapon cooldown is ready.", true));

    private LivingEntity target;
    private int attackCounter;

    public Hunter() {
        super("Hunter", "Auto-targets and pursues nearby entities with flight/elytra.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        target = null;
        attackCounter = 0;
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        attackCounter++;

        boolean hasElytra = client.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);

        if (fly.enabled() && !(elytraAware.enabled() && hasElytra)) {
            client.player.getAbilities().flying = true;
            client.player.getAbilities().setFlySpeed((float) (speed.get() / 5.0));
        }

        if (target == null || !target.isAlive() || client.player.squaredDistanceTo(target) > range.get() * range.get()) {
            target = findTarget(client);
        }

        if (target == null) return;

        Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
        Vec3d playerPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
        Vec3d toTarget = targetPos.subtract(playerPos).normalize();

        if (fly.enabled() && !(elytraAware.enabled() && hasElytra)) {
            client.player.setVelocity(toTarget.x * speed.get(), toTarget.y * speed.get(), toTarget.z * speed.get());
        } else {
            double y = hasElytra ? client.player.getVelocity().y : 0;
            client.player.setVelocity(toTarget.x * speed.get(), y, toTarget.z * speed.get());
        }

        if (client.player.distanceTo(target) <= attackRange.get() && attackCounter >= delay.get()) {
            if (respectCooldown.enabled() && client.player.getAttackCooldownProgress(0) < 0.9f) {
                return;
            }
            if (autoWeapon.enabled()) {
                int swordSlot = CombatUtil.findItem(client.player, Items.NETHERITE_SWORD);
                if (swordSlot == -1) swordSlot = CombatUtil.findItem(client.player, Items.DIAMOND_SWORD);
                if (swordSlot == -1) swordSlot = CombatUtil.findItem(client.player, Items.IRON_SWORD);
                if (swordSlot == -1) swordSlot = CombatUtil.findItem(client.player, Items.STONE_SWORD);
                if (swordSlot >= 0 && swordSlot < 9) {
                    CombatUtil.selectHotbarSlot(client.player, swordSlot);
                }
            }
            client.interactionManager.attackEntity(client.player, target);
            client.player.swingHand(Hand.MAIN_HAND);
            attackCounter = 0;
        }

        client.player.fallDistance = 0.0f;
    }

    @Override
    protected void onDisable() {
        target = null;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && fly.enabled()) {
            client.player.getAbilities().flying = false;
        }
    }

    private LivingEntity findTarget(MinecraftClient client) {
        return client.world.getEntitiesByClass(LivingEntity.class, client.player.getBoundingBox().expand(range.get()), entity -> {
            if (entity == client.player || !entity.isAlive()) return false;
            if (entity instanceof PlayerEntity && !players.enabled()) return false;
            if (entity instanceof HostileEntity && !hostiles.enabled()) return false;
            return true;
        }).stream().min(Comparator.comparingDouble(client.player::distanceTo)).orElse(null);
    }
}
