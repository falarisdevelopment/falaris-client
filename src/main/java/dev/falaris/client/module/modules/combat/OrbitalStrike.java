package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.DoubleSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TntEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

public final class OrbitalStrike extends CombatModule {
    private final IntegerSetting tntCount = setting(new IntegerSetting("TNT Count", "Primed TNT to spawn.", 500, 100, 5000));
    private final DoubleSetting spread = setting(new DoubleSetting("Spread", "Horizontal spread radius.", 15.0, 5.0, 50.0));
    private final DoubleSetting height = setting(new DoubleSetting("Height", "Spawn height above target.", 40.0, 10.0, 100.0));
    private final IntegerSetting minFuse = setting(new IntegerSetting("Min Fuse", "Minimum fuse ticks.", 20, 5, 80));
    private final IntegerSetting maxFuse = setting(new IntegerSetting("Max Fuse", "Maximum fuse ticks.", 60, 10, 160));
    private final IntegerSetting waves = setting(new IntegerSetting("Waves", "Split TNT into waves.", 5, 1, 20));
    private final IntegerSetting waveDelay = setting(new IntegerSetting("Wave Delay", "Ticks between waves.", 5, 1, 20));
    private final BooleanSetting summonBelow = setting(new BooleanSetting("Summon Below", "Spawn TNT at target y-level too.", true));

    private final Random random = new Random();
    private int phase; // 0=idle, 1=striking
    private int waveIndex;
    private int ticksSinceWave;
    private int tntPerWave;
    private Vec3d strikeCenter;
    private int totalSpawned;

    public OrbitalStrike() {
        super("OrbitalStrike", "Summons a devastating TNT barrage from the sky (creative/op only).");
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        phase = 0;
        waveIndex = 0;
        ticksSinceWave = 0;
        tntPerWave = 0;
        totalSpawned = 0;
        strikeCenter = null;
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;
        if (!client.player.isCreative()) {
            if (phase != 0) onDisable();
            return;
        }

        switch (phase) {
            case 0 -> {
                phase = 1;
                waveIndex = 0;
                ticksSinceWave = 0;
                totalSpawned = 0;
                tntPerWave = Math.max(1, tntCount.get() / waves.get());

                var target = CombatUtil.bestLivingTarget(client, 32.0, true, true, false, true, "Distance");
                strikeCenter = target.map(e -> new Vec3d(e.getX(), e.getY(), e.getZ()))
                        .orElseGet(() -> new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ()));
            }
            case 1 -> {
                ticksSinceWave++;
                if (ticksSinceWave >= waveDelay.get()) {
                    ticksSinceWave = 0;
                    spawnWave(client);
                    waveIndex++;
                    if (waveIndex >= waves.get() || totalSpawned >= tntCount.get()) {
                        phase = 2;
                    }
                }
            }
            case 2 -> {
                setEnabled(false);
            }
        }
    }

    private void spawnWave(MinecraftClient client) {
        int count = Math.min(tntPerWave, tntCount.get() - totalSpawned);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = random.nextDouble() * spread.get();
            double x = strikeCenter.x + Math.cos(angle) * dist;
            double z = strikeCenter.z + Math.sin(angle) * dist;
            double y = strikeCenter.y + height.get() + random.nextDouble() * 10;
            int fuse = minFuse.get() + random.nextInt(maxFuse.get() - minFuse.get() + 1);

            String cmd = String.format("/summon minecraft:tnt %.2f %.2f %.2f {fuse:%d}", x, y, z, fuse);
            client.player.networkHandler.sendChatMessage(cmd);
            totalSpawned++;
        }
    }
}
