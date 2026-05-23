package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.DoubleSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;

public final class AimAssist extends CombatModule {
    private final DoubleSetting range = setting(new DoubleSetting("Range", "Maximum assist distance.", 5.5, 1.0, 10.0));
    private final DoubleSetting fov = setting(new DoubleSetting("FOV", "Maximum angle from crosshair.", 55.0, 5.0, 180.0));
    private final DoubleSetting strength = setting(new DoubleSetting("Strength", "Assist rotation speed.", 6.0, 0.5, 30.0));
    private final ModeSetting priority = setting(new ModeSetting("Priority", "Target sorting mode.", "Angle", "Distance", "Health", "Angle"));
    private final BooleanSetting requireAttack = setting(new BooleanSetting("Require Attack", "Only assist while the attack key is held.", true));
    private final BooleanSetting players = setting(new BooleanSetting("Players", "Target players.", true));
    private final BooleanSetting hostiles = setting(new BooleanSetting("Hostiles", "Target hostile mobs.", true));
    private final BooleanSetting passives = setting(new BooleanSetting("Passives", "Target passive mobs.", false));
    private final BooleanSetting throughWalls = setting(new BooleanSetting("Through Walls", "Allow targets without line of sight.", false));

    public AimAssist() {
        super("AimAssist", "Gently helps move your view toward nearby targets.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }
        if (requireAttack.enabled() && !client.options.attackKey.isPressed()) {
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
        ).filter(entity -> CombatUtil.angleTo(client.player, entity.getEyePos()) <= fov.get()).orElse(null);

        if (target != null) {
            CombatUtil.face(rotations(), client.player, target.getEyePos(), strength.get());
        }
    }
}
