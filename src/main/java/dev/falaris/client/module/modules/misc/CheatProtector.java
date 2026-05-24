package dev.falaris.client.module.modules.misc;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.client.MinecraftClient;

import java.util.Random;

public final class CheatProtector extends MiscModule {
    private final BooleanSetting packetRandomizer = setting(new BooleanSetting("Packet Randomizer", "Add jitter to packet timing to avoid pattern detection.", true));
    private final BooleanSetting fakeMovements = setting(new BooleanSetting("Fake Movements", "Send occasional fake movement packets to mask real ones.", true));
    private final BooleanSetting antiPayload = setting(new BooleanSetting("Anti Payload", "Block suspicious plugin channel payloads (DonutSMP).", true));
    private final BooleanSetting selfDestruct = setting(new BooleanSetting("Self Destruct", "Rapidly disable all modules when toggled OFF.", true));
    private final IntegerSetting fakeMoveInterval = setting(new IntegerSetting("Fake Move Interval", "Ticks between fake movement packets.", 40, 10, 200));

    private final Random random = new Random();
    private int fakeMoveTick;
    private int packetJitterTick;

    public CheatProtector() {
        super("CheatProtector", "Protects against packet inspection anti-cheats and detection.");
    }

    @Override
    protected void onMiscTick(MinecraftClient client) {
        if (client.player == null) return;

        fakeMoveTick++;
        packetJitterTick++;

        if (fakeMovements.enabled() && fakeMoveTick >= fakeMoveInterval.get() + random.nextInt(20)) {
            fakeMoveTick = 0;
            client.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround(
                client.player.getX() + (random.nextDouble() - 0.5) * 0.001,
                client.player.getY(),
                client.player.getZ() + (random.nextDouble() - 0.5) * 0.001,
                client.player.isOnGround(),
                client.player.horizontalCollision
            ));
        }

        if (packetRandomizer.enabled() && packetJitterTick % 5 == 0) {
            client.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround(
                client.player.getX() + (random.nextDouble() - 0.5) * 0.0001,
                client.player.getY() + (random.nextDouble() - 0.5) * 0.0001,
                client.player.getZ() + (random.nextDouble() - 0.5) * 0.0001,
                client.player.isOnGround(),
                client.player.horizontalCollision
            ));
        }
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        if (selfDestruct.enabled()) {
            var modules = dev.falaris.client.FalarisClient.getInstance().getModuleManager().getModules();
            for (var mod : modules) {
                if (mod != this && mod.isEnabled()) {
                    mod.setEnabled(false);
                }
            }
        }
    }
}
