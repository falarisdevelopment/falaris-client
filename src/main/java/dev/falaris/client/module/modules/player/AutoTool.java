package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public final class AutoTool extends PlayerModule {
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks between tool switches.", 1, 1, 20));
    private final IntegerSetting jitter = setting(new IntegerSetting("Jitter", "Random extra ticks between tool switches.", 0, 0, 10));
    private final BooleanSetting hotbarOnly = setting(new BooleanSetting("Hotbar Only", "Only select tools already in the hotbar.", true));
    private final BooleanSetting requireAttack = setting(new BooleanSetting("Require Attack", "Only switch while mining.", true));

    public AutoTool() {
        super("AutoTool", "Selects the fastest tool for the block under the crosshair.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.crosshairTarget == null) {
            return;
        }
        if (requireAttack.enabled() && !client.options.attackKey.isPressed()) {
            return;
        }
        if (client.crosshairTarget.getType() != HitResult.Type.BLOCK || !ready(delay.get(), jitter.get())) {
            return;
        }

        BlockHitResult hit = (BlockHitResult) client.crosshairTarget;
        BlockState state = client.world.getBlockState(hit.getBlockPos());
        int best = InventoryUtil.findBestTool(client.player, state, hotbarOnly.enabled());
        if (best >= 0 && best <= 8) {
            InventoryUtil.selectHotbar(client.player, best);
        } else if (!hotbarOnly.enabled() && best != -1) {
            InventoryUtil.quickMove(client, InventoryUtil.screenSlotForInventoryIndex(best));
        }
    }
}
