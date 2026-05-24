package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public final class AutoDisconnect extends MiscModule {
    private final ModeSetting trigger = setting(new ModeSetting("Trigger", "What triggers disconnect.", "Health", "Health", "Crystal", "Totem Pop", "Health+Crystal"));
    private final DoubleSetting healthThreshold = setting(new DoubleSetting("Health Threshold", "Disconnect when health drops below.", 6.0, 0.5, 20.0));
    private final DoubleSetting crystalRange = setting(new DoubleSetting("Crystal Range", "Disconnect when crystal within range.", 6.0, 1.0, 12.0));
    private final BooleanSetting onlyTarget = setting(new BooleanSetting("Only Target", "Only disconnect if a player is nearby.", true));
    private final BooleanSetting toggleOff = setting(new BooleanSetting("Toggle Off", "Disable module after disconnect.", true));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks to wait before disconnecting.", 0, 0, 10));

    private int delayTicks;
    private boolean shouldDisconnect;

    public AutoDisconnect() {
        super("AutoDisconnect", "Automatically disconnects on low health, crystal nearby, or totem pop.");
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        boolean triggered = false;
        String t = trigger.get();

        if (t.contains("Health")) {
            float health = client.player.getHealth() + client.player.getAbsorptionAmount();
            if (health <= healthThreshold.get()) {
                triggered = true;
            }
        }

        if (t.contains("Crystal")) {
            for (Entity entity : client.world.getEntities()) {
                if (entity instanceof EndCrystalEntity && entity.distanceTo(client.player) <= crystalRange.get()) {
                    if (!onlyTarget.enabled() || hasTargetNearby(client)) {
                        triggered = true;
                        break;
                    }
                }
            }
        }

        if (triggered) {
            delayTicks++;
            if (delayTicks >= delay.get() + 1) {
                client.player.networkHandler.getConnection().disconnect(Text.literal("Disconnected by AutoDisconnect"));
                if (toggleOff.enabled()) setEnabled(false);
            }
        } else {
            delayTicks = 0;
        }
    }

    private boolean hasTargetNearby(MinecraftClient client) {
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player != client.player && player.distanceTo(client.player) <= 12.0) return true;
        }
        return false;
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        delayTicks = 0;
    }
}
