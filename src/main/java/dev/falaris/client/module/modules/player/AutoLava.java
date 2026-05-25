package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.ModeSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class AutoLava extends PlayerModule {
    private final ModeSetting mode = setting(new ModeSetting("Mode", "Placement mode.", "Below", "Below", "Crosshair", "Below"));
    private final BooleanSetting autoSwitch = setting(new BooleanSetting("Auto Switch", "Auto-switch to lava bucket.", true));
    private final BooleanSetting toggleAfterPlace = setting(new BooleanSetting("Toggle After Place", "Disable after placing.", true));

    private boolean placed;

    public AutoLava() {
        super("AutoLava", "Places lava for coving, defense, or escaping.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null || client.world == null || placed) return;

        if (mode.is("Below")) {
            var pos = client.player.getBlockPos().down(2);
            if (!client.world.getBlockState(pos).isAir()) return;
            if (!selectLava(client)) return;
            Vec3d center = Vec3d.ofCenter(pos);
            var result = new BlockHitResult(center, Direction.UP, pos, false);
            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, result);
            client.player.swingHand(Hand.MAIN_HAND);
            placed = true;
        } else if (mode.is("Crosshair") && client.crosshairTarget instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            if (!client.world.getBlockState(bhr.getBlockPos().offset(bhr.getSide())).isAir()) return;
            if (!selectLava(client)) return;
            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, bhr);
            client.player.swingHand(Hand.MAIN_HAND);
            placed = true;
        }

        if (placed && toggleAfterPlace.enabled()) setEnabled(false);
    }

    private boolean selectLava(MinecraftClient client) {
        if (!autoSwitch.enabled()) return true;
        var inv = client.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isOf(Items.LAVA_BUCKET)) {
                client.player.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        return false;
    }

    @Override protected void onEnable() { super.onEnable(); placed = false; }
}
