package dev.falaris.client.module.modules.player;

import dev.falaris.client.module.modules.combat.CombatUtil;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public final class AutoPearl extends PlayerModule {
    private final DoubleSetting escapeHealth = setting(new DoubleSetting("Escape Health", "Health below which to pearl away.", 6.0, 1.0, 20.0));
    private final DoubleSetting chaseHealth = setting(new DoubleSetting("Chase Health", "Target health below which to pearl towards.", 4.0, 1.0, 20.0));
    private final DoubleSetting chaseRange = setting(new DoubleSetting("Chase Range", "Max range to chase target.", 32.0, 8.0, 64.0));
    private final IntegerSetting cooldown = setting(new IntegerSetting("Cooldown", "Ticks between pearl throws.", 40, 10, 100));

    private int tickCounter;

    public AutoPearl() {
        super("AutoPearl", "Auto-pearl away when low health, chase when target is weak.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        tickCounter = 0;
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null) return;
        tickCounter++;
        if (tickCounter < cooldown.get()) return;

        // Escape: pearl away when health is low
        if (client.player.getHealth() <= escapeHealth.get()) {
            int pearlSlot = findPearl(client.player);
            if (pearlSlot >= 0) {
                Vec3d lookDir = client.player.getRotationVec(1.0f);
                client.player.setYaw(client.player.getYaw() + 180.0f);
                client.player.setPitch(30.0f);
                client.player.getInventory().setSelectedSlot(pearlSlot);
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                tickCounter = 0;
                return;
            }
        }

        // Chase: pearl towards a weak target
        for (LivingEntity target : client.world.getEntitiesByClass(LivingEntity.class,
                client.player.getBoundingBox().expand(chaseRange.get()),
                e -> e instanceof PlayerEntity && e != client.player && e.isAlive()
                        && e.getHealth() <= chaseHealth.get()
                        && client.player.squaredDistanceTo(e) <= chaseRange.get() * chaseRange.get())) {
            int pearlSlot = findPearl(client.player);
            if (pearlSlot >= 0) {
                float[] rots = CombatUtil.rotationsTo(client.player, new Vec3d(target.getX(), target.getY(), target.getZ()));
                client.player.setYaw(rots[0]);
                client.player.setPitch((float) Math.max(rots[1], 20.0));
                client.player.getInventory().setSelectedSlot(pearlSlot);
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                tickCounter = 0;
                return;
            }
        }
    }

    private int findPearl(ClientPlayerEntity player) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getStack(slot).isOf(Items.ENDER_PEARL)) {
                return slot;
            }
        }
        return -1;
    }
}
