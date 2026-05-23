package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;

public final class SilentAura extends CombatModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum attack distance.", 3.8, 1.0, 6.0));
    private final DoubleSetting fov = setting(new DoubleSetting("FOV", "Maximum target angle from crosshair.", 90.0, 5.0, 180.0));
    private final DoubleSetting cps = setting(new DoubleSetting("CPS", "Average attacks per second.", 8.0, 1.0, 20.0));
    private final IntegerSetting jitter = setting(new IntegerSetting("Delay Jitter", "Random extra ticks between attacks.", 3, 0, 10));
    private final ModeSetting priority = setting(new ModeSetting("Priority", "Target sorting mode.", "Angle", "Distance", "Health", "Angle"));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Target players.", true));
    private final BooleanSetting hostiles = setting(new BooleanSetting("Hostiles", "Target hostile mobs.", true));
    private final BooleanSetting passives = setting(new BooleanSetting("Passives", "Target passive mobs.", false));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Allow targets without line of sight.", false));

    public SilentAura() {
        super("SilentAura", "Attacks selected targets with minimal visible camera movement.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        LivingEntity target = CombatUtil.bestLivingTarget(
                client,
                range.get(),
                players.enabled(),
                hostiles.enabled(),
                passives.enabled(),
                throughWalls.enabled(),
                priority.get()
        ).orElse(null);

        if (target == null) {
            return;
        }
        if (CombatUtil.angleTo(client.player, target.getEyePos()) > fov.get()) {
            return;
        }

        int minimumTicks = Math.max(1, (int) Math.round(20.0 / cps.get()));
        if (actionReady(minimumTicks, jitter.get())) {
            CombatUtil.attack(client, target);
        }
    }
}
