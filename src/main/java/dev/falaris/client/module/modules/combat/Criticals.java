package dev.falaris.client.module.modules.combat;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public final class Criticals extends CombatModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Critical hit method.", "Jump", "Jump", "Packet", "MiniJump"));
    private final ModeSetting bypass = setting(new ModeSetting("Bypass", "Anticheat bypass.", "Vanilla", "Vanilla", "Vulcan", "Verus", "Grim", "Watchdog"));
    private final BooleanSetting onlyGround = setting(new BooleanSetting("Only Ground", "Only crit when standing on ground.", true));
    private final BooleanSetting checkDistance = setting(new BooleanSetting("Check Distance", "Only crit within 3 blocks of target.", false));

    private boolean wasAttacking;

    public Criticals() {
        super("Criticals", "Ensures every hit is a critical strike.");
    }

    @Override
    protected void onCombatTick(MinecraftClient client) {
        if (client.player == null) return;

        boolean attacking = client.options.attackKey.isPressed();

        if (!attacking) {
            wasAttacking = false;
            return;
        }

        if (!client.player.isOnGround()) return;
        if (onlyGround.enabled() && !client.player.isOnGround()) return;

        if (attacking && !wasAttacking) {
            doCrit(client);
        }

        wasAttacking = attacking;
    }

    private void doCrit(MinecraftClient client) {
        String b = bypass.get();

        if (mode.is("Jump")) {
            if (b.equals("Vulcan")) {
                client.player.setVelocity(client.player.getVelocity().x, 0.25, client.player.getVelocity().z);
            } else if (b.equals("Verus")) {
                client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    client.player.getX(), client.player.getY() + 1.0E-6, client.player.getZ(),
                    false, client.player.horizontalCollision));
                client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    client.player.getX(), client.player.getY(), client.player.getZ(),
                    false, client.player.horizontalCollision));
            } else if (b.equals("Grim")) {
                client.player.jump();
            } else {
                client.player.jump();
            }
        } else if (mode.is("Packet")) {
            if (b.equals("Grim")) {
                client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    client.player.getX(), client.player.getY() + 0.0625, client.player.getZ(),
                    false, client.player.horizontalCollision));
            } else if (b.equals("Watchdog")) {
                client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    client.player.getX(), client.player.getY() + 0.0625, client.player.getZ(),
                    false, client.player.horizontalCollision));
                client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    client.player.getX(), client.player.getY(), client.player.getZ(),
                    false, client.player.horizontalCollision));
            } else {
                client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    client.player.getX(), client.player.getY() + 0.0625, client.player.getZ(),
                    false, client.player.horizontalCollision));
                client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    client.player.getX(), client.player.getY(), client.player.getZ(),
                    false, client.player.horizontalCollision));
            }
        } else if (mode.is("MiniJump")) {
            client.player.setVelocity(client.player.getVelocity().x, 0.1, client.player.getVelocity().z);
        }
    }
}
