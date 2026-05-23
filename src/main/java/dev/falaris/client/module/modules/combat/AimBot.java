package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;

public final class AimBot extends CombatModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum lock distance.", 6.0, 1.0, 12.0));
    private final DoubleSetting rotationSpeed = setting(new DoubleSetting("Rotation Speed", "Maximum rotation step per tick.", 14.0, 1.0, 60.0));
    private final ModeSetting priority = setting(new ModeSetting("Priority", "Target sorting mode.", "Angle", "Distance", "Health", "Angle"));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Target players.", true));
    private final BooleanSetting hostiles = setting(new BooleanSetting("Hostiles", "Target hostile mobs.", true));
    private final BooleanSetting passives = setting(new BooleanSetting("Passives", "Target passive mobs.", false));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Allow targets without line of sight.", false));

    public AimBot() {
        super("AimBot", "Smoothly locks view onto the best target.");
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

        if (target != null) {
            CombatUtil.face(rotations(), client.player, target.getEyePos(), rotationSpeed.get());
        }
    }
}
