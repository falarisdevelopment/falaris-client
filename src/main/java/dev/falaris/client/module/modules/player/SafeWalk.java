package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public final class SafeWalk extends PlayerModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Safety method.", "Sneak", "Sneak", "Legit", "Edge"));
    private final BooleanSetting slow = setting(new BooleanSetting("Slow", "Slow down on edges.", true));
    private final BooleanSetting onlySneaking = setting(new BooleanSetting("Only While Sneaking", "Only activate when already sneaking.", false));

    public SafeWalk() {
        super("SafeWalk", "Prevents you from walking off edges.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null) return;

        if (onlySneaking.enabled() && !client.options.sneakKey.isPressed()) return;

        if (!client.player.isOnGround()) return;

        if (mode.is("Sneak")) {
            client.player.setSneaking(true);
        } else if (mode.is("Legit")) {
            if (isAtEdge(client)) {
                client.player.setSneaking(true);
            }
        } else if (mode.is("Edge")) {
            if (isAtEdge(client)) {
                client.player.setVelocity(client.player.getVelocity().x, -0.1, client.player.getVelocity().z);
            }
        }
    }

    private boolean isAtEdge(MinecraftClient client) {
        double x = client.player.getX();
        double z = client.player.getZ();
        double y = client.player.getY() - 0.5;

        var world = client.world;
        if (world == null) return false;

        return world.getBlockState(BlockPos.ofFloored(x - 0.3, y, z - 0.3)).isAir()
                || world.getBlockState(BlockPos.ofFloored(x + 0.3, y, z - 0.3)).isAir()
                || world.getBlockState(BlockPos.ofFloored(x - 0.3, y, z + 0.3)).isAir()
                || world.getBlockState(BlockPos.ofFloored(x + 0.3, y, z + 0.3)).isAir();
    }
}
