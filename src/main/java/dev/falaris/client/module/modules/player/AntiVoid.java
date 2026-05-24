package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

public final class AntiVoid extends PlayerModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Anti-void method.", "Packet", "Packet", "Rubberband"));
    private final IntegerSetting triggerY = setting(new IntegerSetting("Trigger Y", "Y level to trigger.", -32, -64, 0));
    private final IntegerSetting packetCount = setting(new IntegerSetting("Packet Count", "Packets to send per tick.", 10, 1, 50));
    private final BooleanSetting pullUp = setting(new BooleanSetting("Pull Up", "Increases Y to pull back up.", true));

    private boolean wasFalling;

    public AntiVoid() {
        super("AntiVoid", "Prevents falling into the void.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        double y = client.player.getY();
        if (y > triggerY.get()) {
            wasFalling = false;
            return;
        }

        if (client.player.fallDistance <= 0 && wasFalling) return;
        wasFalling = true;

        Vec3d pos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());

        if (mode.is("Packet")) {
            for (int i = 0; i < packetCount.get(); i++) {
                double fakeY = pullUp.enabled() ? y + 10 + i : y;
                client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    pos.x, fakeY, pos.z, true, false
                ));
            }
        } else if (mode.is("Rubberband") && client.player.networkHandler != null) {
            client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                pos.x, pos.y + 20, pos.z, false, false
            ));
        }
    }
}
