package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;

public final class ElytraSwapper extends MiscModule {
    private final BooleanSetting autoEquipElytra = setting(new BooleanSetting("Auto Equip Elytra", "Swap chestplate to elytra when airborne.", true));
    private final BooleanSetting autoSwapBack = setting(new BooleanSetting("Auto Swap Back", "Swap elytra back to chestplate on ground.", true));
    private final DoubleSetting minFallDistance = setting(new DoubleSetting("Min Fall Distance", "Min fall distance before swap.", 3.0, 0.0, 10.0));
    private final IntegerSetting swapCooldown = setting(new IntegerSetting("Swap Cooldown", "Ticks between swaps.", 10, 1, 40));
    private final BooleanSetting maceMode = setting(new BooleanSetting("Mace Mode", "Swap to mace after elytra equip.", false));
    private final BooleanSetting dropChest = setting(new BooleanSetting("Drop Chest", "Drop chestplate when swapping.", false));
    private final BooleanSetting autoGlide = setting(new BooleanSetting("Auto Glide", "Auto-start gliding when falling.", true));
    private final BooleanSetting targetSwap = setting(new BooleanSetting("Target Swap", "Only swap when target is nearby.", false));
    private final DoubleSetting targetRange = setting(new DoubleSetting("Target Range", "Range for target detection.", 15.0, 5.0, 50.0));

    private int tickCounter;
    private boolean hasElytra;
    private int chestplateSlot = -1;

    public ElytraSwapper() {
        super("ElytraSwapper", "Auto-swaps between elytra and chestplate for elytra combat.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        tickCounter = 0;
        hasElytra = false;
        chestplateSlot = -1;
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.player == null) return;
        tickCounter++;
        if (tickCounter < swapCooldown.get()) return;

        boolean hasChestplate = client.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);

        boolean shouldEquip = autoEquipElytra.enabled() && shouldUseElytra(client);
        boolean shouldSwapBack = autoSwapBack.enabled() && !shouldUseElytra(client) && hasElytra;

        if (targetSwap.enabled()) {
            boolean targetNear = client.world != null && client.world.getPlayers().stream()
                .anyMatch(p -> p != client.player && !p.isDead() && client.player.distanceTo(p) < targetRange.get());
            if (!targetNear) {
                shouldEquip = false;
                shouldSwapBack = true;
            }
        }

        if (shouldEquip && !hasChestplate && chestplateSlot == -1) {
            for (int slot = 9; slot < 36; slot++) {
                if (client.player.getInventory().getStack(slot).isOf(Items.DIAMOND_CHESTPLATE) ||
                    client.player.getInventory().getStack(slot).isOf(Items.NETHERITE_CHESTPLATE) ||
                    client.player.getInventory().getStack(slot).isOf(Items.CHAINMAIL_CHESTPLATE) ||
                    client.player.getInventory().getStack(slot).isOf(Items.IRON_CHESTPLATE) ||
                    client.player.getInventory().getStack(slot).isOf(Items.GOLDEN_CHESTPLATE) ||
                    client.player.getInventory().getStack(slot).isOf(Items.LEATHER_CHESTPLATE)) {
                    chestplateSlot = slot;
                    break;
                }
            }
            if (chestplateSlot == -1) chestplateSlot = -2;
        }

        if (shouldEquip && !hasChestplate) {
            int elytraSlot = findElytraSlot(client);
            if (elytraSlot >= 0) {
                equipFromSlot(client, elytraSlot);
                hasElytra = true;
                tickCounter = 0;
            }
        }

        if (shouldSwapBack && hasChestplate) {
            if (chestplateSlot >= 0 && chestplateSlot < 36) {
                equipFromSlot(client, chestplateSlot);
                hasElytra = false;
                tickCounter = 0;
            } else {
                int chestSlot = findChestSlot(client);
                if (chestSlot >= 0) {
                    equipFromSlot(client, chestSlot);
                    hasElytra = false;
                    tickCounter = 0;
                }
            }
        }

        if (autoGlide.enabled() && hasElytra && !client.player.isOnGround() && !client.player.isGliding()) {
            client.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(
                client.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING
            ));
        }

        if (maceMode.enabled() && hasElytra && client.player.isGliding() && client.player.fallDistance > minFallDistance.get()) {
            int maceSlot = dev.falaris.client.module.modules.combat.CombatUtil.findItem(client.player, Items.MACE);
            if (maceSlot >= 0 && maceSlot < 9) {
                client.player.getInventory().setSelectedSlot(maceSlot);
            }
        }
    }

    private boolean shouldUseElytra(MinecraftClient client) {
        return !client.player.isOnGround() && client.player.fallDistance > minFallDistance.get();
    }

    private int findElytraSlot(MinecraftClient client) {
        for (int slot = 0; slot < 36; slot++) {
            if (client.player.getInventory().getStack(slot).isOf(Items.ELYTRA)) return slot;
        }
        return -1;
    }

    private int findChestSlot(MinecraftClient client) {
        for (int slot = 9; slot < 36; slot++) {
            var stack = client.player.getInventory().getStack(slot);
            if (stack.isOf(Items.DIAMOND_CHESTPLATE) || stack.isOf(Items.NETHERITE_CHESTPLATE) ||
                stack.isOf(Items.CHAINMAIL_CHESTPLATE) || stack.isOf(Items.IRON_CHESTPLATE) ||
                stack.isOf(Items.GOLDEN_CHESTPLATE) || stack.isOf(Items.LEATHER_CHESTPLATE)) return slot;
        }
        return -1;
    }

    private void equipFromSlot(MinecraftClient client, int slot) {
        if (slot >= 0 && slot < 9) {
            client.player.getInventory().setSelectedSlot(slot);
            client.interactionManager.interactItem(client.player, net.minecraft.util.Hand.MAIN_HAND);
        } else if (slot >= 9 && slot < 36) {
            int prev = client.player.getInventory().getSelectedSlot();
            client.player.getInventory().setSelectedSlot(slot);
            client.interactionManager.interactItem(client.player, net.minecraft.util.Hand.MAIN_HAND);
            client.player.getInventory().setSelectedSlot(prev);
        }
    }
}
