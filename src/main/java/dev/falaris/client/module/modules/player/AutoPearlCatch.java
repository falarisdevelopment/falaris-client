package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;

public final class AutoPearlCatch extends PlayerModule {
    private final BooleanSetting pearlDive = setting(new BooleanSetting("Pearl Dive", "Pearl then wind charge for speed.", true));
    private final IntegerSetting windChargeDelay = setting(new IntegerSetting("Wind Charge Delay", "Ticks between pearl and wind charge.", 6, 2, 15));
    private final BooleanSetting lookUp = setting(new BooleanSetting("Look Up", "Look straight up before throwing.", true));
    private final BooleanSetting lookBackDown = setting(new BooleanSetting("Look Back Down", "Return pitch after pearl throw.", true));
    private final IntegerSetting cooldown = setting(new IntegerSetting("Cooldown", "Ticks between combos.", 25, 0, 100));
    private final BooleanSetting autoActivate = setting(new BooleanSetting("Auto Activate", "Auto when holding pearl + looking up.", false));
    private final BooleanSetting elytraSafe = setting(new BooleanSetting("Elytra Safe", "Don't activate while using elytra.", true));
    private final BooleanSetting chasePlayers = setting(new BooleanSetting("Chase Players", "Auto-target players with pearls.", false));
    private final BooleanSetting autoSlamCompat = setting(new BooleanSetting("AutoSlam Compat", "Don't interfere with AutoSlam.", true));
    private final IntegerSetting chaseRange = setting(new IntegerSetting("Chase Range", "Distance to target players.", 20, 5, 50));

    private enum Phase { IDLE, LOOK_UP, PEARL, WAIT, CHARGE, LOOK_DOWN }
    private Phase phase = Phase.IDLE;
    private int tickCounter;
    private int cooldownCounter;
    private float prevPitch;

    public AutoPearlCatch() {
        super("AutoPearlCatch", "Pearl + wind charge combo for fast travel, with player chase.");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        phase = Phase.IDLE;
        tickCounter = 0;
        cooldownCounter = 0;
        prevPitch = 0;
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null) return;

        if (cooldownCounter < cooldown.get()) {
            cooldownCounter++;
            return;
        }

        if (elytraSafe.enabled() && client.player.isGliding()) return;

        switch (phase) {
            case IDLE -> {
                boolean shouldActivate = false;

                if (chasePlayers.enabled()) {
                    shouldActivate = findTargetPlayer(client) != null;
                }

                if (!shouldActivate && autoActivate.enabled()) {
                    boolean holdingPearl = client.player.getMainHandStack().isOf(Items.ENDER_PEARL)
                        || client.player.getOffHandStack().isOf(Items.ENDER_PEARL);
                    boolean lookingUp = client.player.getPitch() < -75.0f;
                    shouldActivate = holdingPearl && lookingUp;
                }

                if (!shouldActivate) {
                    shouldActivate = client.options.useKey.isPressed();
                }

                if (pearlDive.enabled() && shouldActivate) {
                    int pearlSlot = findItem(client.player, Items.ENDER_PEARL);
                    if (pearlSlot >= 0) {
                        prevPitch = client.player.getPitch();
                        if (lookUp.enabled()) {
                            client.player.setPitch(-90.0f);
                            phase = Phase.LOOK_UP;
                            tickCounter = 0;
                        } else {
                            doPearlThrow(client, pearlSlot);
                            phase = Phase.WAIT;
                            tickCounter = 0;
                        }
                    }
                }
            }
            case LOOK_UP -> {
                tickCounter++;
                if (tickCounter >= 2) {
                    int pearlSlot = findItem(client.player, Items.ENDER_PEARL);
                    if (pearlSlot >= 0) {
                        doPearlThrow(client, pearlSlot);
                    }
                    phase = Phase.WAIT;
                    tickCounter = 0;
                }
            }
            case WAIT -> {
                tickCounter++;
                if (tickCounter >= windChargeDelay.get()) {
                    int windSlot = findItem(client.player, Items.WIND_CHARGE);
                    if (windSlot >= 0 && windSlot < 9) {
                        client.player.getInventory().setSelectedSlot(windSlot);
                        client.interactionManager.interactItem(client.player, net.minecraft.util.Hand.MAIN_HAND);
                    }
                    if (lookBackDown.enabled()) {
                        phase = Phase.LOOK_DOWN;
                        tickCounter = 0;
                    } else {
                        phase = Phase.IDLE;
                        cooldownCounter = 0;
                    }
                }
            }
            case LOOK_DOWN -> {
                tickCounter++;
                float target = Math.max(prevPitch, 30.0f);
                float step = (target - client.player.getPitch()) / Math.max(1, 5 - tickCounter);
                client.player.setPitch(client.player.getPitch() + step);
                if (tickCounter >= 5 || Math.abs(client.player.getPitch() - target) < 5.0f) {
                    client.player.setPitch(target);
                    phase = Phase.IDLE;
                    cooldownCounter = 0;
                }
            }
        }
    }

    private void doPearlThrow(MinecraftClient client, int slot) {
        if (slot < 9) {
            client.player.getInventory().setSelectedSlot(slot);
        }
        client.interactionManager.interactItem(client.player, net.minecraft.util.Hand.MAIN_HAND);
    }

    private net.minecraft.entity.player.PlayerEntity findTargetPlayer(MinecraftClient client) {
        if (client.world == null || client.player == null) return null;
        double best = chaseRange.get() * chaseRange.get();
        net.minecraft.entity.player.PlayerEntity bestTarget = null;
        for (var player : client.world.getPlayers()) {
            if (player == client.player || player.isDead()) continue;
            double dist = client.player.squaredDistanceTo(player);
            if (dist < best) {
                best = dist;
                bestTarget = player;
            }
        }
        return bestTarget;
    }

    private int findItem(ClientPlayerEntity player, net.minecraft.item.Item item) {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getStack(slot).isOf(item)) return slot;
        }
        return -1;
    }
}
